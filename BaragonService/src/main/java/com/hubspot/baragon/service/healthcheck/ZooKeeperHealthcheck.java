package com.hubspot.baragon.service.healthcheck;

import java.util.concurrent.atomic.AtomicReference;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.dropwizard.guice.InjectableHealthCheck;
import org.apache.curator.framework.state.ConnectionState;

public class ZooKeeperHealthcheck extends InjectableHealthCheck {
  private final AtomicReference<ConnectionState> connectionState;

  @Inject
  public ZooKeeperHealthcheck(@Named(BaragonDataModule.BARAGON_ZK_CONNECTION_STATE) AtomicReference<ConnectionState> connectionState) {
    this.connectionState = connectionState;
  }

  @Override
  public String getName() {
    return "zookeeper";
  }

  @Override
  protected HealthCheck.Result check() throws Exception {
    final ConnectionState currentConnectionState = connectionState.get();

    if (currentConnectionState == null) {
      return HealthCheck.Result.unhealthy("Connection state is null");
    }

    switch (currentConnectionState) {
      case CONNECTED:
      case RECONNECTED:
        return HealthCheck.Result.healthy(currentConnectionState.name());
      default:
        return HealthCheck.Result.unhealthy(currentConnectionState.name());
    }
  }
}
