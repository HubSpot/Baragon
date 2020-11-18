package com.hubspot.baragon.service.managers;

import static com.hubspot.baragon.service.BaragonServiceModule.BARAGON_SERVICE_SYNC_HTTP_CLIENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.exceptions.AgentServiceNotifyException;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.exceptions.BaragonWebException;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Method;
import com.hubspot.horizon.HttpResponse;
import com.hubspot.horizon.ning.NingHttpClient;

public class PurgeCacheManager {

  private static final Logger LOG = LoggerFactory.getLogger(PurgeCacheManager.class);

  private final BaragonStateDatastore stateDatastore;
  private final AgentManager agentManager;
  private final BaragonConfiguration baragonConfiguration;
  private final NingHttpClient httpClient;


  @Inject
  public PurgeCacheManager(BaragonStateDatastore stateDatastore,
                           AgentManager agentManager,
                           BaragonConfiguration baragonConfiguration,
                           @Named(BARAGON_SERVICE_SYNC_HTTP_CLIENT) NingHttpClient httpClient){
    this.stateDatastore = stateDatastore;
    this.agentManager = agentManager;
    this.baragonConfiguration = baragonConfiguration;
    this.httpClient = httpClient;
  }

  public List<HttpResponse> synchronouslyPurgeCache(String serviceId) throws Exception {
    final Set<String> loadBalancers = Sets.newHashSet();
    final Optional<BaragonService> maybeOriginalService = stateDatastore.getService(serviceId);
    if (maybeOriginalService.isPresent()) {
      loadBalancers.addAll(maybeOriginalService.get().getLoadBalancerGroups());
    }
    else {
      throw new BaragonWebException(String.format("Service with id=%s could not be found", serviceId));
    }

    List<BaragonAgentMetadata> agents = agentManager.getAgents(loadBalancers).stream().collect(Collectors.toList());
    List<HttpResponse> responses = new ArrayList<>();
    // go around to each agent, and execute the purgeCache command
    for (BaragonAgentMetadata agent: agents){
      final HttpRequest.Builder builder = HttpRequest.newBuilder()
          .setUrl(
              String.format(
                baragonConfiguration.getAgentPurgeCacheRequestUriFormat(),
                agent.getBaseAgentUri(),
                serviceId
              )
          )
          .setMethod(Method.POST);

      HttpResponse response = httpClient.execute(builder.build());
      LOG.info(String.format("Got %s response from BaragonAgent", response.getStatusCode()));
      if (response.isError()) {
        throw new AgentServiceNotifyException(String.format("Bad response received from BaragonService %s", response.getAsString()));
      }
      responses.add(response);
    }
    return responses;


  }
}
