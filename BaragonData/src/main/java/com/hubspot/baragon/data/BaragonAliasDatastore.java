package com.hubspot.baragon.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.models.BaragonGroupAlias;
import com.hubspot.baragon.models.BaragonRequest;

public class BaragonAliasDatastore extends AbstractDataStore {

  public static final String ALIASES_ROOT = "/aliases";

  @Inject
  public BaragonAliasDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper, ZooKeeperConfiguration zooKeeperConfiguration) {
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

  public BaragonRequest updateForAliases(BaragonRequest original) {
    Set<String> edgeCacheDomains = new HashSet<>(original.getLoadBalancerService().getEdgeCacheDomains());
    if (original.getLoadBalancerService().getEdgeCacheDNS().isPresent()) {
      edgeCacheDomains.add(original.getLoadBalancerService().getEdgeCacheDNS().get());
    }
    return original.withUpdatedGroups(
        processAliases(
            original.getLoadBalancerService().getLoadBalancerGroups(),
            original.getLoadBalancerService().getDomains(),
            edgeCacheDomains
        )
    );
  }

  public BaragonGroupAlias processAliases(Set<String> groups, Set<String> domains, Set<String> edgeCacheDomains) {
    Set<String> allGroups = new HashSet<>();
    Set<String> allDomains = new HashSet<>(domains);
    Set<String> allEdgeCacheDomains = new HashSet<>(edgeCacheDomains);
    for (String group : groups) {
      Optional<BaragonGroupAlias> maybeAlias = getAlias(group);
      if (maybeAlias.isPresent()) {
        allGroups.addAll(maybeAlias.get().getGroups());
        allDomains.addAll(maybeAlias.get().getDomains());
        allEdgeCacheDomains.addAll(maybeAlias.get().getEdgeCacheDomains());
      } else {
        allGroups.add(group);
      }
    }
    return new BaragonGroupAlias(allGroups, allDomains, allEdgeCacheDomains);
  }
}
