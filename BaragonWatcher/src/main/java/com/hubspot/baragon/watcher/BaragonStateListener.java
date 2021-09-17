package com.hubspot.baragon.watcher;

import com.hubspot.baragon.models.BaragonServiceState;
import java.util.Collection;

public interface BaragonStateListener {
  void stateChanged(Collection<BaragonServiceState> newState);
}
