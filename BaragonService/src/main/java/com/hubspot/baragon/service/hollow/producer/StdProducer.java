package com.hubspot.baragon.service.hollow.producer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.hubspot.baragon.service.hollow.HollowDataReplicationBlobRetriever;
import com.hubspot.baragon.service.hollow.common.DataSource;
import com.hubspot.baragon.service.hollow.common.IndexStrategy;
import com.hubspot.baragon.service.hollow.common.NamingStrategy;
import com.hubspot.baragon.service.hollow.common.schema.SchemaObjectMapper;
import com.hubspot.baragon.service.hollow.schema.HollowSchemaWrapper;
import com.hubspot.baragon.service.hollow.schema.SchemaSource;
import com.netflix.hollow.api.consumer.HollowConsumer.AnnouncementWatcher;
import com.netflix.hollow.api.metrics.HollowMetricsCollector;
import com.netflix.hollow.api.metrics.HollowProducerMetrics;
import com.netflix.hollow.api.producer.HollowProducer;
import com.netflix.hollow.api.producer.HollowProducer.ReadState;
import com.netflix.hollow.api.producer.HollowProducer.VersionMinter;
import com.netflix.hollow.api.producer.HollowProducerListener;
import com.netflix.hollow.api.producer.HollowProducerListener.ProducerStatus;
import com.netflix.hollow.api.producer.HollowProducerListener.PublishStatus;
import com.netflix.hollow.api.producer.HollowProducerListener.Status;
import com.netflix.hollow.api.producer.fs.HollowInMemoryBlobStager;
import com.netflix.hollow.core.schema.HollowSchema;

public class StdProducer implements HollowDataReplicationProducer {
  private static final Logger LOG = LoggerFactory.getLogger(StdProducer.class);

  private final SchemaObjectMapper mapper;
  private final HollowProducer producer;

  private volatile long latestVersion = AnnouncementWatcher.NO_ANNOUNCEMENT_AVAILABLE;
  private volatile long nextVersion;

  StdProducer(DataSource dataSource,
              IndexStrategy indexStrategy,
              NamingStrategy namingStrategy,
              SchemaSource schemaSource,
              int statesBetweenSnapshots,
              long targetMaxTypeShardSize,
              SchemaObjectMapper mapper,
              ProducerMetricReporter metricReporter) {
    this.mapper = mapper;

    StdVersionMinter versionMinter = new StdVersionMinter();
    HollowProducer producer = createProducer(
        dataSource,
        indexStrategy,
        namingStrategy,
        schemaSource,
        statesBetweenSnapshots,
        targetMaxTypeShardSize,
        metricReporter,
        versionMinter);

    RestoringWatcher watcher = new RestoringWatcher(dataSource, namingStrategy);
    HollowDataReplicationBlobRetriever retriever = new HollowDataReplicationBlobRetriever(dataSource, namingStrategy, indexStrategy);

    if (watcher.hasLatestVersion()) {
      LOG.debug("Restoring producer state from latest remote version: {}", watcher.getLatestVersion());
      ReadState readState = producer.restore(watcher.getLatestVersion(), retriever);
      if (!hasExpectedSchema(schemaSource.getAll(), readState.getStateEngine().getSchemas())) {
        LOG.debug("Cannot restore producer state due to schema changes, initializing producer without restored state");
        // for now we must force a double-snapshot by avoiding the restore
        producer = createProducer(
            dataSource,
            indexStrategy,
            namingStrategy,
            schemaSource,
            statesBetweenSnapshots,
            targetMaxTypeShardSize,
            metricReporter,
            versionMinter);
        metricReporter.recordDoubleSnapshot();
      }
    }

    // this must be called after createProducer to pick up the restored version (if any)
    long version = this.getLatestVersion();
    if (version != AnnouncementWatcher.NO_ANNOUNCEMENT_AVAILABLE) {
      versionMinter.initializeVersion(version + 1);
    } else {
      versionMinter.initializeVersion(1);
    }

    this.producer = producer;
  }

  @Override
  public Cycle newCycle() {
    return new SimpleCycle(this::runCycle);
  }

  @Override
  public long getLatestVersion() {
    return latestVersion;
  }

