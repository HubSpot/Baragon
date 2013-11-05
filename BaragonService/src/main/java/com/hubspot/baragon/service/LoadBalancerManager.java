package com.hubspot.baragon.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.hubspot.baragon.data.BaragonDataStore;
import com.hubspot.baragon.models.ServiceInfo;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Inject;
import com.hubspot.baragon.models.ServiceInfoAndUpstreams;

public class LoadBalancerManager {
  private static final Log LOG = LogFactory.getLog(LoadBalancerManager.class);
  
  private final BaragonDataStore datastore;
  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper;
  
  @Inject
  public LoadBalancerManager(BaragonDataStore datastore, AsyncHttpClient asyncHttpClient, ObjectMapper objectMapper) {
    this.asyncHttpClient = asyncHttpClient;
    this.datastore = datastore;
    this.objectMapper = objectMapper;
  }

  public List<String> getLoadBalancers() {
    return datastore.getLoadBalancers();
  }
  
  public List<String> getLoadBalancerHosts(String loadBalancer) {
    return datastore.getLoadBalancerHosts(loadBalancer);
  }

  public void apply(String loadBalancer, ServiceInfo serviceInfo, Collection<String> upstreams) {
    boolean success = true;
    
    LOG.info("Deploying to LB " + loadBalancer);
    try {
      Optional<String> maybeLeader = datastore.getLoadBalancerLeader(loadBalancer);
      if (!maybeLeader.isPresent()) {
        throw new RuntimeException("No leader for LB " + loadBalancer);
      }
      LOG.info("   Leader: " + maybeLeader.get());

      Response response = asyncHttpClient.preparePost(String.format("http://%s/baragon-agent/v1/external/configs", maybeLeader.get()))
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
      Optional<String> maybeLeader = datastore.getLoadBalancerLeader(loadBalancer);
      if (!maybeLeader.isPresent()) {
        throw new RuntimeException("No leader for LB " + loadBalancer);
      }

      LOG.info("   Leader: " + maybeLeader.get());

      Response response = asyncHttpClient.prepareGet(String.format("http://%s/baragon-agent/v1/external/configs", maybeLeader.get()))
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
