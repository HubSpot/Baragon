package com.hubspot.baragon.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.models.ServiceSnapshot;
import com.hubspot.baragon.utils.LeaderUtils;
import com.hubspot.baragon.utils.LockUtils;
import com.hubspot.baragon.utils.LogUtils;
import com.hubspot.baragon.utils.ResponseUtils;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class BaragonAgentManager {
  private static final Log LOG = LogFactory.getLog(BaragonAgentManager.class);
  
  private final AsyncHttpClient asyncHttpClient;
  private final LeaderLatch leaderLatch;
  private final ObjectMapper objectMapper;
  private final ReentrantLock clusterLock;

  @Inject
  public BaragonAgentManager(@Named(BaragonAgentServiceModule.LB_CLUSTER_LOCK) ReentrantLock clusterLock,
                             ObjectMapper objectMapper, AsyncHttpClient asyncHttpClient, LeaderLatch leaderLatch) {
    this.clusterLock = clusterLock;
    this.objectMapper = objectMapper;
    this.asyncHttpClient = asyncHttpClient;
    this.leaderLatch = leaderLatch;
  }



  public Collection<String> getCluster() {
    return LeaderUtils.getParticipantIds(leaderLatch);
  }

  public void apply(final ServiceSnapshot snapshot) {
    LogUtils.serviceInfoMessage(LOG, snapshot.getServiceInfo(), "Applying with %s", Joiner.on(", ").join(snapshot.getHealthyUpstreams()));

    LockUtils.tryLock(clusterLock, 30, TimeUnit.SECONDS);

    final Collection<String> successfulNodes = Lists.newLinkedList();
    final Collection<String> unsuccessfulNodes = Lists.newLinkedList();
    final Collection<Future<?>> futures = Lists.newLinkedList();

    try {
      for (final String id : getCluster()) {
        futures.add(asyncHttpClient.preparePost(String.format("http://%s/baragon-agent/v1/internal/configs", id))
            .setHeader("Content-Type", "application/json")
            .setBody(objectMapper.writeValueAsBytes(snapshot))
            .execute(new AsyncCompletionHandler<Object>() {
              @Override
              public Object onCompleted(Response response) throws Exception {
                if (ResponseUtils.isSuccess(response)) {
                  LogUtils.serviceInfoMessage(LOG, snapshot.getServiceInfo(), "    %s SUCCESS", id);
                  successfulNodes.add(id);
                } else {
                  LogUtils.serviceInfoMessage(LOG, snapshot.getServiceInfo(), "    %s FAIL (HTTP %d)", id, response.getStatusCode());
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
        LogUtils.serviceInfoMessage(LOG, snapshot.getServiceInfo(), "Apply failed for: %s", LogUtils.COMMA_JOINER.join(unsuccessfulNodes));
      } else {
        LogUtils.serviceInfoMessage(LOG, snapshot.getServiceInfo(), "Apply succeeded!");
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    } finally {
      clusterLock.unlock();
    }
  }



  public Map<String, Boolean> checkConfigs() {
    LOG.info("Going to check configs");

    LockUtils.tryLock(clusterLock, 30, TimeUnit.SECONDS);

    final ConcurrentMap<String, Boolean> status = Maps.newConcurrentMap();

    try {
      Collection<Future<?>> futures = Lists.newLinkedList();
      for (final String id : LeaderUtils.getParticipantIds(leaderLatch)) {
        futures.add(asyncHttpClient.prepareGet(String.format("http://%s/baragon-agent/v1/internal/configs/check", id))
            .execute(new AsyncCompletionHandler<Object>() {
              @Override
              public Object onCompleted(Response response) throws Exception {
                status.put(id, ResponseUtils.isSuccess(response));
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
