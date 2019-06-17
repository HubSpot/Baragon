package com.hubspot.baragon.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.HostAndPort;
import com.google.common.net.InetAddresses;
import com.google.inject.Singleton;

@Singleton
public class UpstreamResolver {
  private final Cache<String, String> resolveCache;

  public UpstreamResolver(long maxSize, long expireAfterDays) {
    this.resolveCache = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterAccess(expireAfterDays, TimeUnit.DAYS)
        .build();
  }

  public Optional<String> resolveUpstreamDNS(String address) {
    String host;
    Optional<Integer> port = Optional.absent();

    if (address.contains(":")) {
      HostAndPort hostAndPort = HostAndPort.fromString(address);
      host = hostAndPort.getHost();
      port = Optional.of(hostAndPort.getPort());
    } else {
      host = address;
    }

    try {
      InetAddresses.forString(host);
      return Optional.of(address); // `address` is already an IP
    } catch (IllegalArgumentException e) {
      // `address` is not an IP, continue and try to resolve it
    }

    try {
      String cached = resolveCache.getIfPresent(host);
      String ip;
      if (cached != null) {
        ip = cached;
      } else {
        ip = InetAddress.getByName(host).getHostAddress();
        resolveCache.put(host, ip);
      }

      if (port.isPresent()) {
        return Optional.of(String.format("%s:%d", ip, port.get()));
      } else {
        return Optional.of(String.format("%s", ip));
      }
    } catch (UnknownHostException uhe) {
      // Don't let this exception block rendering of the template, the lb config check will still fail if the host is truly unknown
      return Optional.absent();
    }
  }
}
