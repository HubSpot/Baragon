package com.hubspot.baragon.service.hollow.producer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NoOpProducerMetricReporter implements ProducerMetricReporter {
  private static final Logger LOG = LoggerFactory.getLogger(NoOpProducerMetricReporter.class);

  @Override
  public void recordProducerInitMillis(long value) {
    // no-op
  }

  @Override
  public void recordProducerRestoreMillis(long value) {
    // no-op
  }

  @Override
  public void recordProducerCycleMillis(long value) {
    // no-op
  }

  @Override
  public void recordProducerPublishMillis(long value) {
    // no-op
  }

  @Override
  public void setCurrentVersion(long version) {
    LOG.warn("This consumer is does not have metrics enabled! You will never know if you are missing updates!");
  }

  @Override
  public void recordHeapSpaceBytes(long bytes) {

  }

  @Override
  public void recordTotalPopulatedOrdinals(long count) {

  }

  @Override
  public void recordCyclesFailed(long cycles) {

  }

  @Override
  public void recordCyclesCompleted(long cycles) {

  }

  @Override
  public void recordDoubleSnapshot() {

  }
}
