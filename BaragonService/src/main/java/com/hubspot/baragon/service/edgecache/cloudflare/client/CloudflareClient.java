package com.hubspot.baragon.service.edgecache.cloudflare.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.config.EdgeCacheConfiguration;
import com.hubspot.baragon.service.edgecache.cloudflare.client.models.CloudflareDnsRecord;
import com.hubspot.baragon.service.edgecache.cloudflare.client.models.CloudflareListDnsRecordsResponse;
import com.hubspot.baragon.service.edgecache.cloudflare.client.models.CloudflareListZonesResponse;
import com.hubspot.baragon.service.edgecache.cloudflare.client.models.CloudflarePurgeCacheResponse;
import com.hubspot.baragon.service.edgecache.cloudflare.client.models.CloudflarePurgeRequest;
import com.hubspot.baragon.service.edgecache.cloudflare.client.models.CloudflareResponse;
import com.hubspot.baragon.service.edgecache.cloudflare.client.models.CloudflareResultInfo;
import com.hubspot.baragon.service.edgecache.cloudflare.client.models.CloudflareZone;
import com.hubspot.horizon.HttpRequest.Method;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.Response;

@Singleton
public class CloudflareClient {
  private static final Logger LOG = LoggerFactory.getLogger(CloudflareClient.class);

  private static final int MAX_ZONES_PER_PAGE = 50;
  private static final int MAX_DNS_RECORDS_PER_PAGE = 100;

  private final AsyncHttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final String apiBase;
  private final String apiEmail;
  private final String apiKey;

  //                                  <ZoneId, CloudflareZone>
  private final Supplier<Map<String, List<CloudflareZone>>> zoneCache;
  //                        <ZoneId,   <DnsName, CloudflareDnsRecord>>
  private final LoadingCache<String, Map<String, CloudflareDnsRecord>> dnsRecordCache;

  @Inject
  public CloudflareClient(EdgeCacheConfiguration edgeCacheConfiguration,
                          @Named(BaragonServiceModule.BARAGON_SERVICE_HTTP_CLIENT) AsyncHttpClient httpClient,
                          ObjectMapper objectMapper) {
    this.httpClient = httpClient;
    this.objectMapper = objectMapper;
    Map<String, String> integrationSettings = edgeCacheConfiguration.getIntegrationSettings();
    this.apiBase = integrationSettings.get("apiBase");
    this.apiEmail = integrationSettings.get("apiEmail");
    this.apiKey = integrationSettings.get("apiKey");

    this.zoneCache = Suppliers.memoizeWithExpiration(() -> {
      try {
        return retrieveAllZones().stream().collect(Collectors.groupingBy(CloudflareZone::getName));
      } catch (CloudflareClientException e) {
        LOG.error("Unable to refresh Cloudflare zone cache", e);
        return new ConcurrentHashMap<>();
      }
    }, 10, TimeUnit.MINUTES);

    this.dnsRecordCache = CacheBuilder.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build(new CacheLoader<String, Map<String, CloudflareDnsRecord>>() {
          @Override
          public Map<String, CloudflareDnsRecord> load(String zoneId) throws Exception {
            return retrieveDnsRecords(zoneId).stream().collect(Collectors.toConcurrentMap(
                CloudflareDnsRecord::getName,
                Function.identity(),
                (name1, name2) -> {
                  LOG.debug("Duplicate name found", name1);
                  return name1;
                }
            ));
          }
        });
  }

