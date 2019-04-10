package com.hubspot.baragon.service.hollow.producer;

import com.hubspot.baragon.service.hollow.common.DataSource;
import com.hubspot.baragon.service.hollow.common.NamingStrategy;

public class HollowDataReplicationAnnouncer implements com.netflix.hollow.api.producer.HollowProducer.Announcer {
  public HollowDataReplicationAnnouncer(DataSource dataSource, NamingStrategy namingStrategy) {
  }
}
