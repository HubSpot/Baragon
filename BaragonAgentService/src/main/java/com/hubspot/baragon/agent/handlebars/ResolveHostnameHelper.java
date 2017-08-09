package com.hubspot.baragon.agent.handlebars;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.google.common.net.HostAndPort;

public class ResolveHostnameHelper implements Helper<String> {

  public static final String NAME = "resolveHostname";

  @Override
  public CharSequence apply(String address, Options options) throws UnknownHostException {
    if (address.contains(":")) {
      HostAndPort hostAndPort = HostAndPort.fromString(address);
      InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(hostAndPort.getHost()), hostAndPort.getPort());
      return String.format("%s:%d", socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
    } else {
      return InetAddress.getByName(address).getHostAddress();
    }
  }
}
