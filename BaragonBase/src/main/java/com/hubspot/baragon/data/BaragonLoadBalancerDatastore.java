package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.zookeeper.KeeperException;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Singleton
public class BaragonLoadBalancerDatastore extends AbstractDataStore {
  public static final String LOAD_BALANCER_GROUPS_FORMAT = "/load-balancer";
  public static final String LOAD_BALANCER_GROUP_HOSTS_FORMAT = LOAD_BALANCER_GROUPS_FORMAT + "/%s/hosts";
  public static final String LOAD_BALANCER_GROUP_HOST_FORMAT = LOAD_BALANCER_GROUP_HOSTS_FORMAT + "/%s";

  public static final String LOAD_BALANCER_BASE_PATH_FORMAT = LOAD_BALANCER_GROUPS_FORMAT + "/%s/base-uris/%s";

  @Inject
  public BaragonLoadBalancerDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public LeaderLatch createLeaderLatch(String clusterName, String hostname) {
    return new LeaderLatch(curatorFramework, String.format(LOAD_BALANCER_GROUP_HOSTS_FORMAT, clusterName), hostname);
  }

  public Collection<String> getClusters() {
    return getChildren(LOAD_BALANCER_GROUPS_FORMAT);
  }

  public Collection<String> getBaseUrls(String clusterName) {
    final Collection<String> nodes = getChildren(String.format(LOAD_BALANCER_GROUP_HOSTS_FORMAT, clusterName));

    if (nodes.isEmpty()) {
      return Collections.emptyList();
    }

    final Collection<String> baseUrls = Lists.newArrayListWithCapacity(nodes.size());

    for (String node : nodes) {
      try {
        baseUrls.add(new String(curatorFramework.getData().forPath(String.format(LOAD_BALANCER_GROUP_HOST_FORMAT, clusterName, node)), Charsets.UTF_8));
      } catch (KeeperException.NoNodeException nne) {
        // uhh, didnt see that...
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    return baseUrls;
  }

  public Collection<String> getAllBaseUrls(Collection<String> clusterNames) {
    final Set<String> baseUrls = Sets.newHashSet();

    for (String clusterName : clusterNames) {
      baseUrls.addAll(getBaseUrls(clusterName));
    }

    return baseUrls;
  }

  public Optional<String> getBasePathServiceId(String loadBalancerGroup, String basePath) {
    return readFromZk(String.format(LOAD_BALANCER_BASE_PATH_FORMAT, loadBalancerGroup, encodeUrl(basePath)), String.class);
  }

  public void clearBasePath(String loadBalancerGroup, String basePath) {
    deleteNode(String.format(LOAD_BALANCER_BASE_PATH_FORMAT, loadBalancerGroup, encodeUrl(basePath)));
  }

  public void setBasePathServiceId(String loadBalancerGroup, String basePath, String serviceId) {
    writeToZk(String.format(LOAD_BALANCER_BASE_PATH_FORMAT, loadBalancerGroup, encodeUrl(basePath)), serviceId);
  }
}
