package com.hubspot.baragon.service.listeners;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAgentWatchListener extends AbstractLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(AgentAddedElbListener.class);

  private final CuratorFramework curatorFramework;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;

  private Map<String, PathChildrenCache> agentCaches = new HashMap<>();
  private PathChildrenCache groupsCache = null;

  @Inject
  public AbstractAgentWatchListener(CuratorFramework curatorFramework,
                                    BaragonLoadBalancerDatastore loadBalancerDatastore) {
    this.curatorFramework = curatorFramework;
    this.loadBalancerDatastore = loadBalancerDatastore;
  }

  @Override
  public void isLeader() {
    LOG.info("We are the leader! Starting agent listeners...");
    try {
      groupsCache = new PathChildrenCache(curatorFramework, BaragonLoadBalancerDatastore.LOAD_BALANCER_GROUPS_FORMAT, true);
      groupsCache.start();
      addGroupListener(groupsCache);

      for (BaragonGroup group : loadBalancerDatastore.getLoadBalancerGroups()) {
        PathChildrenCache cache = new PathChildrenCache(curatorFramework, String.format(BaragonLoadBalancerDatastore.LOAD_BALANCER_GROUP_HOSTS_FORMAT, group.getName()), true);
        cache.start();
        addAgentsListener(cache);
        agentCaches.put(group.getName(), cache);
      }
    } catch (Exception e) {
      LOG.warn("Error starting caches", e);
    }
  }

  @Override
  public void notLeader() {
    LOG.info("We are not the leader!");
    try {
      if (groupsCache != null) {
        groupsCache.close();
      }
    } catch (IOException e) {
      LOG.warn("Error closing cache listener: ", e);
    }
    if (!agentCaches.isEmpty()) {
      try {
        for (PathChildrenCache cache : agentCaches.values()) {
          cache.close();
        }
      } catch (IOException e) {
        LOG.warn("Error closing cache listener: ", e);
      }
    }
  }

  public abstract void agentUpdated(BaragonAgentMetadata agent, Optional<BaragonGroup> group, String groupName);

  public abstract void agentRemoved(PathChildrenCacheEvent event);

  private void addGroupListener(PathChildrenCache cache) {

    PathChildrenCacheListener listener = new PathChildrenCacheListener() {
      public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        switch (event.getType()) {
          case CHILD_ADDED:
            String newGroup = ZKPaths.getNodeFromPath(event.getData().getPath());
            LOG.info(String.format("Found group %s, starting listeners", newGroup));
            if (!agentCaches.containsKey(newGroup)) {
              String path = String.format(BaragonLoadBalancerDatastore.LOAD_BALANCER_GROUP_HOSTS_FORMAT, newGroup);
              PathChildrenCache cache = new PathChildrenCache(curatorFramework, path, true);
              cache.start();
              addAgentsListener(cache);
              agentCaches.put(newGroup, cache);
            }
            break;
          case CHILD_REMOVED:
            String removedGroup = ZKPaths.getNodeFromPath(event.getData().getPath());
            LOG.info(String.format("group %s was removed, stopping listeners", removedGroup));
            if (agentCaches.containsKey(removedGroup)) {
              agentCaches.get(removedGroup).close();
              agentCaches.remove(removedGroup);
            }
            break;
          default:
            LOG.debug(event.getType().toString());
            break;
        }
      }
    };
    cache.getListenable().addListener(listener);
  }

  private void addAgentsListener(PathChildrenCache cache) {

    PathChildrenCacheListener listener = new PathChildrenCacheListener() {
      public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        switch (event.getType()) {
          case CHILD_UPDATED:
          case CHILD_ADDED:
            LOG.debug(String.format("Agent with latch %s added", ZKPaths.getNodeFromPath(event.getData().getPath())));
            Optional<BaragonAgentMetadata> maybeAgent = loadBalancerDatastore.getAgent(event.getData().getPath());
            if (maybeAgent.isPresent()) {
              List<String> pathElements = Arrays.asList(event.getData().getPath().split("/"));
              String groupName = pathElements.get(pathElements.size() - 3);
              Optional<BaragonGroup> group = loadBalancerDatastore.getLoadBalancerGroup(groupName);
              agentUpdated(maybeAgent.get(), group, groupName);
            } else {
              LOG.warn(String.format("Could not get agent data for agent %s", event.getData().getPath()));
            }
            break;
          case CHILD_REMOVED:
            LOG.debug(String.format("Agent with latch %s removed", ZKPaths.getNodeFromPath(event.getData().getPath())));
            agentRemoved(event);
          default:
            LOG.debug(event.getType().toString());
            break;
        }
      }
    };
    cache.getListenable().addListener(listener);
  }
}
