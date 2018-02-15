package com.hubspot.baragon.client;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonGroup;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.BaragonServiceStatus;
import com.hubspot.baragon.models.QueuedRequestId;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Method;
import com.hubspot.horizon.HttpResponse;

public class BaragonServiceClient {

  private static final Logger LOG = LoggerFactory.getLogger(BaragonServiceClient.class);

  private static final String WORKERS_FORMAT = "%s/workers";

  private static final String LOAD_BALANCER_FORMAT = "%s/load-balancer";
  private static final String LOAD_BALANCER_BASE_PATH_FORMAT = LOAD_BALANCER_FORMAT + "/%s/base-path";
  private static final String LOAD_BALANCER_ALL_BASE_PATHS_FORMAT = LOAD_BALANCER_BASE_PATH_FORMAT + "/all";
  private static final String LOAD_BALANCER_AGENTS_FORMAT = LOAD_BALANCER_FORMAT + "/%s/agents";
  private static final String LOAD_BALANCER_KNOWN_AGENTS_FORMAT = LOAD_BALANCER_FORMAT + "/%s/known-agents";
  private static final String LOAD_BALANCER_DELETE_KNOWN_AGENT_FORMAT = LOAD_BALANCER_KNOWN_AGENTS_FORMAT + "/%s";
  private static final String LOAD_BALANCER_GROUP_FORMAT = LOAD_BALANCER_FORMAT + "/%s";
  private static final String ALL_LOAD_BALANCER_GROUPS_FORMAT = LOAD_BALANCER_FORMAT + "/all";
  private static final String LOAD_BALANCER_TRAFFIC_SOURCE_FORMAT = LOAD_BALANCER_GROUP_FORMAT + "/sources";

  private static final String REQUEST_FORMAT = "%s/request";
  private static final String REQUEST_ID_FORMAT = REQUEST_FORMAT + "/%s";

  private static final String STATE_FORMAT = "%s/state";
  private static final String STATE_SERVICE_ID_FORMAT = STATE_FORMAT + "/%s";
  private static final String STATE_RELOAD_FORMAT = STATE_SERVICE_ID_FORMAT + "/reload";

  private static final String STATUS_FORMAT = "%s/status";

  private static final TypeReference<Collection<String>> STRING_COLLECTION = new TypeReference<Collection<String>>() {};
  private static final TypeReference<Collection<BaragonGroup>> BARAGON_GROUP_COLLECTION = new TypeReference<Collection<BaragonGroup>>() {};
  private static final TypeReference<Collection<BaragonAgentMetadata>> BARAGON_AGENTS_COLLECTION = new TypeReference<Collection<BaragonAgentMetadata>>() {};
  private static final TypeReference<Collection<QueuedRequestId>> QUEUED_REQUEST_COLLECTION = new TypeReference<Collection<QueuedRequestId>>() {};
  private static final TypeReference<Collection<BaragonServiceState>> BARAGON_SERVICE_STATE_COLLECTION = new TypeReference<Collection<BaragonServiceState>>() {};

  private final Random random;
  private final Provider<List<String>> baseUrlProvider;
  private final Provider<Optional<String>> authkeyProvider;

  private final HttpClient httpClient;

  public BaragonServiceClient(String contextPath, HttpClient httpClient, List<String> hosts, Optional<String> authkey) {
    this(
        httpClient,
        ProviderUtils.of(ImmutableList.copyOf(hosts.stream().map((h) -> String.format("%s/%s", h, contextPath)).collect(Collectors.toList()))),
        ProviderUtils.of(authkey)
    );
  }

  public BaragonServiceClient(HttpClient httpClient, Provider<List<String>> baseUrlProvider, Provider<Optional<String>> authkeyProvider) {
    this.httpClient = httpClient;
    this.baseUrlProvider = baseUrlProvider;
    this.authkeyProvider = authkeyProvider;
    this.random = new Random();
  }

