package com.hubspot.baragon.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

import io.dropwizard.lifecycle.Managed;

public class BaragonKubernetesManaged implements Managed {
  private static final Logger LOG = LoggerFactory.getLogger(KubernetesWatcher.class);

  private final KubernetesWatcher watcher;

  @Inject BaragonKubernetesManaged(KubernetesWatcher watcher) {
    this.watcher = watcher;
  }

  @Override
  public void start() throws Exception {
    LOG.info("Starting kubernetes watcher.");
    watcher.run();
  }

  @Override
  public void stop() throws Exception {
  }
}
