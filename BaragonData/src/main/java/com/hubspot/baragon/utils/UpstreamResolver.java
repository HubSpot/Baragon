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
    try {
      if (address.contains(":")) {
        HostAndPort hostAndPort = HostAndPort.fromString(address);

        try {
          InetAddresses.forString(hostAndPort.getHost());
          return Optional.of(address); // `address` is already an IP
        } catch (IllegalArgumentException e) {
          // `address` is not an IP, continue and try to resolve it
        }

        String cached = resolveCache.getIfPresent(hostAndPort.getHost());
        String ip;
        int port = hostAndPort.getPort();
        if (cached != null) {
          ip = cached;
        } else {
          ip = InetAddress.getByName(hostAndPort.getHost()).getHostAddress();
          resolveCache.put(hostAndPort.getHost(), ip);
        }
        return Optional.of(String.format("%s:%d", ip, port));
      } else {
        return Optional.of(InetAddress.getByName(address).getHostAddress());
      }
    } catch (UnknownHostException uhe) {
      // Don't let this exception block rendering of the template, the lb config check will still fail if the host is truly unknown
      return Optional.absent();
    }
  }
}
