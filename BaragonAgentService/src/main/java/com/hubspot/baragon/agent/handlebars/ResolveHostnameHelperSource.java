package com.hubspot.baragon.agent.handlebars;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.google.common.net.HostAndPort;

public class ResolveHostnameHelperSource {
  public static CharSequence resolveHostname(String address) throws UnknownHostException {
    if (address.contains(":")) {
      HostAndPort hostAndPort = HostAndPort.fromString(address);
      InetSocketAddress socketAddress = new InetSocketAddress(hostAndPort.getHostText(), hostAndPort.getPort());
      return String.format("%s:%d", socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
    } else {
      return InetAddress.getByName(address).getHostAddress();
    }
  }
}