  private HollowProducer createProducer(DataSource dataSource,
                                        IndexStrategy indexStrategy,
                                        NamingStrategy namingStrategy,
                                        SchemaSource schemaSource,
                                        int statesBetweenSnapshots,
                                        long targetMaxTypeShardSize,
                                        ProducerMetricReporter metricReporter,
                                        VersionMinter versionMinter) {
    HollowProducer producer = HollowProducer.withPublisher(new HollowDataReplicationPublisher(dataSource, namingStrategy, indexStrategy))
        .withAnnouncer(new HollowDataReplicationAnnouncer(dataSource, namingStrategy))
        .withVersionMinter(versionMinter)
        .withNumStatesBetweenSnapshots(statesBetweenSnapshots)
        .withTargetMaxTypeShardSize(targetMaxTypeShardSize)
        .withListener(new InternalProducerListener(metricReporter))
        .withBlobStager(new HollowInMemoryBlobStager())
        .withMetricsCollector(new StdMetricsCollector(metricReporter))
        .withBlobStorageCleaner(
            new AsyncBlobStorageCleaner(
                dataSource,
                namingStrategy,
                () -> latestVersion,
                indexStrategy))
        .build();

    LOG.debug("Initializing producer schema");
    HollowSchema[] schemas = schemaSource.getAll()
        .stream()
        .map(HollowSchemaWrapper::getHollowSchema)
        .toArray(HollowSchema[]::new);

    producer.initializeDataModel(schemas);
    return producer;
  }

  private synchronized long runCycle(ListMultimap<String, JsonNode> data) {
    return producer.runCycle(writeState -> {
      data.entries().forEach(e -> mapper.writeTo(e.getKey(), e.getValue(), writeState.getStateEngine()));
    });
  }

  private void setVersion(long version) {
    nextVersion = version;
  }

  private void confirmVersion(ProducerMetricReporter metricReporter) {
    latestVersion = nextVersion;
    metricReporter.setCurrentVersion(latestVersion);
  }

  private boolean hasExpectedSchema(Collection<HollowSchemaWrapper> expected, List<HollowSchema> actual) {
    if (actual.size() != expected.size()) {
      return false;
    }

    Map<String, HollowSchema> actualLookup = actual.stream()
        .collect(Collectors.toMap(
            HollowSchema::getName,
            Function.identity()));

    try (ByteArrayOutputStream expectedStream = new ByteArrayOutputStream();
         ByteArrayOutputStream actualStream = new ByteArrayOutputStream()) {
      for (HollowSchemaWrapper wrapper : expected) {
        HollowSchema act = actualLookup.get(wrapper.getName());
        if (act == null) {
          return false;
        }

        wrapper.getHollowSchema().writeTo(expectedStream);
        act.writeTo(actualStream);

        byte[] expectedBytes = expectedStream.toByteArray();
        byte[] actualBytes = actualStream.toByteArray();
        if (!Arrays.equals(expectedBytes, actualBytes)) {
          LOG.debug("Actual schema: {}, Expected: {}",
              new String(actualBytes, StandardCharsets.UTF_8),
              new String(expectedBytes, StandardCharsets.UTF_8));
          return false;
        }

        expectedStream.reset();
        actualStream.reset();
      }
    } catch (Exception e) {
      LOG.error("Encountered exception verifying restored producer schema state against the expected schema", e);
      return false;
    }

    return true;
  }

  private class InternalProducerListener implements HollowProducerListener {
    private final ProducerMetricReporter metricReporter;

    InternalProducerListener(ProducerMetricReporter metricReporter) {
      this.metricReporter = metricReporter;
    }

    @Override
    public void onProducerInit(long elapsed, TimeUnit unit) {
      LOG.debug("Producer initialized in {}ms", unit.toMillis(elapsed));
      metricReporter.recordProducerInitMillis(unit.toMillis(elapsed));
    }

    @Override
    public void onProducerRestoreStart(long restoreVersion) {
      LOG.debug("Producer restoration started with version: {}", restoreVersion);
      setVersion(restoreVersion);
    }

    @Override
    public void onProducerRestoreComplete(RestoreStatus status, long elapsed, TimeUnit unit) {
      LOG.debug("Producer restoration complete: {} - elapsed: {}ms", status.getStatus(), unit.toMillis(elapsed));
      if (status.getStatus() == Status.SUCCESS) {
        confirmVersion(metricReporter);
        metricReporter.recordProducerRestoreMillis(unit.toMillis(elapsed));
      }
    }

