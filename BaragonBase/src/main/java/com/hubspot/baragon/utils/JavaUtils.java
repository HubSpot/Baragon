package com.hubspot.baragon.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Enumeration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class JavaUtils {
  private JavaUtils() { }

  public static String getHostAddress() throws Exception {
    Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
    while (interfaces.hasMoreElements()) {
      NetworkInterface current = interfaces.nextElement();
      if (!current.isUp() || current.isLoopback() || current.isVirtual())
        continue;
      Enumeration<InetAddress> addresses = current.getInetAddresses();
      while (addresses.hasMoreElements()) {
        InetAddress current_addr = addresses.nextElement();
        if (current_addr.isLoopbackAddress())
          continue;
        if (current_addr instanceof Inet4Address) {
          return current_addr.getHostAddress();
        }
      }
    }
    throw new RuntimeException("Couldn't deduce host address");
  }

  public static boolean reduceBooleanFutures(Collection<Future<Boolean>> futures) throws InterruptedException {
    for (Future<Boolean> future : futures) {
      try {
        final Boolean result = future.get();
        if (result == null || result.booleanValue() == false) {
          return false;
        }
      } catch (ExecutionException e) {
        return false;
      }
    }

    return true;
  }
}
