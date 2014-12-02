package com.hubspot.baragon.client;

import javax.inject.Provider;
import java.util.Random;
import java.util.List;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;

import com.hubspot.baragon.models.BaragonAgentStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpRequest;
import com.hubspot.horizon.HttpRequest.Method;
import com.hubspot.horizon.HttpResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.hubspot.baragon.models.*;

public class BaragonClient {

  private static final Logger LOG = LoggerFactory.getLogger(BaragonClient.class);

  private static final String AGENT_STATUS_FORMAT = "http://%s/%s/status";
  private static final String AGENT_REQUEST_FORMAT = "http://%s/%s/request/%s";

  private static final String WORKERS_FORMAT = "http://%s/%s/workers";

  private static final String LOAD_BALANCER_FORMAT = "http://%s/%s/load-balancer";
  private static final String LOAD_BALANCER_BASE_PATH_FORMAT = LOAD_BALANCER_FORMAT + "/%s/base-path";
  private static final String LOAD_BALANCER_ALL_BASE_PATHS_FORMAT = LOAD_BALANCER_BASE_PATH_FORMAT + "/all";
  private static final String LOAD_BALANCER_AGENTS_FORMAT = LOAD_BALANCER_FORMAT + "/%s/agents";
  private static final String LOAD_BALANCER_HOSTS_FORMAT = LOAD_BALANCER_FORMAT + "/%s/hosts";

  private static final String REQUEST_FORMAT = "http://%s/%s/request";
  private static final String REQUEST_ID_FORMAT = REQUEST_FORMAT + "/%s";

  private static final String AUTH_FORMAT = "http://%s/%s/auth/keys";
  private static final String AUTH_DELETE_KEY_FORMAT = AUTH_FORMAT + "/%s";

  private static final String STATE_FORMAT = "http://%s/%s/state";
  private static final String STATE_SERVICE_ID_FORMAT = STATE_FORMAT + "/%s";
  private static final String STATE_UPSTREAM_FORMAT = STATE_SERVICE_ID_FORMAT + "/%s";

  private static final String STATUS_FORMAT = "http://%s/%s/status";

  private static final TypeReference<Collection<String>> WORKERS_COLLECTION = new TypeReference<Collection<String>>() {};
  private static final TypeReference<Collection<String>> LOAD_BALANCER_COLLECTION = new TypeReference<Collection<String>>() {};
  private static final TypeReference<Collection<String>> LOAD_BALANCER_HOSTS_COLLECTION = new TypeReference<Collection<String>>() {};
  private static final TypeReference<Collection<String>> BASE_PATHS_COLLECTION = new TypeReference<Collection<String>>() {};
  private static final TypeReference<Collection<BaragonAgentMetadata>> BARAGON_AGENTS_COLLECTION = new TypeReference<Collection<BaragonAgentMetadata>>() {};
  private static final TypeReference<Collection<QueuedRequestId>> QUEUED_REQUEST_COLLECTION = new TypeReference<Collection<QueuedRequestId>>() {};
  private static final TypeReference<Collection<BaragonAuthKey>> BARAGON_AUTH_KEY_COLLECTION = new TypeReference<Collection<BaragonAuthKey>>() {};

  private final Random random;
  private final Provider<List<String>> hostsProvider;
  private final String contextPath;

  private final HttpClient httpClient;

  public BaragonClient(String contextPath, HttpClient httpClient, List<String> hosts) {
    this(contextPath, httpClient, ProviderUtils.<List<String>>of(ImmutableList.copyOf(hosts)));
  }

  public BaragonClient(String contextPath, HttpClient httpClient, Provider<List<String>> hostsProvider) {
    this.httpClient = httpClient;
    this.contextPath = contextPath;
    this.hostsProvider = hostsProvider;
    this.random = new Random();
  }

  private String getHost() {
    final List<String> hosts = hostsProvider.get();
    return hosts.get(random.nextInt(hosts.size()));
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
    checkNotNull(id, String.format("Provide a %s id", type));
    LOG.info("Getting {} {} from {}", type, id, uri);
    final long start = System.currentTimeMillis();
    HttpResponse response = httpClient.execute(HttpRequest.newBuilder().setUrl(uri).build());

    if (response.getStatusCode() == 404) {
      return Optional.absent();
    }

    checkResponse(type, response);
    LOG.info("Got {} {} in {}ms", type, id, System.currentTimeMillis() - start);
    return Optional.fromNullable(response.getAs(clazz));
  }

  private <T> Collection<T> getCollection(String uri, String type, TypeReference<Collection<T>> typeReference) {
    LOG.info("Getting all {} from {}", type, uri);
    final long start = System.currentTimeMillis();
    HttpResponse response = httpClient.execute(HttpRequest.newBuilder().setUrl(uri).build());

    if (response.getStatusCode() == 404) {
      return ImmutableList.of();
    }

    checkResponse(type, response);
    LOG.info("Got {} in {}ms", type, System.currentTimeMillis() - start);
    return response.getAs(typeReference);
  }