    @Override
    public void onNewDeltaChain(long version) {
      LOG.debug("New delta chain for version: {}", version);
    }

    @Override
    public void onCycleStart(long version) {
      LOG.debug("Cycle started for version: {}", version);
      setVersion(version);
    }

    @Override
    public void onCycleComplete(ProducerStatus status, long elapsed, TimeUnit unit) {
      LOG.debug("Cycle complete: {} - elapsed: {}ms", status.getStatus(), unit.toMillis(elapsed));
      if (status.getStatus() == Status.SUCCESS) {
        confirmVersion(metricReporter);
        metricReporter.recordProducerCycleMillis(unit.toMillis(elapsed));
      }
    }

    @Override
    public void onNoDeltaAvailable(long version) {
      LOG.debug("No delta available for version: {}", version);
    }

    @Override
    public void onPopulateStart(long version) {
      LOG.debug("Populate started for version: {}", version);
    }

    @Override
    public void onPopulateComplete(ProducerStatus status, long elapsed, TimeUnit unit) {
      LOG.debug("Populate complete: {} - elapsed: {}", status.getStatus(), unit.toMillis(elapsed));
    }

    @Override
    public void onPublishStart(long version) {
      LOG.debug("Publish started for version: {}", version);
    }

    @Override
    public void onPublishComplete(ProducerStatus status, long elapsed, TimeUnit unit) {
      LOG.debug("Publish complete in: {}ms", unit.toMillis(elapsed));
      metricReporter.recordProducerPublishMillis(unit.toMillis(elapsed));
    }

    @Override
    public void onArtifactPublish(PublishStatus publishStatus, long elapsed, TimeUnit unit) {
      LOG.debug("Artifact published: {} - elapsed: {}ms", publishStatus.getStatus(), unit.toMillis(elapsed));
    }

    @Override
    public void onIntegrityCheckStart(long version) {
      LOG.debug("Integrity check started for version: {}", version);
    }

    @Override
    public void onIntegrityCheckComplete(ProducerStatus status, long elapsed, TimeUnit unit) {
      LOG.debug("Integrity check complete: {} - elapsed: {}ms", status.getStatus(), unit.toMillis(elapsed));
    }

    @Override
    public void onValidationStart(long version) {
      LOG.debug("Validation started for version: {}", version);
    }

    @Override
    public void onValidationComplete(ProducerStatus status, long elapsed, TimeUnit unit) {
      LOG.debug("Validation complete: {} - elapsed: {}ms", status.getStatus(), unit.toMillis(elapsed));
    }

    @Override
    public void onAnnouncementStart(long version) {
      LOG.debug("Announcement started for version: {}", version);
    }

    @Override
    public void onAnnouncementComplete(ProducerStatus status, long elapsed, TimeUnit unit) {
      LOG.debug("Announcement complete: {} - elapsed: {}ms", status.getStatus(), unit.toMillis(elapsed));
    }
  }

  private static class StdMetricsCollector extends HollowMetricsCollector<HollowProducerMetrics> {
    private final ProducerMetricReporter metricReporter;

    StdMetricsCollector(ProducerMetricReporter metricReporter) {
      this.metricReporter = metricReporter;
    }

    @Override
    public void collect(HollowProducerMetrics metrics) {
      metricReporter.recordHeapSpaceBytes(metrics.getTotalHeapFootprint());
      metricReporter.recordTotalPopulatedOrdinals(metrics.getTotalPopulatedOrdinals());
      metricReporter.recordCyclesCompleted(metrics.getCyclesCompleted());
      metricReporter.recordCyclesFailed(metrics.getCycleFailed());
    }
  }

  private static class StdVersionMinter implements VersionMinter {
    private AtomicLong version;

    void initializeVersion(long version) {
      if (this.version != null) {
        throw new IllegalStateException("Version already initialized");
      }

      this.version = new AtomicLong(version);
    }

    @Override
    public long mint() {
      return Preconditions.checkNotNull(version, "Mint called before minter has been initialized!").getAndIncrement();
    }
  }
}
