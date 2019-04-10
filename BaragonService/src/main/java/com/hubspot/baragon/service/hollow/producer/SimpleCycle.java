package com.hubspot.baragon.service.hollow.producer;

import java.util.function.Function;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.hubspot.baragon.service.hollow.producer.HollowDataReplicationProducer.Cycle;

public class SimpleCycle implements Cycle {
  private final ListMultimap<String, JsonNode> values = ArrayListMultimap.create();
  private final Function<ListMultimap<String, JsonNode>, Long> dataProcessor;

  SimpleCycle(Function<ListMultimap<String, JsonNode>, Long> dataProcessor) {
    this.dataProcessor = dataProcessor;
  }

  @Override
  public Cycle addData(String schemaType, JsonNode value) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(schemaType), "SchemaType cannot be null or empty!");
    Preconditions.checkNotNull(value, "Data value cannot be null!");

    values.put(schemaType, value);

    return this;
  }

  @Override
  public Cycle addData(String schemaType, JsonNode... values) {
    for (JsonNode value : values) {
      addData(schemaType, value);
    }

    return this;
  }

  @Override
  public Cycle addData(String schemaTYpe, Iterable<? extends JsonNode> values) {
    values.forEach(d -> addData(schemaTYpe, d));
    return this;
  }

  @Override
  public long run() {
    return dataProcessor.apply(values);
  }
}
