package com.hubspot.baragon.data;

import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.zookeeper.KeeperException;

@Singleton
public class BaragonWorkerDatastore extends AbstractDataStore {
  public static final String WORKERS_FORMAT = "/workers";
  public static final String WORKER_FORMAT = WORKERS_FORMAT + "/%s";

  @Inject
  public BaragonWorkerDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public LeaderLatch createLeaderLatch(String baseUri) {
    return new LeaderLatch(curatorFramework, WORKERS_FORMAT, baseUri);
  }

  public Collection<String> getBaseUris() {
    final Collection<String> nodes = getChildren(WORKERS_FORMAT);

    if (nodes.isEmpty()) {
      return Collections.emptyList();
    }

    final Collection<String> baseUrls = Lists.newArrayListWithCapacity(nodes.size());

    for (String node : nodes) {
      try {
        baseUrls.add(new String(curatorFramework.getData().forPath(String.format(WORKER_FORMAT, node)), Charsets.UTF_8));
      } catch (KeeperException.NoNodeException nne) {
        // uhh, didnt see that...
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }

    return baseUrls;
  }
}