  private <T> void delete(String uri, String type, String id) {
    delete(uri, type, id, Optional.<Class<T>> absent());
  }

  private <T> Optional<T> delete(String uri, String type, String id, Optional<Class<T>> clazz) {
    LOG.info("Deleting {} {} from {}", type, id, uri);
    final long start = System.currentTimeMillis();
    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(uri).setMethod(Method.DELETE);
    HttpResponse response = httpClient.execute(request.build());

    if (response.getStatusCode() == 404) {
      LOG.info("{} ({}) was not found", type, id);
      return Optional.absent();
    }

    checkResponse(type, response);
    LOG.info("Deleted {} ({}) from Baragon in %sms", type, id, System.currentTimeMillis() - start);

    if (clazz.isPresent()) {
      return Optional.of(response.getAs(clazz.get()));
    }

    return Optional.absent();
  }

  private <T> Optional<T> post(String uri, String type, Optional<?> body, Optional<Class<T>> clazz) {
    try {
      HttpResponse response = post(uri, type, body);

      if (clazz.isPresent()) {
        return Optional.of(response.getAs(clazz.get()));
      }
    } catch (Exception e) {
      LOG.warn("Http post failed", e);
    }

    return Optional.absent();
  }

  private HttpResponse post(String uri, String type, Optional<?> body) {
    LOG.info("Posting {} to {}", type, uri);
    final long start = System.currentTimeMillis();
    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(uri).setMethod(Method.POST);

    if (body.isPresent()) {
      request.setBody(body.get());
    }

    HttpResponse response = httpClient.execute(request.build());
    checkResponse(type, response);
    LOG.info("Successfully posted {} in {}ms", type, System.currentTimeMillis() - start);
    return response;
  }

  // BaragonAgent actions

  public BaragonAgentStatus getAgentStatus() {
    final String uri = String.format(AGENT_STATUS_FORMAT, getHost(), contextPath);
    LOG.info("Fetching state from {}", uri);
    final long start = System.currentTimeMillis();
    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(uri);
    HttpResponse response = httpClient.execute(request.build());
    checkResponse("state", response);
    LOG.info("Got state in {}ms", System.currentTimeMillis() - start);
    return response.getAs(BaragonAgentStatus.class);
  }

  public Optional<BaragonRequest> deleteAgentRequest(String requestId) {
    final String requestUri = String.format(AGENT_REQUEST_FORMAT, getHost(), contextPath, requestId);
    return delete(requestUri, "request", requestId, Optional.of(BaragonRequest.class));
  }

  public void createAgentRequest(BaragonRequest request) {
    checkNotNull(request.getLoadBalancerRequestId(), "A Baragon Request must have an id");
    final String requestUri = String.format(AGENT_REQUEST_FORMAT, getHost(), contextPath, request.getLoadBalancerRequestId());
    post(requestUri, String.format("request %s", request.getLoadBalancerRequestId()), Optional.of(request));
  }


  // BaragonService status

  public Optional<BaragonServiceStatus> getStatus() {
    final String uri = String.format(STATUS_FORMAT, getHost(), contextPath);
    return getSingle(uri, "status", "", BaragonServiceStatus.class);
  }

  // BaragonService service states

  public Optional<BaragonServiceState> getState() {
    final String uri = String.format(STATE_FORMAT, getHost(), contextPath);
    return getSingle(uri, "state", "", BaragonServiceState.class);
  }

  public Optional<BaragonServiceState> getServiceState(String serviceId) {
    final String uri = String.format(STATE_SERVICE_ID_FORMAT, getHost(), contextPath, serviceId);
    return getSingle(uri, "service state", serviceId, BaragonServiceState.class);
  }

  public Optional<UpstreamInfo> getUpstreamState(String serviceId, String upstream) {
    final String uri = String.format(STATE_UPSTREAM_FORMAT, getHost(), contextPath, serviceId, upstream);
    return getSingle(uri, "upstream info", upstream, UpstreamInfo.class);
  }

  public void deleteService(String serviceId) {
    final String uri = String.format(STATE_SERVICE_ID_FORMAT, getHost(), contextPath, serviceId);
    delete(uri, "service state", serviceId);
  }

  // BaragonService Workers

  public Collection<String> getWorkers() {
    final String requestUri = String.format(WORKERS_FORMAT, getHost(), contextPath);
    return getCollection(requestUri, "workers", WORKERS_COLLECTION);
  }

  // BaragonService load balancer actions

  public Collection<String> getLoadBalancers() {
    final String requestUri = String.format(LOAD_BALANCER_FORMAT, getHost(), contextPath);
    return getCollection(requestUri, "load balancers", LOAD_BALANCER_COLLECTION);
  }

