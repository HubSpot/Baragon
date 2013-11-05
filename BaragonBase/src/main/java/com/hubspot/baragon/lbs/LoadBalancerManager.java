package com.hubspot.baragon.lbs;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.hubspot.baragon.models.ServiceInfo;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import com.hubspot.baragon.BaragonUtils;
import com.hubspot.baragon.lbs.models.ServiceInfoAndUpstreams;

public class LoadBalancerManager {
  private static final Log LOG = LogFactory.getLog(LoadBalancerManager.class);
  
  private final BaragonUtils utils;
  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper;
  
  @Inject
  public LoadBalancerManager(BaragonUtils utils, AsyncHttpClient asyncHttpClient, ObjectMapper objectMapper) {
    this.asyncHttpClient = asyncHttpClient;
    this.utils = utils;
    this.objectMapper = objectMapper;
  }

  public List<String> getLoadBalancers() {
    return utils.getLoadBalancerNames();
  }
  
  public List<String> getLoadBalancerHosts(String loadBalancer) {
    try {
      String leader = utils.getAgentLeaderLatch(loadBalancer).getLeader().getId();

      Response response = asyncHttpClient.prepareGet(String.format("http://%s/baragon-agent/v1/external/cluster", leader))
          .execute().get();

      return objectMapper.readValue(response.getResponseBodyAsStream(), new TypeReference<List<String>>() { });
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public void apply(String loadBalancer, ServiceInfo serviceInfo, Collection<String> upstreams) {
    boolean success = true;
    
    LOG.info("Deploying to LB " + loadBalancer);
    try {
      String leader = utils.getAgentLeaderLatch(loadBalancer).getLeader().getId();
      LOG.info("   Leader: " + leader);

      Response response = asyncHttpClient.preparePost(String.format("http://%s/baragon-agent/v1/external/configs", leader))
          .addHeader("Content-Type", "application/json")
          .setBody(objectMapper.writeValueAsBytes(new ServiceInfoAndUpstreams(serviceInfo, upstreams)))
          .execute().get();

      LOG.info("    Response: " + response.getStatusCode() + ", " + response.getResponseBody());
      
      // TODO: rollback
    } catch (Exception e) {
      Throwables.propagate(e);
    }
  }

  public Map<String, Boolean> checkConfigs(String loadBalancer) {
    try {
      String leader = utils.getAgentLeaderLatch(loadBalancer).getLeader().getId();
      LOG.info("   Leader: " + leader);

      Response response = asyncHttpClient.prepareGet(String.format("http://%s/baragon-agent/v1/external/configs", leader))
          .execute().get();


      
      if (isSuccess(response)) {
        return objectMapper.readValue(response.getResponseBodyAsStream(), new TypeReference<Map<String, Boolean>>() {});
      } else {
        throw new RuntimeException(response.getResponseBody());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean isSuccess(Response response) {
    return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
  }
}
