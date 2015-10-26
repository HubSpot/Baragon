package com.hubspot.baragon.data;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.config.ZooKeeperConfiguration;

public class BaragonZkMetaDatastore extends AbstractDataStore {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonZkMetaDatastore.class);

  private static final String ZK_DATA_VERSION_PATH = "/zk-data-version";

  @Inject
  public BaragonZkMetaDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper, ZooKeeperConfiguration zooKeeperConfiguration) {
    super(curatorFramework, objectMapper, zooKeeperConfiguration);
  }

  public Optional<String> getZkDataVersion() {
    return readFromZk(ZK_DATA_VERSION_PATH, String.class);
  }

  public void setZkDataVersion(String version) {
    writeToZk(ZK_DATA_VERSION_PATH, version);
  }
}
