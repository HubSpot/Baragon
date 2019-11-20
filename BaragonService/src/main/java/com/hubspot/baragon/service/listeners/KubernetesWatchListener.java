package com.hubspot.baragon.service.listeners;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.kubernetes.KubernetesEndpointsWatcher;
import com.hubspot.baragon.kubernetes.KubernetesServiceWatcher;

public class KubernetesWatchListener extends AbstractLatchListener {
  private static final Logger LOG = LoggerFactory.getLogger(ElbSyncWorkerListener.class);

  private final KubernetesEndpointsWatcher kubernetesEndpointsWatcher;
  private final KubernetesServiceWatcher kubernetesServiceWatcher;
  private final KubernetesConfiguration kubernetesConfiguration;

  @Inject
  public KubernetesWatchListener(KubernetesEndpointsWatcher kubernetesEndpointsWatcher,
                                 KubernetesServiceWatcher kubernetesServiceWatcher,
                                 KubernetesConfiguration kubernetesConfiguration) {
    this.kubernetesEndpointsWatcher = kubernetesEndpointsWatcher;
    this.kubernetesServiceWatcher = kubernetesServiceWatcher;
    this.kubernetesConfiguration = kubernetesConfiguration;
  }

  @Override
  public void isLeader() {
    LOG.info("We are the leader! Starting KubernetesWatchers...");
    kubernetesServiceWatcher.createWatch(true);
    kubernetesEndpointsWatcher.createWatch(true);

  }

  @Override
  public void notLeader() {
    kubernetesServiceWatcher.close();
    kubernetesEndpointsWatcher.close();
  }

  @Override
  public boolean isEnabled() {
    return kubernetesConfiguration.isEnabled();
  }
}
