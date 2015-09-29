package com.hubspot.baragon.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.commons.lang3.time.DurationFormatUtils;

import com.google.common.base.Joiner;

public class JavaUtils {
  public static final Joiner COMMA_JOINER = Joiner.on(", ").skipNulls();

  private JavaUtils() { }

  public static String getHostAddress() throws Exception {
    final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

    while (interfaces.hasMoreElements()) {
      final NetworkInterface current = interfaces.nextElement();

      if (!current.isUp() || current.isLoopback() || current.isVirtual()) {
        continue;
      }

      final Enumeration<InetAddress> addresses = current.getInetAddresses();

      while (addresses.hasMoreElements()) {
        final InetAddress current_addr = addresses.nextElement();

        if (current_addr.isLoopbackAddress()) {
          continue;
        }

        if (current_addr instanceof Inet4Address) {
          return current_addr.getHostAddress();
        }
      }
    }

    throw new RuntimeException("Couldn't deduce host address");
  }

  private static final String DURATION_FORMAT = "mm:ss.S";

  public static String duration(final long start) {
    return DurationFormatUtils.formatDuration(System.currentTimeMillis() - start, DURATION_FORMAT);
  }
}
