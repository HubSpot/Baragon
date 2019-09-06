package com.hubspot.baragon.agent.kubernetes;

import com.google.inject.Inject;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.kubernetes.KubernetesWatcher;

import io.dropwizard.lifecycle.Managed;

public class KubernetesWatcherManaged implements Managed {
  private final KubernetesConfiguration kubernetesConfiguration;
  private final KubernetesWatcher kubernetesWatcher;

  @Inject
  public KubernetesWatcherManaged(KubernetesConfiguration kubernetesConfiguration,
                                  KubernetesWatcher kubernetesWatcher) {
    this.kubernetesConfiguration = kubernetesConfiguration;
    this.kubernetesWatcher = kubernetesWatcher;
  }

  @Override
  public void start() {
    if (kubernetesConfiguration.isEnabled()) {
      kubernetesWatcher.createWatch(false);
    }
  }

  @Override
  public void stop() {
    if (kubernetesConfiguration.isEnabled()) {
      kubernetesWatcher.close();
    }
  }
}
