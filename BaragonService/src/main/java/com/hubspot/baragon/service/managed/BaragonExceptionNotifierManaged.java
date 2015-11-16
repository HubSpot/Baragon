package com.hubspot.baragon.service.managed;


import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;
import com.hubspot.baragon.service.exceptions.NotifyingExceptionMapper;
import com.hubspot.baragon.service.exceptions.NotifyingUncaughtExceptionManager;

import io.dropwizard.lifecycle.Managed;

@Singleton
public class BaragonExceptionNotifierManaged implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(NotifyingExceptionMapper.class);

  private final BaragonExceptionNotifier exceptionNotifier;

  @Inject
  public BaragonExceptionNotifierManaged(BaragonExceptionNotifier exceptionNotifier) {
    this.exceptionNotifier = exceptionNotifier;
  }

  @Override
  public void start() throws Exception {
    LOG.info("Setting NotifyingUncaughtExceptionManager as the default uncaught exception provider...");
    Thread.setDefaultUncaughtExceptionHandler(new NotifyingUncaughtExceptionManager(exceptionNotifier));
  }

  @Override
  public void stop() throws Exception {

  }
}
