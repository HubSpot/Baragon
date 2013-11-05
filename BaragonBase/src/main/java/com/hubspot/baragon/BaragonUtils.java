package com.hubspot.baragon;

import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.zookeeper.KeeperException;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class BaragonUtils {
  private static final Log LOG = LogFactory.getLog(BaragonUtils.class);

  private final CuratorFramework curatorFramework;

  @Inject
  public BaragonUtils(CuratorFramework curator) {
    this.curatorFramework = curator;
  }

  public List<String> getLoadBalancerNames() {
    try {
      return curatorFramework.getChildren().forPath("/agent-leader");
    } catch (KeeperException.NoNodeException e) {
      return Collections.emptyList();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  public LeaderLatch getAgentLeaderLatch(String loadBalancerName) {
    return new LeaderLatch(curatorFramework, String.format("/agent-leader/%s", loadBalancerName));
  }

  public InterProcessMutex acquireLoadBalancerLock(final String loadBalancer) {
    LOG.info("Acquiring lock on LB " + loadBalancer);
    final InterProcessMutex lock = new InterProcessMutex(curatorFramework, getLoadBalancerLockPath(loadBalancer));

    try {
      if (!lock.acquire(30, TimeUnit.SECONDS)) {
        throw new RuntimeException(String.format("Could not acquire lock to update load balancer %s", loadBalancer));
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    return lock;
  }

  public InterProcessMutex acquireProjectLock(final String project) {
    LOG.info("Acquiring lock for project " + project);
    final InterProcessMutex lock = new InterProcessMutex(curatorFramework, getProjectLockPath(project));

    try {
      if (!lock.acquire(30, TimeUnit.SECONDS)) {
        throw new RuntimeException(String.format("Could not acquire lock to update project %s", project));
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    return lock;
  }

  public void release(InterProcessMutex lock) {
    try {
      lock.release();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private String getGenericLockPath(String name) {
    return String.format("/locks/%s", name);
  }

  private String getLoadBalancerLockPath(final String loadBalancer) {
    return getGenericLockPath(String.format("load-balancer/%s", loadBalancer));
  }

  private String getProjectLockPath(final String project) {
    return getGenericLockPath(String.format("project/%s", project));
  }

}