  private String getBaseUrl() {
    final List<String> baseUrls = baseUrlProvider.get();
    String chosenBaseUrl = baseUrls.get(random.nextInt(baseUrls.size()));
    if (chosenBaseUrl.endsWith("/")) {
      return chosenBaseUrl.substring(0, chosenBaseUrl.length() - 1);
    } else {
      return chosenBaseUrl;
    }
  }

  private HttpRequest.Builder buildRequest(String uri) {
    return buildRequest(uri, null);
  }

  private HttpRequest.Builder buildRequest(String uri, Map<String, String> queryParams) {
    final HttpRequest.Builder builder = HttpRequest.newBuilder().setUrl(uri);

    final Optional<String> maybeAuthkey = authkeyProvider.get();

    if (maybeAuthkey.isPresent()) {
      builder.setQueryParam("authkey").to(maybeAuthkey.get());
    }

    if ((queryParams != null) && (!queryParams.isEmpty())) {
      for (Map.Entry<String, String> entry : queryParams.entrySet()) {
        builder.setQueryParam(entry.getKey()).to(entry.getValue());
      }
    }

    return builder;
  }

  private void checkResponse(String type, HttpResponse response) {
    if (response.isError()) {
      throw fail(type, response);
    }
  }

  private BaragonClientException fail(String type, HttpResponse response) {
    String body = "";
    try {
      body = response.getAsString();
    } catch (Exception e) {
      LOG.warn("Unable to read body", e);
    }

    String uri = "";
    try {
      uri = response.getRequest().getUrl().toString();
    } catch (Exception e) {
      LOG.warn("Unable to read uri", e);
    }

    throw new BaragonClientException(String.format("Failed '%s' action on Baragon (%s) - code: %s, %s", type, uri, response.getStatusCode(), body), response.getStatusCode());
  }

  private <T> Optional<T> getSingle(String uri, String type, String id, Class<T> clazz) {
    return getSingle(uri, type, id, clazz, null);
  }

  private <T> Optional<T> getSingle(String uri, String type, String id, Class<T> clazz, Map<String, String> queryParams) {
    checkNotNull(id, String.format("Provide a %s id", type));
    LOG.debug("Getting {} {} from {}", type, id, uri);
    final long start = System.currentTimeMillis();

    HttpResponse response = httpClient.execute(buildRequest(uri, queryParams).build());

    if (response.getStatusCode() == 404) {
      return Optional.absent();
    }

    checkResponse(type, response);
    LOG.debug("Got {} {} in {}ms", type, id, System.currentTimeMillis() - start);
    return Optional.fromNullable(response.getAs(clazz));
  }

  private <T> Collection<T> getCollection(String uri, String type, TypeReference<Collection<T>> typeReference) {
    LOG.debug("Getting all {} from {}", type, uri);
    final long start = System.currentTimeMillis();

    HttpResponse response = httpClient.execute(buildRequest(uri).build());

    if (response.getStatusCode() == 404) {
      throw new BaragonClientException(String.format("%s not found", type), 404);
    }

    checkResponse(type, response);
    LOG.debug("Got {} in {}ms", type, System.currentTimeMillis() - start);
    return response.getAs(typeReference);
  }

  private <T> void delete(String uri, String type, String id, Map<String, String> queryParams) {
    delete(uri, type, id, queryParams, Optional.<Class<T>>absent());
  }

  private <T> Optional<T> delete(String uri, String type, String id, Map<String, String> queryParams, Optional<Class<T>> clazz) {
    LOG.debug("Deleting {} {} from {}", type, id, uri);
    final long start = System.currentTimeMillis();
    HttpRequest.Builder request = buildRequest(uri, queryParams).setMethod(Method.DELETE);
    HttpResponse response = httpClient.execute(request.build());

    if (response.getStatusCode() == 404) {
      LOG.debug("{} ({}) was not found", type, id);
      return Optional.absent();
    }

    checkResponse(type, response);
    LOG.debug("Deleted {} ({}) from Baragon in %sms", type, id, System.currentTimeMillis() - start);

    if (clazz.isPresent()) {
      return Optional.of(response.getAs(clazz.get()));
    }

    return Optional.absent();
  }

