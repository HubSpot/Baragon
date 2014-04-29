package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.BaseEncoding;
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
  public static final String CLUSTERS_FORMAT = "/singularity/load-balancer";
  public static final String CLUSTER_HOSTS_FORMAT = "/singularity/load-balancer/%s/hosts";
  public static final String CLUSTER_HOST_FORMAT = "/singularity/load-balancer/%s/hosts/%s";
  public static final String BASE_URI_FORMAT = "/singularity/load-balancer/%s/base-uris/%s";

  @Inject
  public BaragonLoadBalancerDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public LeaderLatch createLeaderLatch(String clusterName, String hostname) {
    return new LeaderLatch(curatorFramework, String.format(CLUSTER_HOSTS_FORMAT, clusterName), hostname);
  }

  public Collection<String> getClusters() {
    return getChildren(CLUSTERS_FORMAT);
  }

  public Collection<String> getHosts(String clusterName) {
    final Collection<String> nodes = getChildren(String.format(CLUSTER_HOSTS_FORMAT, clusterName));

    if (nodes.isEmpty()) {
      return Collections.emptyList();
    }

    final Collection<String> hosts = Lists.newArrayListWithCapacity(nodes.size());

    for (String node : nodes) {
      try {
        hosts.add(new String(curatorFramework.getData().forPath(String.format(CLUSTER_HOST_FORMAT, clusterName, node)), Charsets.UTF_8));
      } catch (KeeperException.NoNodeException nne) {
        // uhh, didnt see that...
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    return hosts;
  }

  public Collection<String> getAllHosts(Collection<String> clusterNames) {
    final Set<String> hosts = Sets.newHashSet();

    for (String clusterName : clusterNames) {
      hosts.addAll(getHosts(clusterName));
    }

    return hosts;
  }

  private String sanitizeBasePath(String basePath) {
    return BaseEncoding.base64Url().encode(basePath.trim().toLowerCase().getBytes());
  }

  public Optional<String> getBasePathServiceId(String loadBalancerGroup, String basePath) {
    return readFromZk(String.format(BASE_URI_FORMAT, loadBalancerGroup, sanitizeBasePath(basePath)), String.class);
  }

  public void clearBasePath(String loadBalancerGroup, String basePath) {
    deleteNode(String.format(BASE_URI_FORMAT, loadBalancerGroup, sanitizeBasePath(basePath)));
  }

  public void setBasePathServiceId(String loadBalancerGroup, String basePath, String serviceId) {
    writeToZk(String.format(BASE_URI_FORMAT, loadBalancerGroup, sanitizeBasePath(basePath)), serviceId);
  }
}
