package com.hubspot.baragon.service.hollow.producer;

import com.hubspot.baragon.service.hollow.common.DataSource;
import com.hubspot.baragon.service.hollow.common.IndexStrategy;
import com.hubspot.baragon.service.hollow.common.NamingStrategy;
import com.hubspot.baragon.service.hollow.common.schema.SchemaObjectMapper;
import com.hubspot.baragon.service.hollow.schema.SchemaSource;

public class StdProducer implements HollowDataReplicationProducer {
  public StdProducer(DataSource dataSource,
                     IndexStrategy indexStrategy,
                     NamingStrategy namingStrategy,
                     SchemaSource schemaSource,
                     int statesBetweenSnapshots,
                     long targetMaxTypeShardSize,
                     SchemaObjectMapper schemaObjectMapper, ProducerMetricReporter metricReporter) {
  }
}
