package com.hubspot.baragon.service.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.kubernetes.KubernetesWatcher;

import io.fabric8.kubernetes.client.Watch;

public class KubernetesWatchListener extends AbstractLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(ElbSyncWorkerListener.class);

  private final KubernetesWatcher kubernetesWatcher;
  private final KubernetesConfiguration kubernetesConfiguration;

  private Watch watch;

  @Inject
  public KubernetesWatchListener(KubernetesWatcher kubernetesWatcher,
                                 KubernetesConfiguration kubernetesConfiguration) {
    this.kubernetesWatcher = kubernetesWatcher;
    this.kubernetesConfiguration = kubernetesConfiguration;
  }

  @Override
  public void isLeader() {
    LOG.info("We are the leader! Starting KubernetesWatcher...");
    watch = kubernetesWatcher.createWatch(true);

  }

  @Override
  public void notLeader() {
    if (watch != null) {
      LOG.info("Closing kubernetes watch");
      watch.close();
    }
  }

  @Override
  public boolean isEnabled() {
    return kubernetesConfiguration.isEnabled();
  }
}
