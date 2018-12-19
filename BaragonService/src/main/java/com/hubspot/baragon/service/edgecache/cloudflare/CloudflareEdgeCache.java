package com.hubspot.baragon.service.edgecache.cloudflare;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.service.config.EdgeCacheConfiguration;
import com.hubspot.baragon.service.edgecache.EdgeCache;
import com.hubspot.baragon.service.edgecache.cloudflare.client.CloudflareClient;
import com.hubspot.baragon.service.edgecache.cloudflare.client.CloudflareClientException;
import com.hubspot.baragon.service.edgecache.cloudflare.client.models.CloudflareDnsRecord;
import com.hubspot.baragon.service.edgecache.cloudflare.client.models.CloudflareZone;

/**
 * An implementation of a proxying edge cache backed by Cloudflare.
 *
 * Config example:
 * ...
 * edgeCache:
 *   enabled: true
 *   edgeCacheClass: CLOUDFLARE
 *   integrationSettings:
 *     apiBase: https://api.cloudflare.com/client/v4/
 *     apiEmail: email@host.net
 *     apiKey: some-key
 *     cacheTagFormat: cache-tag-for-%-service
 * ...
 */
public class CloudflareEdgeCache implements EdgeCache {
  private static final Logger LOG = LoggerFactory.getLogger(CloudflareEdgeCache.class);

  private final CloudflareClient cf;
  private final EdgeCacheConfiguration edgeCacheConfiguration;

  @Inject
  public CloudflareEdgeCache(CloudflareClient cf, EdgeCacheConfiguration edgeCacheConfiguration) {
    this.cf = cf;
    this.edgeCacheConfiguration = edgeCacheConfiguration;
  }

  /**
   * Invalidation will eventually occur when the TTL expires, so it's not a showstopper if this fails.
   */
  @Override
  public boolean invalidateIfNecessary(BaragonRequest request) {
    if (request.getLoadBalancerService().getEdgeCacheDomains().isEmpty()) {
      return false;
    }

    try {
      boolean allSucceeded = true;
      for (String edgeCacheDNS : request.getLoadBalancerService().getEdgeCacheDomains()) {
        List<CloudflareZone> matchingZones = getCloudflareZone(edgeCacheDNS);

        if (matchingZones.isEmpty()) {
          LOG.warn("`edgeCacheDNS` was defined on the request, but no matching Cloudflare Zone was found!");
          return false;
        }

        for (CloudflareZone matchingZone : matchingZones) {
          String zoneId = matchingZone.getId();
          Optional<CloudflareDnsRecord> matchingDnsRecord = getCloudflareDnsRecord(edgeCacheDNS, zoneId);

          if (!matchingDnsRecord.isPresent()) {
            LOG.warn("`edgeCacheDNS` was defined on the request, but no matching Cloudflare DNS Record was found!");
            return false;
          }

          if (!matchingDnsRecord.get().isProxied()) {
            LOG.warn("`edgeCacheDNS` was defined on the request, but {} is not a proxied DNS record!", edgeCacheDNS);
            return false;
          }

          String cacheTag = String.format(
              edgeCacheConfiguration.getIntegrationSettings().get("cacheTagFormat"),
              request.getLoadBalancerService().getServiceId()
          );

          LOG.debug("Sending cache purge request against {} for {} to Cloudflare...", matchingDnsRecord.get().getName(), cacheTag);

          allSucceeded = cf.purgeEdgeCache(zoneId, Collections.singletonList(cacheTag)) && allSucceeded;
        }
      }
      return allSucceeded;

    } catch (CloudflareClientException e) {
      LOG.error("Unable to invalidate Cloudflare cache for request {}", request, e);
      return false;
    }
  }

  private Optional<CloudflareDnsRecord> getCloudflareDnsRecord(String edgeCacheDNS, String zoneId) throws CloudflareClientException {
    return Optional.ofNullable(cf.getDnsRecord(zoneId, edgeCacheDNS));
  }

  private List<CloudflareZone> getCloudflareZone(String edgeCacheDNS) throws CloudflareClientException {
    String baseDomain = getBaseDomain(edgeCacheDNS);
    return cf.getZone(baseDomain);
  }

  private String getBaseDomain(String dns) {
    String[] domainTokens = dns.split("\\.");
    return String.format("%s.%s", domainTokens[domainTokens.length - 2], domainTokens[domainTokens.length - 1]);
  }

}
