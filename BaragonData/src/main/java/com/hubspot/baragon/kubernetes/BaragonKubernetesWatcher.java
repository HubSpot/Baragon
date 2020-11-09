package com.hubspot.baragon.kubernetes;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;

public abstract class BaragonKubernetesWatcher<T> implements Watcher<T>, Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(KubernetesEndpointsWatcher.class);

  private final AtomicBoolean closing;
  protected Watch watch;
  protected boolean processInitialFetch = false;

  public BaragonKubernetesWatcher() {
    this.closing = new AtomicBoolean(false);
  }

  public abstract void createWatch(boolean processInitialFetch);

  @Override
  public void onClose(KubernetesClientException cause) {
    if (!closing.get()) {
      LOG.error("Watch closed early, restarting", cause);
      createWatch(processInitialFetch);
    } else {
      LOG.info("Watch closed {}", cause.getMessage());
    }
  }

  @Override
  public void close() {
    LOG.info("Closing kubernetes watcher");
    closing.set(true);
    if (watch != null) {
      watch.close();
    }
  }
}
