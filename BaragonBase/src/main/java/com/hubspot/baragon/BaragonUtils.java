package com.hubspot.baragon;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.zookeeper.KeeperException;

import java.util.Collections;
import java.util.List;

@Singleton
public class BaragonUtils {
  private static final Log LOG = LogFactory.getLog(BaragonUtils.class);

  private final CuratorFramework curatorFramework;

  @Inject
  public BaragonUtils(CuratorFramework curator) {
    this.curatorFramework = curator;
  }
  
  public LeaderLatch getAgentLeaderLatch(String loadBalancerName) {
    return new LeaderLatch(curatorFramework, String.format("/agent-leader/%s", loadBalancerName));
  }
}
