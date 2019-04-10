package com.hubspot.baragon.service.hollow.producer;

import com.fasterxml.jackson.databind.JsonNode;

public interface HollowDataReplicationProducer {
  Cycle newCycle();

  long getLatestVersion();

  interface Cycle {
    Cycle addData(String schemaType, JsonNode value);
    Cycle addData(String schemaType, JsonNode... values);
    Cycle addData(String schemaType, Iterable<? extends JsonNode> values);
    long run();
  }
}
