package com.hubspot.baragon.watcher;

import java.util.Collection;

import com.hubspot.baragon.models.BaragonServiceState;

public interface BaragonStateListener {
  void stateChanged(Collection<BaragonServiceState> newState);
}
