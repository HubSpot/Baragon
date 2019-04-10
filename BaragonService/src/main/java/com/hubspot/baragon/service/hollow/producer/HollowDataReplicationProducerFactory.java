package com.hubspot.baragon.service.hollow.producer;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.hubspot.baragon.service.hollow.common.DataSource;
import com.hubspot.baragon.service.hollow.common.DataSourceDecorator;
import com.hubspot.baragon.service.hollow.common.IndexStrategy;
import com.hubspot.baragon.service.hollow.common.NamingStrategy;
import com.hubspot.baragon.service.hollow.common.implementation.StdIndexStrategy;
import com.hubspot.baragon.service.hollow.common.implementation.StdNamingStrategy;
import com.hubspot.baragon.service.hollow.common.schema.SchemaObjectMapper;
import com.hubspot.baragon.service.hollow.common.schema.StdSchemaObjectMapper;
import com.hubspot.baragon.service.hollow.schema.SchemaSource;

public class HollowDataReplicationProducerFactory {
  private final Set<DataSourceDecorator> dataSourceDecorator;
  private final ObjectMapper objectMapper;

  public HollowDataReplicationProducerFactory(Set<DataSourceDecorator> dataSourceDecorator,
                                              ObjectMapper objectMapper) {
    this.dataSourceDecorator = dataSourceDecorator;
    this.objectMapper = objectMapper;
  }

  public Builder newBuilder() {
    return new Builder(dataSourceDecorator, objectMapper);
  }

  public static class Builder<T> {
    private final Set<DataSourceDecorator> dataSourceDecorators;
    private final ObjectMapper objectMapper;

    protected DataSource dataSource;
    protected NamingStrategy namingStrategy;
    protected IndexStrategy indexStrategy;
    protected SchemaSource schemaSource;
    protected int statesBetweenSnapshots = 4;
    protected long targetMaxTypeShardSize = 25 * 1024 * 1024;
    protected SchemaObjectMapper schemaObjectMapper;
    protected ProducerMetricReporter metricReporter;

    protected Builder(Set<DataSourceDecorator> dataSourceDecorators, ObjectMapper objectMapper) {
      this.dataSourceDecorators = dataSourceDecorators;
      this.objectMapper = objectMapper;
    }

    public Builder<T> setDataSource(DataSource dataSource) {
      this.dataSource = Preconditions.checkNotNull(dataSource, "Data source cannot be null");
      return this;
    }

    public Builder<T> setNamingStrategy(NamingStrategy namingStrategy) {
      this.namingStrategy = Preconditions.checkNotNull(namingStrategy, "Naming strategy cannot be null");
      return this;
    }

    public Builder<T> setIndexStrategy(IndexStrategy indexStrategy) {
      this.indexStrategy = Preconditions.checkNotNull(indexStrategy, "Index strategy cannot be null");
      return this;
    }

    public Builder<T> setSchemaSource(SchemaSource schemaSource) {
      this.schemaSource = Preconditions.checkNotNull(schemaSource, "Schema source cannot be null");
      return this;
    }

    public Builder<T> setStatesBetweenSnapshots(int statesBetweenSnapshots) {
      Preconditions.checkArgument(statesBetweenSnapshots >= 0, "States between snapshots must not be negative");
      this.statesBetweenSnapshots = statesBetweenSnapshots;
      return this;
    }

    public Builder<T> setTargetMaxTypeShardSize(long targetMaxTypeShardSize) {
      Preconditions.checkArgument(targetMaxTypeShardSize >= 0, "Target max type shard size must not be negative");
      this.targetMaxTypeShardSize = targetMaxTypeShardSize;
      return this;
    }

    public Builder<T> setSchemaObjectMapper(SchemaObjectMapper schemaObjectMapper) {
      this.schemaObjectMapper = Preconditions.checkNotNull(schemaObjectMapper, "Schema object mapper must not be null");
      return this;
    }

    public Builder<T> setMetricReporter(ProducerMetricReporter metricReporter) {
      this.metricReporter = Preconditions.checkNotNull(metricReporter, "Metric reporter must not be null!");
      return this;
    }

    public final HollowDataReplicationProducer build() {
      Preconditions.checkNotNull(schemaSource, "Schema source must be set");
      Preconditions.checkNotNull(dataSource, "Datasource must be set");

      namingStrategy = namingStrategy != null ? namingStrategy : new StdNamingStrategy();
      dataSource = decorate(dataSource, namingStrategy);
      indexStrategy = indexStrategy != null ? indexStrategy : new StdIndexStrategy(dataSource, namingStrategy);
      schemaObjectMapper = schemaObjectMapper != null ? schemaObjectMapper : new StdSchemaObjectMapper(schemaSource, objectMapper);
      metricReporter = metricReporter != null ? metricReporter : new NoOpProducerMetricReporter();

      return new StdProducer(
          dataSource,
          indexStrategy,
          namingStrategy,
          schemaSource,
          statesBetweenSnapshots,
          targetMaxTypeShardSize,
          schemaObjectMapper,
          metricReporter);
    }

    private DataSource decorate(DataSource dataSource, NamingStrategy namingStrategy) {
      for (DataSourceDecorator decorator : dataSourceDecorators) {
        dataSource = decorator.decorate(dataSource, namingStrategy);
      }

      return dataSource;
    }
  }
}
