package com.hubspot.baragon.data;

import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;

@Singleton
public class BaragonConnectionStateListener implements ConnectionStateListener {
  private final AtomicReference<ConnectionState> connectionState;

  @Inject
  public BaragonConnectionStateListener(@Named(BaragonDataModule.BARAGON_ZK_CONNECTION_STATE) AtomicReference<ConnectionState> connectionState) {
    this.connectionState = connectionState;
  }

  @Override
  public void stateChanged(CuratorFramework client, ConnectionState newState) {
    connectionState.set(newState);
  }
}
