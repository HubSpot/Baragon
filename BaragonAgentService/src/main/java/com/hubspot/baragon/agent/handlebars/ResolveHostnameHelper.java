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
    try {
      if (address.contains(":")) {
        HostAndPort hostAndPort = HostAndPort.fromString(address);
        InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(hostAndPort.getHost()), hostAndPort.getPort());
        return String.format("%s:%d", socketAddress.getAddress().getHostAddress(), socketAddress.getPort());
      } else {
        return InetAddress.getByName(address).getHostAddress();
      }
    } catch (UnknownHostException uhe) {
      // Don't let this exception block rendering of the template, the lb config check will still fail if the host is truly unknown
      return address;
    }
  }
}