  public Collection<String> getHosts(String clusterName) {
    final String requestUri = String.format(LOAD_BALANCER_HOSTS_FORMAT, getHost(), contextPath, clusterName);
    return getCollection(requestUri, "load balancer hosts", LOAD_BALANCER_HOSTS_COLLECTION);
  }

  public Collection<String> getAllBasePaths(String clusterName) {
    final String requestUri = String.format(LOAD_BALANCER_ALL_BASE_PATHS_FORMAT, getHost(), contextPath, clusterName);
    return getCollection(requestUri, "base paths", BASE_PATHS_COLLECTION);
  }

  public BaragonService getBasePath(String clusterName, String basePath) {
    final String requestUri = String.format(LOAD_BALANCER_BASE_PATH_FORMAT, getHost(), contextPath, clusterName);
    final long start = System.currentTimeMillis();
    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(requestUri);
    request.addQueryParam("basePath", basePath);
    HttpResponse response = httpClient.execute(request.build());
    checkResponse("base path", response);
    LOG.info("Got base path in {}ms", System.currentTimeMillis() - start);
    return response.getAs(BaragonService.class);
  }

  public void deleteBasePath(String clusterName, String basePath) {
    final String requestUri = String.format(LOAD_BALANCER_BASE_PATH_FORMAT, getHost(), contextPath, clusterName);
    final long start = System.currentTimeMillis();
    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(requestUri).setMethod(Method.DELETE);
    request.addQueryParam("basePath", basePath);
    HttpResponse response = httpClient.execute(request.build());
    if (response.getStatusCode() == 404) {
      LOG.info("{} ({}) was not found", clusterName, basePath);
    }

    checkResponse("base paths", response);
    LOG.info("Deleted {} ({}) from Baragon base paths in %sms", clusterName, basePath, System.currentTimeMillis() - start);
  }

  public Collection<BaragonAgentMetadata> getAgents(String clusterName) {
    final String requestUri = String.format(LOAD_BALANCER_AGENTS_FORMAT, getHost(), contextPath, clusterName);
    return getCollection(requestUri, "agents", BARAGON_AGENTS_COLLECTION);
  }

  // BaragonService request actions


  public Optional<BaragonResponse> getRequest(String requestId) {
    final String uri = String.format(REQUEST_ID_FORMAT, getHost(), contextPath, requestId);
    return getSingle(uri, "request", requestId, BaragonResponse.class);
  }

  public Collection<QueuedRequestId> getQueuedRequests() {
    final String uri = String.format(REQUEST_FORMAT, getHost(), contextPath);
    return getCollection(uri, "request", QUEUED_REQUEST_COLLECTION);
  }

  public Optional<BaragonResponse> enqueueRequest(BaragonRequest request) {
    final String uri = String.format(REQUEST_FORMAT, getHost(), contextPath);
    return post(uri, "request", Optional.of(request), Optional.of(BaragonResponse.class));
  }

  public Optional<BaragonResponse> cancelRequest(String requestId) {
    final String uri = String.format(REQUEST_ID_FORMAT, getHost(), contextPath, requestId);
    return delete(uri, "request", requestId, Optional.of(BaragonResponse.class));
  }

  // BaragonService auth key actions

  public Collection<BaragonAuthKey> getKeys() {
    final String uri = String.format(AUTH_FORMAT, getHost(), contextPath);
    return getCollection(uri, "auth keys", BARAGON_AUTH_KEY_COLLECTION);
  }

  public Optional<BaragonAuthKey> expirekey(String authkey) {
    final String uri = String.format(AUTH_DELETE_KEY_FORMAT, getHost(), contextPath, authkey);
    final long start = System.currentTimeMillis();
    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(uri).setMethod(Method.DELETE);
    request.addQueryParam("authkey", authkey);
    HttpResponse response = httpClient.execute(request.build());
    if (response.getStatusCode() == 404) {
      LOG.info("{} ({}) was not found", "authkey", authkey);
    }

    checkResponse("base paths", response);
    LOG.info("Deleted {} ({}) from Baragon base paths in %sms", "authkey", authkey, System.currentTimeMillis() - start);
    return Optional.of(response.getAs(BaragonAuthKey.class));
  }

  public void addKey(BaragonAuthKey authkey, String queryAuthkey) {
    final String uri = String.format(AUTH_FORMAT, getHost(), contextPath);
    LOG.info("Posting {} to {}", "authkey", uri);
    final long start = System.currentTimeMillis();
    HttpRequest.Builder request = HttpRequest.newBuilder().setUrl(uri).setMethod(Method.POST);
    request.setBody(authkey);
    request.addQueryParam("authkey", queryAuthkey);
    HttpResponse response = httpClient.execute(request.build());
    checkResponse("authkey", response);
    LOG.info("Successfully posted {} in {}ms", "authkey", System.currentTimeMillis() - start);
  }

}
