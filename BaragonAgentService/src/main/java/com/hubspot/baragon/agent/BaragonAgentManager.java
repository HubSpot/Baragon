package com.hubspot.baragon.agent;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.inject.name.Named;
import com.hubspot.baragon.models.ServiceInfoAndUpstreams;
import com.hubspot.baragon.models.ServiceInfo;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.Participant;

public class BaragonAgentManager {
  private static final Log LOG = LogFactory.getLog(BaragonAgentManager.class);
  
  private final AsyncHttpClient asyncHttpClient;
  private final LeaderLatch leaderLatch;
  private final ObjectMapper objectMapper;
  private final ReentrantLock clusterLock;

  private void tryLock(long timeout, TimeUnit timeUnit) {
    try {
      if (!clusterLock.tryLock(timeout, timeUnit)) {
        throw new RuntimeException("Failed to acquire lock");
      }
    } catch (InterruptedException e) {
      throw Throwables.propagate(e);
    }
  }
  
  @Inject
  public BaragonAgentManager(@Named(BaragonAgentServiceModule.LB_CLUSTER_LOCK) ReentrantLock clusterLock, ObjectMapper objectMapper, AsyncHttpClient asyncHttpClient, LeaderLatch leaderLatch) {
    this.clusterLock = clusterLock;
    this.objectMapper = objectMapper;
    this.asyncHttpClient = asyncHttpClient;
    this.leaderLatch = leaderLatch;
  }

  public void checkForLeader() {
    if (!leaderLatch.hasLeadership()) {
      throw new RuntimeException("Not leader!");
    }
  }
  
  private Collection<String> getParticipantIds() throws Exception {
    Collection<String> results = Lists.newArrayList();
    
    for (Participant p : leaderLatch.getParticipants()) {
      results.add(p.getId());
    }
    
    return results;
  }

  public Collection<String> getCluster() {
    Collection<String> results = Lists.newLinkedList();
    
    try {
      for (Participant p : leaderLatch.getParticipants()) {
        results.add(p.getId());
      }
    } catch (Exception e) {
      Throwables.propagate(e);
    }
    
    return results;
  }

  private boolean isSuccess(Response response) {
    return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
  }

  public void apply(final ServiceInfo serviceInfo, final Collection<String> upstreams) {
    LOG.info("Going to apply deploy " + serviceInfo.getName());

    tryLock(30, TimeUnit.SECONDS);

    final Collection<String> successfulNodes = Lists.newLinkedList();
    final Collection<String> unsuccessfulNodes = Lists.newLinkedList();
    final Collection<Future<?>> futures = Lists.newLinkedList();

    try {
      Collection<String> ids = getParticipantIds();

      for (final String id : ids) {
        futures.add(asyncHttpClient.preparePost(String.format("http://%s/baragon-agent/v1/internal/configs", id))
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsBytes(new ServiceInfoAndUpstreams(serviceInfo, upstreams)))
            .execute(new AsyncCompletionHandler<Object>() {
              @Override
              public Object onCompleted(Response response) throws Exception {
                if (isSuccess(response)) {
                  successfulNodes.add(id);
                } else {
                  unsuccessfulNodes.add(id);
                }
                return null;
              }
            }));
      }

      for (Future<?> future : futures) {
        future.get();
      }
      
      if (unsuccessfulNodes.size() > 0) {
        LOG.info("Need to rollback!");
        // TODO: rollback
      }

    } catch (Exception e) {
      throw Throwables.propagate(e);
    } finally {
      clusterLock.unlock();
    }
  }

  public Map<String, Boolean> checkConfigs() {
    LOG.info("Going to check configs");

    tryLock(30, TimeUnit.SECONDS);

    final ConcurrentMap<String, Boolean> status = Maps.newConcurrentMap();

    try {
      Collection<Future<?>> futures = Lists.newLinkedList();
      for (final String id : getParticipantIds()) {
        futures.add(asyncHttpClient.prepareGet(String.format("http://%s/baragon-agent/v1/internal/configs/check", id))
            .execute(new AsyncCompletionHandler<Object>() {
              @Override
              public Object onCompleted(Response response) throws Exception {
                status.put(id, isSuccess(response));
                return null;
              }
            }));
      }

      for (Future<?> future : futures) {
        future.get();
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    } finally {
      clusterLock.unlock();
    }
    
    return status;
  }
}