  public boolean purgeEdgeCache(String zoneId, List<String> cacheTags) throws CloudflareClientException {
    CloudflarePurgeRequest purgeRequest = new CloudflarePurgeRequest(Collections.emptyList(), cacheTags);
    Response response = requestWith(Method.DELETE, String.format("zones/%s/purge_cache", zoneId), purgeRequest);

    try {
      LOG.trace("Sending purge request to Cloudflare: {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(purgeRequest));
    } catch (JsonProcessingException e) {
      // no-op
    }

    boolean success = isSuccess(response);

    try {
      if (!success && response.getResponseBody() != null) {
        CloudflarePurgeCacheResponse parsedResponse = objectMapper.readValue(response.getResponseBody(), CloudflarePurgeCacheResponse.class);

        LOG.error("Failed to purge Cloudflare edge cache for cache tag(s) {}. Errors were: {}", cacheTags, parsedResponse.getErrors());
      }
    } catch (IOException e) {
      LOG.warn("Failed to read response from Cloudflare while trying to inspect unsuccessful cache purge response.");
    }

    return success;
  }

  private Response requestWith(Method method, String path, Object body) throws CloudflareClientException {
    return request(method, path, Optional.of(body), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent());
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

    if (body.isPresent()) {
      try {
        builder.setBody(objectMapper.writeValueAsString(body.get()));
      } catch (JsonProcessingException e) {
        throw new CloudflareClientException("Unable to serialize body while preparing to send API request", e);
      }
    }

    page.asSet().forEach(p -> builder.addQueryParameter("page", page.get().toString()));
    perPage.asSet().forEach(p -> builder.addQueryParameter("per_page", perPage.get().toString()));
    order.asSet().forEach(o -> builder.addQueryParameter("order", order.get()));
    direction.asSet().forEach(d -> builder.addQueryParameter("direction", direction.get()));

    try {
      return builder.execute().get();
    } catch (Exception e) {
      throw new CloudflareClientException("Unexpected error during Cloudflare API call", e);
    }
  }

  private boolean isSuccess(Response response) {
    return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
  }

  public List<CloudflareZone> getZone(String name) throws CloudflareClientException {
    return zoneCache.get().get(name);
  }

  public List<CloudflareZone> retrieveAllZones() throws CloudflareClientException {
    CloudflareListZonesResponse cloudflareResponse = listZonesPaged(1);

    List<CloudflareZone> zones = new ArrayList<>();
    zones.addAll(cloudflareResponse.getResult());

    CloudflareResultInfo paginationInfo = cloudflareResponse.getResultInfo();
    for (int i = 2; i <= paginationInfo.getTotalPages(); i++) {
      CloudflareListZonesResponse cloudflarePageResponse = listZonesPaged(i);
      zones.addAll(cloudflarePageResponse.getResult());
    }
    return zones;
  }

  private CloudflareListZonesResponse listZonesPaged(Integer page) throws CloudflareClientException {
    Response response = pagedRequest(Method.GET, "zones", page, MAX_ZONES_PER_PAGE);
    if (!isSuccess(response)) {
      try {
        CloudflareResponse parsedResponse = objectMapper.readValue(response.getResponseBody(), CloudflareListZonesResponse.class);
        throw new CloudflareClientException("Failed to get zones, " + parsedResponse);
      } catch (IOException e) {
        throw new CloudflareClientException("Failed to get zones; unable to parse error response");
      }
    }

    try {
      return objectMapper.readValue(response.getResponseBody(), CloudflareListZonesResponse.class);
    } catch (IOException e) {
      throw new CloudflareClientException("Unable to parse Cloudflare List Zones response", e);
    }
  }
  public CloudflareDnsRecord getDnsRecord(String zoneId, String name) throws CloudflareClientException {
    try {
      return dnsRecordCache.get(zoneId).get(name);
    } catch (ExecutionException e) {
      throw new CloudflareClientException(e.getMessage(), e.getCause());
    }
  }

  public Set<CloudflareDnsRecord> retrieveDnsRecords(String zoneId) throws CloudflareClientException {
    CloudflareListDnsRecordsResponse cloudflareResponse = listDnsRecordsPaged(zoneId, 1);

    Set<CloudflareDnsRecord> dnsRecords = cloudflareResponse.getResult();

    CloudflareResultInfo paginationInfo = cloudflareResponse.getResultInfo();
    for (int i = 2; i <= paginationInfo.getTotalPages(); i++) {
      CloudflareListDnsRecordsResponse cloudflarePageResponse = listDnsRecordsPaged(zoneId, i);
      dnsRecords.addAll(cloudflarePageResponse.getResult());
    }
    return dnsRecords;
  }

  private CloudflareListDnsRecordsResponse listDnsRecordsPaged(String zoneId, Integer page) throws CloudflareClientException {
    Response response = pagedRequest(Method.GET, String.format("zones/%s/dns_records", zoneId), page, MAX_DNS_RECORDS_PER_PAGE);
    if (!isSuccess(response)) {
      try {
        CloudflareResponse parsedResponse = objectMapper.readValue(response.getResponseBody(), CloudflareListDnsRecordsResponse.class);
        throw new CloudflareClientException("Failed to get DNS records, " + parsedResponse);
      } catch (IOException e) {
        throw new CloudflareClientException("Failed to get DNS records; unable to parse error response");
      }
    }

    try {
      return objectMapper.readValue(response.getResponseBody(), CloudflareListDnsRecordsResponse.class);
    } catch (IOException e) {
      throw new CloudflareClientException("Unable to parse Cloudflare List DNS Records response", e);
    }
  }

  private Response pagedRequest(Method method, String path, Integer page, Integer perPage) throws CloudflareClientException {
    return request(method, path, Optional.absent(), Optional.of(page), Optional.of(perPage), Optional.absent(), Optional.absent());
  }
}
