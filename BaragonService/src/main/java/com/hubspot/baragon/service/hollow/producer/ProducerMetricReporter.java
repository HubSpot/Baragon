package com.hubspot.baragon.service.hollow.producer;

public interface ProducerMetricReporter {
  void setCurrentVersion(long version);
  void recordProducerInitMillis(long valueMs);
  void recordProducerRestoreMillis(long valueMs);
  void recordProducerCycleMillis(long valueMs);
  void recordProducerPublishMillis(long valueMs);
  void recordHeapSpaceBytes(long bytes);
  void recordTotalPopulatedOrdinals(long count);
  void recordCyclesFailed(long cycles);
  void recordCyclesCompleted(long cycles);
  void recordDoubleSnapshot();
}
