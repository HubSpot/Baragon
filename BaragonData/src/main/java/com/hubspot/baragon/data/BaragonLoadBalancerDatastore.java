package com.hubspot.baragon.data;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.TrafficSource;

@Singleton
public class BaragonLoadBalancerDatastore extends AbstractDataStore {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonLoadBalancerDatastore.class);

  public static final String LOAD_BALANCER_GROUPS_FORMAT = "/load-balancer";
  public static final String LOAD_BALANCER_GROUP_FORMAT = LOAD_BALANCER_GROUPS_FORMAT + "/%s";
  public static final String LOAD_BALANCER_TARGET_COUNT_FORMAT = LOAD_BALANCER_GROUP_FORMAT + "/targetCount";
  public static final String LOAD_BALANCER_GROUP_LAST_REQUEST_FORMAT = LOAD_BALANCER_GROUP_FORMAT + "/lastRequest";
  public static final String LOAD_BALANCER_GROUP_HOSTS_FORMAT = LOAD_BALANCER_GROUP_FORMAT + "/hosts";
  public static final String LOAD_BALANCER_GROUP_HOST_FORMAT = LOAD_BALANCER_GROUP_HOSTS_FORMAT + "/%s";

  public static final String LOAD_BALANCER_BASE_PATHS_FORMAT = LOAD_BALANCER_GROUPS_FORMAT + "/%s/base-uris";
  public static final String LOAD_BALANCER_BASE_PATH_FORMAT = LOAD_BALANCER_BASE_PATHS_FORMAT + "/%s";

  @Inject
  public BaragonLoadBalancerDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper, ZooKeeperConfiguration zooKeeperConfiguration) {
    super(curatorFramework, objectMapper, zooKeeperConfiguration);
  }

  public LeaderLatch createLeaderLatch(String clusterName, BaragonAgentMetadata agentMetadata) {
    try {
      return new LeaderLatch(curatorFramework, String.format(LOAD_BALANCER_GROUP_HOSTS_FORMAT, clusterName), objectMapper.writeValueAsString(agentMetadata));
    } catch (JsonProcessingException e) {
      throw Throwables.propagate(e);
    }
  }

  @Timed
  public Collection<BaragonGroup> getLoadBalancerGroups() {
    final Collection<String> nodes = getChildren(LOAD_BALANCER_GROUPS_FORMAT);

    if (nodes.isEmpty()) {
      return Collections.emptyList();
    }
    final Collection<BaragonGroup> groups = Lists.newArrayListWithCapacity(nodes.size());

    for (String node : nodes) {
      try {
        groups.addAll(readFromZk(String.format(LOAD_BALANCER_GROUP_FORMAT, node), BaragonGroup.class).asSet());
      } catch (Exception e) {
        LOG.error(String.format("Could not fetch info for group %s due to error %s", node, e));
      }
    }

    return groups;
  }

  @Timed
  public Optional<BaragonGroup> getLoadBalancerGroup(String name) {
    try {
      return readFromZk(String.format(LOAD_BALANCER_GROUP_FORMAT, name), BaragonGroup.class);
    } catch (RuntimeException e) {
      if (e.getMessage().contains("No content")) {
        return Optional.absent();
      }
      throw Throwables.propagate(e);
    }
  }

  @Timed
  public BaragonGroup addSourceToGroup(String name, TrafficSource source) {
    Optional<BaragonGroup> maybeGroup = getLoadBalancerGroup(name);
    BaragonGroup group;
    if (maybeGroup.isPresent()) {
      group = maybeGroup.get();
      group.addTrafficSource(source);
    } else {
      group = new BaragonGroup(name, Optional.<String>absent(), Sets.newHashSet(source), null, Optional.<String>absent(), Collections.<String>emptySet());
    }
    writeToZk(String.format(LOAD_BALANCER_GROUP_FORMAT, name), group);
    return group;
  }

  @Timed
  public Optional<BaragonGroup> removeSourceFromGroup(String name, TrafficSource source) {
    Optional<BaragonGroup> maybeGroup = getLoadBalancerGroup(name);
    if (maybeGroup.isPresent()) {
      maybeGroup.get().removeTrafficSource(source);
      writeToZk(String.format(LOAD_BALANCER_GROUP_FORMAT, name), maybeGroup.get());
      return maybeGroup;
    } else {
      return Optional.absent();
    }
  }

  @Timed
  public void updateGroupInfo(String name, Optional<String> defaultDomain, Set<String> domains) {
    Optional<BaragonGroup> maybeGroup = getLoadBalancerGroup(name);
    BaragonGroup group;
    if (maybeGroup.isPresent()) {
      group = maybeGroup.get();
      group.setDefaultDomain(defaultDomain);
      group.setDomains(domains);
    } else {
      group = new BaragonGroup(name, defaultDomain, Collections.<TrafficSource>emptySet(), null, defaultDomain, domains);
    }
    writeToZk(String.format(LOAD_BALANCER_GROUP_FORMAT, name), group);
  }

  @Timed
  public Set<String> getLoadBalancerGroupNames() {
    return ImmutableSet.copyOf(getChildren(LOAD_BALANCER_GROUPS_FORMAT));
  }

  @Timed
  public Optional<BaragonAgentMetadata> getAgent(String path) {
    return readFromZk(path, BaragonAgentMetadata.class);
  }

  @Timed
  public Optional<BaragonAgentMetadata> getAgent(String clusterName, String agentId) {
    Collection<BaragonAgentMetadata> agents = getAgentMetadata(clusterName);
    Optional<BaragonAgentMetadata> maybeAgent = Optional.absent();
    for (BaragonAgentMetadata agent : agents) {
      if (agent.getAgentId().equals(agentId)) {
        maybeAgent = Optional.of(agent);
        break;
      }
    }
    return maybeAgent;
  }

  @Timed
  public Collection<BaragonAgentMetadata> getAgentMetadata(String clusterName) {
    final Collection<String> nodes = getChildren(String.format(LOAD_BALANCER_GROUP_HOSTS_FORMAT, clusterName));

    if (nodes.isEmpty()) {
      return Collections.emptyList();
    }

    final Collection<BaragonAgentMetadata> metadata = Lists.newArrayListWithCapacity(nodes.size());

    for (String node : nodes) {
      try {
        final String value = new String(curatorFramework.getData().forPath(String.format(LOAD_BALANCER_GROUP_HOST_FORMAT, clusterName, node)), Charsets.UTF_8);
        if (value.startsWith("http://")) {
          metadata.add(BaragonAgentMetadata.fromString(value));
        } else {
          metadata.add(objectMapper.readValue(value, BaragonAgentMetadata.class));
        }
      } catch (KeeperException.NoNodeException nne) {
        // uhh, didnt see that...
      } catch (JsonParseException | JsonMappingException je) {
        LOG.warn(String.format("Exception deserializing %s", String.format(LOAD_BALANCER_GROUP_HOST_FORMAT, clusterName, node)), je);
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    return metadata;
  }

  @Timed
  public Collection<BaragonAgentMetadata> getAgentMetadata(Collection<String> clusterNames) {
    final Set<BaragonAgentMetadata> metadata = Sets.newHashSet();

    for (String clusterName : clusterNames) {
      metadata.addAll(getAgentMetadata(clusterName));
    }

    return metadata;
  }

  @Timed
  public Optional<String> getBasePathServiceId(String loadBalancerGroup, String basePath) {
    return readFromZk(String.format(LOAD_BALANCER_BASE_PATH_FORMAT, loadBalancerGroup, encodeUrl(basePath)), String.class);
  }

  @Timed
  public void clearBasePath(String loadBalancerGroup, String basePath) {
    deleteNode(String.format(LOAD_BALANCER_BASE_PATH_FORMAT, loadBalancerGroup, encodeUrl(basePath)));
  }

  @Timed
  public void setBasePathServiceId(String loadBalancerGroup, String basePath, String serviceId) {
    writeToZk(String.format(LOAD_BALANCER_BASE_PATH_FORMAT, loadBalancerGroup, encodeUrl(basePath)), serviceId);
  }

  @Timed
  public Collection<String> getBasePaths(String loadBalancerGroup) {
    final Collection<String> encodedPaths = getChildren(String.format(LOAD_BALANCER_BASE_PATHS_FORMAT, loadBalancerGroup));
    final Collection<String> decodedPaths = Lists.newArrayListWithCapacity(encodedPaths.size());

    for (String encodedPath : encodedPaths) {
      decodedPaths.add(decodeUrl(encodedPath));
    }

    return decodedPaths;
  }

  @Timed
  public Optional<String> getLastRequestForGroup(String loadBalancerGroup) {
    return readFromZk(String.format(LOAD_BALANCER_GROUP_LAST_REQUEST_FORMAT, loadBalancerGroup), String.class);
  }

  @Timed
  public void setLastRequestId(String loadBalancerGroup, String requestId) {
    writeToZk(String.format(LOAD_BALANCER_GROUP_LAST_REQUEST_FORMAT, loadBalancerGroup), requestId);
  }

  public int setTargetCount(String group, Integer count) {
    writeToZk(String.format(LOAD_BALANCER_TARGET_COUNT_FORMAT, group), count.toString());
    return count;
  }

  public Optional<Integer> getTargetCount(String group) {
    return readFromZk(String.format(LOAD_BALANCER_TARGET_COUNT_FORMAT, group), Integer.class);
  }
}
