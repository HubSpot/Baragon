package com.hubspot.baragon.service.edgecache.cloudflare.client;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.horizon.HttpRequest.Method;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

public class CloudflareClient {

  private final AsyncHttpClient httpClient;
  private final String apiEmail;
  private final String apiKey;
  private final String apiBase;

  @Inject
  public CloudflareClient(@Named(BaragonServiceModule.BARAGON_SERVICE_HTTP_CLIENT) AsyncHttpClient httpClient,
                          String apiEmail,
                          String apiKey,
                          String apiBase) {
    this.httpClient = httpClient;
    this.apiEmail = apiEmail;
    this.apiKey = apiKey;
    this.apiBase = apiBase;
  }

  boolean purgeCache(String zoneId, List<String> cacheTags) throws CloudflareClientException {
    CloudflarePurgeRequest purgeRequest = new CloudflarePurgeRequest(Collections.emptyList(), cacheTags);
    Response response = requestWith(Method.DELETE, String.format("zones/%s/purge_cache", zoneId), purgeRequest);
    return isSuccess(response);
  }

  private Response requestWith(Method method, String path, Object body) throws CloudflareClientException {
    return request(method, path, Optional.of(body), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
  }

  private Response request(Method method, String path, Optional<Object> body, Optional<Integer> page, Optional<Integer> perPage, Optional<String> order, Optional<String> direction) throws CloudflareClientException {
    BoundRequestBuilder builder;

    switch (method) {
      case DELETE:
        builder = httpClient.prepareDelete(apiBase + path);
        break;
      case GET:
      default:
        builder = httpClient.prepareGet(apiBase + path);
    }

    builder
        .addHeader("X-Auth-Email", apiEmail)
        .addHeader("X-Auth-Key", apiKey);

    body.ifPresent(b -> builder.setBody(b.toString()));

    page.ifPresent(p -> builder.addQueryParameter("page", page.get().toString()));
    perPage.ifPresent(p -> builder.addQueryParameter("per_page", perPage.get().toString()));
    order.ifPresent(o -> builder.addQueryParameter("order", order.get()));
    direction.ifPresent(d -> builder.addQueryParameter("direction", direction.get()));

    try {
      return builder.execute().get();
    } catch (Exception e) {
      throw new CloudflareClientException("Unexpected error during Cloudflare API call", e);
    }
  }

  private boolean isSuccess(Response response) {
    return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
  }

}
