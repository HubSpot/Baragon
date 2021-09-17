package com.hubspot.baragon.service.managers;

import static com.hubspot.baragon.service.BaragonServiceModule.BARAGON_SERVICE_SYNC_HTTP_CLIENT;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Optional;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.exceptions.AgentServiceNotifyException;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonConfigFile;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.service.exceptions.BaragonWebException;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpResponse;
import com.hubspot.horizon.ning.NingHttpClient;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenderedConfigsManager {
  private static final Logger LOG = LoggerFactory.getLogger(RenderedConfigsManager.class);
  private static final Random RANDOM = new Random();
  private static final TypeReference<List<BaragonConfigFile>> BARAGON_CONFIG_FILE_LIST_TYPE_REFERENCE = new TypeReference<List<BaragonConfigFile>>() {};

  private final BaragonStateDatastore stateDatastore;
  private final AgentManager agentManager;
  private final NingHttpClient httpClient;

  @Inject
  public RenderedConfigsManager(
    BaragonStateDatastore stateDatastore,
    AgentManager agentManager,
    @Named(BARAGON_SERVICE_SYNC_HTTP_CLIENT) NingHttpClient httpClient
  ) {
    this.stateDatastore = stateDatastore;
    this.agentManager = agentManager;
    this.httpClient = httpClient;
  }

  public List<BaragonConfigFile> synchronouslySendRenderConfigsRequest(String serviceId)
    throws Exception {
    final Set<String> loadBalancers = Sets.newHashSet();
    final Optional<BaragonService> maybeOriginalService = stateDatastore.getService(
      serviceId
    );
    if (maybeOriginalService.isPresent()) {
      loadBalancers.addAll(maybeOriginalService.get().getLoadBalancerGroups());
    } else {
      throw new BaragonWebException(
        String.format("Service with id=%s could not be found", serviceId)
      );
    }

    // get relevant agents, and select one
    List<BaragonAgentMetadata> agents = agentManager
      .getAgents(loadBalancers)
      .stream()
      .collect(Collectors.toList());

    BaragonAgentMetadata randomAgent = agents.get(RANDOM.nextInt(agents.size()));
    final String baseUrl = randomAgent.getBaseAgentUri();

    final HttpRequest.Builder builder = HttpRequest
      .newBuilder()
      .setUrl(baseUrl + "/renderedConfigs/" + serviceId);

    HttpResponse response = httpClient.execute(builder.build());
    LOG.info(
      String.format("Got %s response from BaragonAgent", response.getStatusCode())
    );
    if (response.isError()) {
      throw new AgentServiceNotifyException(
        String.format(
          "Bad response received from BaragonService %s",
          response.getAsString()
        )
      );
    }
    return response.getAs(BARAGON_CONFIG_FILE_LIST_TYPE_REFERENCE);
  }
}
