package com.hubspot.baragon.client;

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
import java.util.concurrent.ExecutionException;

public class BaragonClient {
  private static final String SERVICE_FORMAT = "%s/service";
  private static final String SERVICE_PENDING_FORMAT = "%s/service/%s/pending";
  private static final String UPSTREAMS_FORMAT = "%s/upstreams/%s/%s";
  private static final String UPSTREAM_FORMAT = "%s/upstreams/%s/%s/%s";
  private static final String SERVICE_ACTIVATE_FORMAT = "%s/service/%s/activate";


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

      if (response.getStatusCode() == 404) {
        return Optional.absent();
      }

      if (!isSuccess(response)) {
        throw new RuntimeException(String.format("Failed to remove pending service -- HTTP %s: %s", response.getStatusCode(), response.getResponseBody()));
      }

      return Optional.of(objectMapper.readValue(response.getResponseBodyAsStream(), ServiceInfo.class));

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

  public Optional<ServiceInfo> activateService(String serviceName) {
    try {
      Response response = asyncHttpClient.preparePost(String.format(SERVICE_ACTIVATE_FORMAT, baseUrl, serviceName))
          .execute().get();

      if (response.getStatusCode() == 404) {
        return Optional.absent();
      }

      if (!isSuccess(response)) {
        throw new RuntimeException(String.format("Failed to activate service -- HTTP %s: %s", response.getStatusCode(), response.getResponseBody()));
      }

      return Optional.of(objectMapper.readValue(response.getResponseBodyAsStream(), ServiceInfo.class));
    } catch (IOException | InterruptedException | ExecutionException e) {
      throw Throwables.propagate(e);
    }
  }
}
