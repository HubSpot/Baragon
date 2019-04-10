package com.hubspot.baragon.service.hollow.producer;

import com.hubspot.baragon.service.hollow.common.DataSource;
import com.hubspot.baragon.service.hollow.common.IndexStrategy;
import com.hubspot.baragon.service.hollow.common.NamingStrategy;

public class AsyncBlobStorageCleaner extends com.netflix.hollow.api.producer.HollowProducer.BlobStorageCleaner {
  public AsyncBlobStorageCleaner(DataSource dataSource, NamingStrategy namingStrategy, Object o, IndexStrategy indexStrategy) {
  }
}
