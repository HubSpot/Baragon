package com.hubspot.baragon.service.edgecache.cloudflare;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.service.edgecache.EdgeCache;
import com.hubspot.baragon.service.edgecache.cloudflare.client.CloudflareClient;
import com.hubspot.baragon.service.edgecache.cloudflare.client.CloudflareClientException;
import com.hubspot.baragon.service.edgecache.cloudflare.client.CloudflareDnsRecord;
import com.hubspot.baragon.service.edgecache.cloudflare.client.CloudflareZone;

import io.dropwizard.setup.Environment;

public class CloudflareEdgeCache implements EdgeCache {
  private static final Logger LOG = LoggerFactory.getLogger(CloudflareEdgeCache.class);

  private static final String CACHE_TAG_FORMAT = "staticjsapp-%s-%s";

  private final CloudflareClient cf;
  private final Environment environment;

  @Inject
  public CloudflareEdgeCache(CloudflareClient cf, Environment environment) {
    this.cf = cf;
    this.environment = environment;
  }

  /**
   * Invalidation will eventually occur when the TTL expires, so it's not a showstopper if this fails.
   */
  @Override
  public boolean invalidateIfNecessary(BaragonRequest request) {
    if (request.getLoadBalancerService().getOptions().get("edgeCacheDNS") == null) {
      return false;
    }

    try {
      String edgeCacheDNS = ((String) request.getLoadBalancerService().getOptions().get("edgeCacheDNS"));
      Optional<CloudflareZone> matchingZone = getCloudflareZone(edgeCacheDNS);

      if (!matchingZone.isPresent()) {
        LOG.warn("`edgeCacheDNS` was defined on the request, but no matching Cloudflare Zone was found!");
        return false;
      }

      String zoneId = matchingZone.get().getId();
      Optional<CloudflareDnsRecord> matchingDnsRecord = getCloudflareDnsRecord(edgeCacheDNS, zoneId);

      if (!matchingDnsRecord.isPresent()) {
        LOG.warn("`edgeCacheDNS` was defined on the request, but no matching Cloudflare DNS Record was found!");
        return false;
      }

      if (!matchingDnsRecord.get().isProxied()) {
        LOG.warn("`edgeCacheDNS` was defined on the request, but {} is not a proxied DNS record!", edgeCacheDNS);
        return false;
      }

      return cf.purgeCache(
          zoneId,
          Collections.singletonList(                   // TODO this isn't the right way to get the env, is it vvv
              String.format(CACHE_TAG_FORMAT, request.getLoadBalancerService().getServiceId(), environment.getName())
          )
      );

    } catch (CloudflareClientException e) {
      LOG.error("Unable to invalidate Cloudflare cache for request {}", request, e);
      return false;
    }
  }

  private Optional<CloudflareDnsRecord> getCloudflareDnsRecord(String edgeCacheDNS, String zoneId) throws CloudflareClientException {
    List<CloudflareDnsRecord> dnsRecords = cf.listDnsRecords(zoneId);

    return dnsRecords.stream()
        .filter(r -> r.getZoneName().equals(edgeCacheDNS))
        .findAny();
  }

  private Optional<CloudflareZone> getCloudflareZone(String edgeCacheDNS) throws CloudflareClientException {
    List<CloudflareZone> zones = cf.getAllZones();
    String[] domainTokens = edgeCacheDNS.split("\\.");
    String baseDomain = String.format("%s.%s", domainTokens[domainTokens.length - 2], domainTokens[domainTokens.length - 1]);

    return zones.stream()
        .filter(z -> z.getName().equals(baseDomain))
        .findAny();
  }

}