  private <T> Optional<T> post(String uri, String type, Optional<?> body, Optional<Class<T>> clazz) {
    return post(uri, type, body, clazz, Collections.<String, String>emptyMap());
  }

  private <T> Optional<T> post(String uri, String type, Optional<?> body, Optional<Class<T>> clazz, Map<String, String> queryParams) {
    try {
      HttpResponse response = post(uri, type, body, queryParams);

      if (clazz.isPresent()) {
        return Optional.of(response.getAs(clazz.get()));
      }
    } catch (Exception e) {
      LOG.warn("Http post failed", e);
    }

    return Optional.absent();
  }

  private HttpResponse post(String uri, String type, Optional<?> body, Map<String, String> params) {
    LOG.debug("Posting {} to {}", type, uri);
    final long start = System.currentTimeMillis();
    HttpRequest.Builder request = buildRequest(uri, params).setMethod(Method.POST);

    if (body.isPresent()) {
      request.setBody(body.get());
    }

    HttpResponse response = httpClient.execute(request.build());
    checkResponse(type, response);
    LOG.debug("Successfully posted {} in {}ms", type, System.currentTimeMillis() - start);
    return response;
  }

  // BaragonService overall status

  public Optional<BaragonServiceStatus> getBaragonServiceStatus(String baseUrl) {
    final String uri = String.format(STATUS_FORMAT, baseUrl);
    return getSingle(uri, "status", "", BaragonServiceStatus.class);
  }

  public Optional<BaragonServiceStatus> getAnyBaragonServiceStatus() {
    return getBaragonServiceStatus(getBaseUrl());
  }


  // BaragonService service states

  public Collection<BaragonServiceState> getGlobalState() {
    final String uri = String.format(STATE_FORMAT, getBaseUrl());
    return getCollection(uri, "global state", BARAGON_SERVICE_STATE_COLLECTION);
  }

  public Optional<BaragonServiceState> getServiceState(String serviceId) {
    final String uri = String.format(STATE_SERVICE_ID_FORMAT, getBaseUrl(), serviceId);
    return getSingle(uri, "service state", serviceId, BaragonServiceState.class);
  }

  public Optional<BaragonResponse> deleteService(String serviceId) {
    final String uri = String.format(STATE_SERVICE_ID_FORMAT, getBaseUrl(), serviceId);
    return delete(uri, "service state", serviceId, Collections.emptyMap(), Optional.of(BaragonResponse.class));
  }

  public Optional<BaragonResponse> reloadServiceConfigs(String serviceId){
    final String uri = String.format(STATE_RELOAD_FORMAT, getBaseUrl(), serviceId);
    return post(uri, "service reload",Optional.absent(), Optional.of(BaragonResponse.class));
  }


  // BaragonService Workers

  public Collection<String> getBaragonServiceWorkers() {
    final String requestUri = String.format(WORKERS_FORMAT, getBaseUrl());
    return getCollection(requestUri, "baragon service workers", STRING_COLLECTION);
  }


  // BaragonService load balancer group actions

  public Collection<String> getLoadBalancerGroups() {
    final String requestUri = String.format(LOAD_BALANCER_FORMAT, getBaseUrl());
    return getCollection(requestUri, "load balancer groups", STRING_COLLECTION);
  }

  public Collection<BaragonGroup> getAllLoadBalancerGroups() {
    final String requestUri = String.format(ALL_LOAD_BALANCER_GROUPS_FORMAT, getBaseUrl());
    return getCollection(requestUri, "load balancer groups", BARAGON_GROUP_COLLECTION);
  }

  public Collection<BaragonAgentMetadata> getLoadBalancerGroupAgentMetadata(String loadBalancerGroupName) {
    final String requestUri = String.format(LOAD_BALANCER_AGENTS_FORMAT, getBaseUrl(), loadBalancerGroupName);
    return getCollection(requestUri, "load balancer agent metadata", BARAGON_AGENTS_COLLECTION);
  }

