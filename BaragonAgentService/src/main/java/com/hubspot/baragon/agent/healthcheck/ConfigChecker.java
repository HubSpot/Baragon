package com.hubspot.baragon.agent.healthcheck;

import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.agent.BaragonAgentServiceModule;
import com.hubspot.baragon.agent.lbs.LocalLbAdapter;
import com.hubspot.baragon.exceptions.InvalidConfigException;

public class ConfigChecker implements Runnable {

  private final LocalLbAdapter adapter;
  private final AtomicReference<Optional<String>> errorMessage;

  @Inject
  public ConfigChecker(LocalLbAdapter adapter,
                       @Named(BaragonAgentServiceModule.CONFIG_ERROR_MESSAGE) AtomicReference<Optional<String>> errorMessage) {
    this.adapter = adapter;
    this.errorMessage = errorMessage;
  }

  @Override
  public void run() {
    try {
      adapter.checkConfigs();
      errorMessage.set(Optional.<String>absent());
    } catch (InvalidConfigException e) {
      errorMessage.set(Optional.of(e.getMessage()));
    }
  }
}
