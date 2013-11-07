package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.ServiceInfo;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.Participant;
import org.apache.zookeeper.KeeperException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Singleton
public class BaragonDataStore {
  private final CuratorFramework curatorFramework;
  private final ObjectMapper objectMapper;

  @Inject
  public BaragonDataStore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    this.curatorFramework = curatorFramework;
    this.objectMapper = objectMapper;
  }

  private <T> Optional<T> readFromZk(String path, Class<T> klass) {
    try {
      return Optional.of(objectMapper.readValue(curatorFramework.getData().forPath(path), klass));
    } catch (KeeperException.NoNodeException nne) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private <T> boolean writeToZk(String path, T data) {
    try {
    final byte[] serializedInfo = objectMapper.writeValueAsBytes(data);

    if (curatorFramework.checkExists().forPath(path) != null) {
      curatorFramework.setData().forPath(path, serializedInfo);
      return false;
    } else {
      curatorFramework.create().creatingParentsIfNeeded().forPath(path, serializedInfo);
      return true;
    }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private String buildPendingServicePath(String name) {
    return String.format("/services/pending/%s", name);
  }

  private String buildActiveServicePath(String name) {
    return String.format("/services/active/%s", name);
  }

  private String buildActiveServicesPath() {
    return "/services/active";
  }

  private String buildPendingServicesPath() {
    return "/services/pending";
  }

  private String buildHealthyUpstreamsPath(String name, String id) {
    return String.format("/upstreams/healthy/%s/%s", name, id);
  }

  private String buildHealthyUpstreamPath(String name, String id, String upstream) {
    return String.format("/upstreams/healthy/%s/%s/%s", name, id, upstream);
  }

  private String buildUnhealthyUpstreamsPath(String name, String id) {
    return String.format("/upstreams/unhealthy/%s/%s", name, id);
  }

  private String buildUnhealthyUpstreamPath(String name, String id, String upstream) {
    return String.format("/upstreams/unhealthy/%s/%s/%s", name, id, upstream);
  }

  // pending services
  public boolean hasPendingService(String name) {
    try {
      return curatorFramework.checkExists().forPath(buildPendingServicePath(name)) != null;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Collection<String> getPendingServices() {
    try {
      return curatorFramework.getChildren().forPath(buildPendingServicesPath());
    } catch (KeeperException.NoNodeException nne) {
      return Collections.emptyList();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Optional<ServiceInfo> getPendingService(String name) {
    return readFromZk(buildPendingServicePath(name), ServiceInfo.class);
  }

  public void addPendingService(ServiceInfo serviceInfo) {
    if (hasPendingService(serviceInfo.getName())) {
      throw new RuntimeException("already a pending service");
    }

    writeToZk(buildPendingServicePath(serviceInfo.getName()), serviceInfo);
  }

  public boolean removePendingService(String name) {
    try {
      curatorFramework.delete().forPath(buildPendingServicePath(name));
      return true;
    } catch (KeeperException.NoNodeException nne) {
      return false;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  // active services
  public Collection<String> getActiveServices() {
    try {
      return curatorFramework.getChildren().forPath(buildActiveServicesPath());
    } catch (KeeperException.NoNodeException nne) {
      return Collections.emptyList();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Optional<ServiceInfo> getActiveService(String name) {
    return readFromZk(buildActiveServicePath(name), ServiceInfo.class);
  }

  public void makeServiceActive(String name) {
    Optional<ServiceInfo> maybeServiceInfo = getPendingService(name);

    if (!maybeServiceInfo.isPresent()) {
      throw new RuntimeException(String.format("No such pending service name: %s", name));
    }

    writeToZk(buildActiveServicePath(name), maybeServiceInfo.get());

    removePendingService(name);
  }

  public boolean removeActiveService(String name) {
    try {
      curatorFramework.delete().forPath(buildActiveServicePath(name));
      return true;
    } catch (KeeperException.NoNodeException nne) {
      return false;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  // unhealthy upstreams
  public Collection<String> getUnhealthyUpstreams(String name, String id) {
    try {
      return curatorFramework.getChildren().forPath(buildUnhealthyUpstreamsPath(name, id));
    } catch (KeeperException.NoNodeException nne) {
      return Collections.emptyList();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public boolean addUnhealthyUpstream(String name, String id, String upstream) {
    try {
      curatorFramework.create().creatingParentsIfNeeded().forPath(buildUnhealthyUpstreamPath(name, id, upstream));
      return true;
    } catch (KeeperException.NodeExistsException e) {
      return false;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public boolean removeUnhealthyUpstream(String name, String id, String upstream) {
    try {
      curatorFramework.delete().forPath(buildUnhealthyUpstreamPath(name, id, upstream));
      return true;
    } catch (KeeperException.NoNodeException e) {
      return false;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  // healthy upstreams
  public Collection<String> getHealthyUpstreams(String name, String id) {
    try {
      return curatorFramework.getChildren().forPath(buildHealthyUpstreamsPath(name, id));
    } catch (KeeperException.NoNodeException e) {
      return Collections.emptyList();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public void makeUpstreamHealthy(String name, String id, String upstream) {
    if (!getUnhealthyUpstreams(name, id).contains(upstream)) {
      throw new RuntimeException(String.format("No such unhealthy upstream: %s", upstream));
    }

    try {
      curatorFramework.create().creatingParentsIfNeeded().forPath(buildHealthyUpstreamPath(name, id, upstream));
    } catch (KeeperException.NodeExistsException e) {
      // weird
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    removeUnhealthyUpstream(name, id, upstream);
  }

  public void makeUpstreamUnhealthy(String name, String id, String upstream) {
    if (!getHealthyUpstreams(name, id).contains(upstream)) {
      throw new RuntimeException(String.format("No such healthy upstream: %s", upstream));
    }

    try {
      curatorFramework.create().creatingParentsIfNeeded().forPath(buildUnhealthyUpstreamPath(name, id, upstream));
    } catch (KeeperException.NodeExistsException e) {
      // weird
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }

    removeHealthyUpstream(name, id, upstream);
  }

  public boolean removeHealthyUpstream(String name, String id, String upstream) {
    try {
      curatorFramework.delete().forPath(buildHealthyUpstreamPath(name, id, upstream));
      return true;
    } catch (KeeperException.NoNodeException e) {
      return false;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public List<String> getLoadBalancers() {
    try {
      return curatorFramework.getChildren().forPath("/agent-leader");
    } catch (KeeperException.NoNodeException e) {
      return Collections.emptyList();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public List<String> getLoadBalancerHosts(String name) {
    try {
      final LeaderLatch latch = new LeaderLatch(curatorFramework, String.format("/agent-leader/%s", name));
      List<String> hosts = Lists.newArrayList();
      for (Participant participant : latch.getParticipants()) {
        hosts.add(participant.getId());
      }
      return hosts;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public Optional<String> getLoadBalancerLeader(String name) {
    try {
      final LeaderLatch latch = new LeaderLatch(curatorFramework, String.format("/agent-leader/%s", name));
      return Optional.of(latch.getLeader().getId());
    } catch (KeeperException.NoNodeException nne) {
      return Optional.absent();
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
