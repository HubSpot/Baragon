package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.Participant;
import org.apache.zookeeper.KeeperException;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

@Singleton
public class BaragonLoadBalancerDatastore extends AbstractDataStore {
  public static final String CLUSTERS_FORMAT = "/load-balancer";
  public static final String CLUSTER_FORMAT = "/load-balancer/%s";

  @Inject
  public BaragonLoadBalancerDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public Collection<String> getClusters() {
    return getChildren(CLUSTERS_FORMAT);
  }

  public Collection<String> getHosts(String clusterName) {
    try {
      final LeaderLatch leaderLatch = new LeaderLatch(curatorFramework, String.format(CLUSTER_FORMAT, clusterName));

      final Collection<Participant> participants = leaderLatch.getParticipants();
      final Collection<String> results = Lists.newArrayListWithCapacity(participants.size());

      for (Participant participant : participants) {
        results.add(participant.getId());
      }

      return results;
    } catch (KeeperException.NoNodeException e) {
      return Collections.emptyList();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Collection<String> getAllHosts(Collection<String> clusterNames) {
    final Set<String> hosts = Sets.newHashSet();

    for (String clusterName : clusterNames) {
      hosts.addAll(getHosts(clusterName));
    }

    return hosts;
  }
}
