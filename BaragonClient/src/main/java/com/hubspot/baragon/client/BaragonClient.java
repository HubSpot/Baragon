package com.hubspot.baragon.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.exceptions.PendingServiceOccupiedException;
import com.hubspot.baragon.models.ServiceInfo;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class BaragonClient {
  private static final String SERVICE_FORMAT = "%s/service";
  private static final String SERVICE_PENDING_FORMAT = "%s/service/%s/pending";
  private static final String SERVICE_ACTIVE_FORMAT = "%s/service/%s/active";
  private static final String UPSTREAMS_FORMAT = "%s/upstreams/%s/%s";
  private static final String UPSTREAM_FORMAT = "%s/upstreams/%s/%s/%s";
  private static final String SERVICE_ACTIVATE_FORMAT = "%s/service/%s/activate";
  private static final String UPSTREAMS_UNHEALTHY_FORMAT = "%s/upstreams/%s/%s/unhealthy";
  private static final String UPSTREAMS_HEALTHY_FORMAT = "%s/upstreams/%s/%s/unhealthy";
  private static final String WEBHOOKS_FORMAT = "%s/webhooks";


  private final AsyncHttpClient asyncHttpClient;
  private final ObjectMapper objectMapper;
  private final String baseUrl;

  @Inject
  public BaragonClient(@Named(BaragonClientModule.ASYNC_HTTP_CLIENT_NAME) AsyncHttpClient asyncHttpClient, @Named(BaragonClientModule.OBJECT_MAPPER_NAME) ObjectMapper objectMapper, @Named(BaragonClientModule.BASE_URL_NAME) String baseUrl) {
    this.asyncHttpClient = asyncHttpClient;
    this.baseUrl = baseUrl;
    this.objectMapper = objectMapper;
  }

  private boolean isSuccess(Response response) {
    return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
  }

  private <T> Optional<T> tryDeserialize(Response response, Class<T> klass, String action) throws IOException {
    if (response.getStatusCode() == 404) {
      return Optional.absent();
    }

    if (!isSuccess(response)) {
      throw new RuntimeException(String.format("Failed to %s -- HTTP %s: %s", action, response.getStatusCode(), response.getResponseBody()));
    }

    return Optional.of(objectMapper.readValue(response.getResponseBodyAsStream(), klass));
  }

  private <T> Collection<T> tryDeserializeCollection(Response response, Class<T> klass, String action) throws IOException {
    if (response.getStatusCode() == 404) {
      return Collections.emptyList();
    }

    if (!isSuccess(response)) {
      throw new RuntimeException(String.format("Failed to %s -- HTTP %s: %s", action, response.getStatusCode(), response.getResponseBody()));
    }

    return objectMapper.readValue(response.getResponseBodyAsStream(), new TypeReference<Collection<T>>() { });
  }

  public void addPendingService(ServiceInfo serviceInfo) throws PendingServiceOccupiedException {
    try {
      Response response = asyncHttpClient.preparePost(String.format(SERVICE_FORMAT, baseUrl))
          .setBody(objectMapper.writeValueAsBytes(serviceInfo))
          .addHeader("content-type", "application/json")
          .execute().get();

      if (response.getStatusCode() == 409) {
        throw new PendingServiceOccupiedException(objectMapper.readValue(response.getResponseBodyAsStream(), ServiceInfo.class));
      }

      if (!isSuccess(response)) {
        throw new RuntimeException(String.format("Failed to add pending service -- HTTP %s: %s", response.getStatusCode(), response.getResponseBody()));
      }
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public Optional<ServiceInfo> removePendingService(String serviceName) {
    try {
      Response response = asyncHttpClient.prepareDelete(String.format(SERVICE_PENDING_FORMAT, baseUrl, serviceName))
          .execute().get();

      return tryDeserialize(response, ServiceInfo.class, "remove pending service");
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public Optional<ServiceInfo> getPendingService(String serviceName) {
    try {
      Response response = asyncHttpClient.prepareGet(String.format(SERVICE_PENDING_FORMAT, baseUrl, serviceName))
          .execute().get();

      return tryDeserialize(response, ServiceInfo.class, "get pending service");
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public void addUpstream(String serviceName, String serviceId, String upstream) {
    try {
      Response response = asyncHttpClient.preparePost(String.format(UPSTREAMS_FORMAT, baseUrl, serviceName, serviceId))
          .addQueryParameter("upstream", upstream)
          .execute().get();

      if (!isSuccess(response)) {
        throw new RuntimeException(String.format("Failed to add upstream -- HTTP %s: %s", response.getStatusCode(), response.getResponseBody()));
      }
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public void removeUpstream(String serviceName, String serviceId, String upstream) {
    try {
      Response response = asyncHttpClient.prepareDelete(String.format(UPSTREAM_FORMAT, baseUrl, serviceName, serviceId, upstream))
          .execute().get();

      if (!isSuccess(response)) {
        throw new RuntimeException(String.format("Failed to remove upstream -- HTTP %s: %s", response.getStatusCode(), response.getResponseBody()));
      }
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public Collection<String> getUnhealthyUpstreams(String serviceName, String serviceId) {
    try {
      Response response = asyncHttpClient.prepareGet(String.format(UPSTREAMS_UNHEALTHY_FORMAT, baseUrl, serviceName, serviceId))
          .execute().get();

      return tryDeserializeCollection(response, String.class, "get unhealthy upstreams");
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public Collection<String> getHealthyUpstreams(String serviceName, String serviceId) {
    try {
      Response response = asyncHttpClient.prepareGet(String.format(UPSTREAMS_HEALTHY_FORMAT, baseUrl, serviceName, serviceId))
          .execute().get();

      return tryDeserializeCollection(response, String.class, "get healthy upstreams");
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public Optional<ServiceInfo> getActiveService(String serviceName) {
    try {
      Response response = asyncHttpClient.prepareGet(String.format(SERVICE_ACTIVE_FORMAT, baseUrl, serviceName))
          .execute().get();

      return tryDeserialize(response, ServiceInfo.class, "get pending service");
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public Optional<ServiceInfo> activateService(String serviceName) {
    try {
      Response response = asyncHttpClient.preparePost(String.format(SERVICE_ACTIVATE_FORMAT, baseUrl, serviceName))
          .execute().get();

      return tryDeserialize(response, ServiceInfo.class, "activate service");
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public Collection<String> getWebhooks() {
    try {
      Response response = asyncHttpClient.prepareGet(String.format(WEBHOOKS_FORMAT, baseUrl))
          .execute().get();

      return tryDeserializeCollection(response, String.class, "get webhooks");
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public void addWebhook(String webhook) {
    try {
      Response response = asyncHttpClient.preparePost(String.format(WEBHOOKS_FORMAT, baseUrl))
          .addQueryParameter("url", webhook)
          .execute().get();

      if (!isSuccess(response)) {
        throw new RuntimeException(String.format("Failed to add webhook -- HTTP %s: %s", response.getStatusCode(), response.getResponseBody()));
      }
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }

  public void removeWebhook(String webhook) {
    try {
      Response response = asyncHttpClient.prepareDelete(String.format(WEBHOOKS_FORMAT, baseUrl))
          .addQueryParameter("url", webhook)
          .execute().get();

      if (!isSuccess(response)) {
        throw new RuntimeException(String.format("Failed to remove webhook -- HTTP %s: %s", response.getStatusCode(), response.getResponseBody()));
      }
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }
}
