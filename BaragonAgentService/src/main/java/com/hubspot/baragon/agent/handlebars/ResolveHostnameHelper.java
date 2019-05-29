package com.hubspot.baragon.agent.handlebars;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.HostAndPort;

public class ResolveHostnameHelper implements Helper<String> {

  public static final String NAME = "resolveHostname";

  private final Cache<String, String> resolveCache;

  public ResolveHostnameHelper(long maxSize, long expireAfterDays) {
    this.resolveCache = CacheBuilder.newBuilder()
        .maximumSize(maxSize)
        .expireAfterAccess(expireAfterDays, TimeUnit.DAYS)
        .build();
  }

  @Override
  public CharSequence apply(String address, Options options) {
    try {
      if (address.contains(":")) {
        HostAndPort hostAndPort = HostAndPort.fromString(address);
        String cached = resolveCache.getIfPresent(hostAndPort.getHost());
        String ip;
        int port = hostAndPort.getPort();
        if (cached != null) {
          ip = cached;
        } else {
          ip = InetAddress.getByName(hostAndPort.getHost()).getHostAddress();
          resolveCache.put(hostAndPort.getHost(), ip);
        }
        return String.format("%s:%d", ip, port);
      } else {
        return InetAddress.getByName(address).getHostAddress();
      }
    } catch (UnknownHostException uhe) {
      // Don't let this exception block rendering of the template, the lb config check will still fail if the host is truly unknown
      return address;
    }
  }
}
