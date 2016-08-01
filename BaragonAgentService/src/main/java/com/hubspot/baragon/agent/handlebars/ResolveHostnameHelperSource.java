package com.hubspot.baragon.agent.handlebars;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.google.common.net.HostAndPort;

public class ResolveHostnameHelperSource {
  public static CharSequence resolveHostname(String hostname) throws UnknownHostException {
    if (hostname.contains(":")) {
      HostAndPort hostAndPort = HostAndPort.fromString(hostname);
      InetSocketAddress socketAddress = new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
      return String.format("%s:%d", socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
    } else {
      return InetAddress.getByName(hostname).getHostAddress();
    }
  }
}
