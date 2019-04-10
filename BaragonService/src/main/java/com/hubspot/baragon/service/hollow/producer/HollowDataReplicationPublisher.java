package com.hubspot.baragon.service.hollow.producer;

import com.hubspot.baragon.service.hollow.common.DataSource;
import com.hubspot.baragon.service.hollow.common.IndexStrategy;
import com.hubspot.baragon.service.hollow.common.NamingStrategy;
import com.netflix.hollow.api.producer.HollowProducer.Publisher;

public class HollowDataReplicationPublisher implements Publisher {
  public HollowDataReplicationPublisher(DataSource dataSource, NamingStrategy namingStrategy, IndexStrategy indexStrategy) {
  }
}