  public Collection<BaragonAgentMetadata> getLoadBalancerGroupKnownAgentMetadata(String loadBalancerGroupName) {
    final String requestUri = String.format(LOAD_BALANCER_KNOWN_AGENTS_FORMAT, getBaseUrl(), loadBalancerGroupName);
    return getCollection(requestUri, "load balancer known agent metadata", BARAGON_AGENTS_COLLECTION);
  }

  public void deleteLoadBalancerGroupKnownAgent(String loadBalancerGroupName, String agentId) {
    final String requestUri = String.format(LOAD_BALANCER_DELETE_KNOWN_AGENT_FORMAT, getBaseUrl(), loadBalancerGroupName, agentId);
    delete(requestUri, "known agent", agentId, Collections.<String, String>emptyMap());
  }

  public BaragonGroup addTrafficSource(String loadBalancerGroupName, String source) {
    final String requestUri = String.format(LOAD_BALANCER_TRAFFIC_SOURCE_FORMAT, getBaseUrl(), loadBalancerGroupName);
    return post(requestUri, "add source", Optional.absent(), Optional.of(BaragonGroup.class), ImmutableMap.of("source", source)).get();
  }

  public Optional<BaragonGroup> removeTrafficSource(String loadBalancerGroupName, String source) {
    final String requestUri = String.format(LOAD_BALANCER_TRAFFIC_SOURCE_FORMAT, getBaseUrl(), loadBalancerGroupName);
    return delete(requestUri, "remove source", source, ImmutableMap.of("source", source), Optional.of(BaragonGroup.class));
  }

  public Optional<BaragonGroup> getGroupDetail(String loadBalancerGroupName) {
    final String requestUri = String.format(LOAD_BALANCER_GROUP_FORMAT, getBaseUrl(), loadBalancerGroupName);
    return getSingle(requestUri, "group detail", loadBalancerGroupName, BaragonGroup.class);
  }

  // BaragonService base path actions

  public Collection<String> getOccupiedBasePaths(String loadBalancerGroupName) {
    final String requestUri = String.format(LOAD_BALANCER_ALL_BASE_PATHS_FORMAT, getBaseUrl(), loadBalancerGroupName);
    return getCollection(requestUri, "occupied base paths", STRING_COLLECTION);
  }

  public Optional<BaragonService> getServiceForBasePath(String loadBalancerGroupName, String basePath) {
    final String requestUri = String.format(LOAD_BALANCER_BASE_PATH_FORMAT, getBaseUrl(), loadBalancerGroupName);
    return getSingle(requestUri, "service for base path", "", BaragonService.class, ImmutableMap.of("basePath", basePath));
  }

  public void clearBasePath(String loadBalancerGroupName, String basePath) {
    final String requestUri = String.format(LOAD_BALANCER_BASE_PATH_FORMAT, getBaseUrl(), loadBalancerGroupName);
    delete(requestUri, "base path", "", ImmutableMap.of("basePath", basePath));
  }


  // BaragonService request actions

  public Optional<BaragonResponse> getRequest(String requestId) {
    final String uri = String.format(REQUEST_ID_FORMAT, getBaseUrl(), requestId);
    return getSingle(uri, "request", requestId, BaragonResponse.class);
  }

  public Optional<BaragonResponse> enqueueRequest(BaragonRequest request) {
    final String uri = String.format(REQUEST_FORMAT, getBaseUrl());
    return post(uri, "request", Optional.of(request), Optional.of(BaragonResponse.class));
  }

  public Optional<BaragonResponse> cancelRequest(String requestId) {
    final String uri = String.format(REQUEST_ID_FORMAT, getBaseUrl(), requestId);
    return delete(uri, "request", requestId, Collections.<String, String>emptyMap(), Optional.of(BaragonResponse.class));
  }


  // BaragonService queued request actions

  public Collection<QueuedRequestId> getQueuedRequests() {
    final String uri = String.format(REQUEST_FORMAT, getBaseUrl());
    return getCollection(uri, "queued requests", QUEUED_REQUEST_COLLECTION);
  }
}
