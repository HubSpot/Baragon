package com.hubspot.baragon.service.healthcheck;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.curator.framework.state.ConnectionState;

import com.codahale.metrics.health.HealthCheck;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;

@Singleton
public class ZooKeeperHealthcheck extends HealthCheck {
  private final AtomicReference<ConnectionState> connectionState;

  @Inject
  public ZooKeeperHealthcheck(@Named(BaragonDataModule.BARAGON_ZK_CONNECTION_STATE) AtomicReference<ConnectionState> connectionState) {
    this.connectionState = connectionState;
  }


  @Override
  protected Result check() throws Exception {
    final ConnectionState currentConnectionState = connectionState.get();

    if (currentConnectionState == null) {
      return Result.unhealthy("Connection state is null");
    }

    switch (currentConnectionState) {
      case CONNECTED:
      case RECONNECTED:
        return Result.healthy(currentConnectionState.name());
      default:
        return Result.unhealthy(currentConnectionState.name());
    }
  }
}
