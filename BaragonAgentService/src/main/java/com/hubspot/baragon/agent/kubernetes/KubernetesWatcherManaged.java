package com.hubspot.baragon.agent.kubernetes;

import com.google.inject.Inject;
import com.hubspot.baragon.config.KubernetesConfiguration;
import com.hubspot.baragon.kubernetes.KubernetesEndpointsWatcher;
import com.hubspot.baragon.kubernetes.KubernetesServiceWatcher;

import io.dropwizard.lifecycle.Managed;

public class KubernetesWatcherManaged implements Managed {
  private final KubernetesConfiguration kubernetesConfiguration;
  private final KubernetesEndpointsWatcher kubernetesEndpointsWatcher;
  private final KubernetesServiceWatcher kubernetesServiceWatcher;

  @Inject
  public KubernetesWatcherManaged(KubernetesConfiguration kubernetesConfiguration,
                                  KubernetesEndpointsWatcher kubernetesEndpointsWatcher,
                                  KubernetesServiceWatcher kubernetesServiceWatcher) {
    this.kubernetesConfiguration = kubernetesConfiguration;
    this.kubernetesEndpointsWatcher = kubernetesEndpointsWatcher;
    this.kubernetesServiceWatcher = kubernetesServiceWatcher;
  }

  @Override
  public void start() {
    if (kubernetesConfiguration.isEnabled()) {
      kubernetesServiceWatcher.createWatch(false);
      kubernetesEndpointsWatcher.createWatch(false);
    }
  }

  @Override
  public void stop() {
    if (kubernetesConfiguration.isEnabled()) {
      kubernetesServiceWatcher.close();
      kubernetesEndpointsWatcher.close();
    }
  }
}
