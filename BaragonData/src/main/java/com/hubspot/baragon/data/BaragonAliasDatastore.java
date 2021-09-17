package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.models.BaragonGroupAlias;
import java.util.List;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

public class BaragonAliasDatastore extends AbstractDataStore {
  public static final String ALIASES_ROOT = "/aliases";

  @Inject
  public BaragonAliasDatastore(
    CuratorFramework curatorFramework,
    ObjectMapper objectMapper,
    ZooKeeperConfiguration zooKeeperConfiguration
  ) {
    super(curatorFramework, objectMapper, zooKeeperConfiguration);
  }

  private String getAliasPath(String name) {
    return ZKPaths.makePath(ALIASES_ROOT, name);
  }

  public void saveAlias(String name, BaragonGroupAlias alias) {
    writeToZk(getAliasPath(name), alias);
  }

  public List<String> getAllAliases() {
    return getChildren(ALIASES_ROOT);
  }

  public Optional<BaragonGroupAlias> getAlias(String name) {
    return readFromZk(getAliasPath(name), BaragonGroupAlias.class);
  }

  public void deleteAlias(String name) {
    deleteNode(getAliasPath(name));
  }
}
