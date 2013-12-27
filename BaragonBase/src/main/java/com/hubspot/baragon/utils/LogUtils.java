package com.hubspot.baragon.utils;

import com.google.common.base.Joiner;
import com.hubspot.baragon.models.ServiceInfo;
import org.apache.commons.logging.Log;

public class LogUtils {
  private LogUtils() { }

  public static final Joiner COMMA_JOINER = Joiner.on(", ");

  public static void serviceInfoMessage(Log log, ServiceInfo serviceInfo, String message, Object... args) {
    log.info(String.format("%s:%s | ", serviceInfo.getName(), serviceInfo.getId()) + String.format(message, args));
  }
}
