package com.hubspot.baragon.hollow;

import com.hubspot.baragon.models.BaragonServiceState;

import java.util.Collection;

public interface BaragonStatePersister {
  void persist(Collection<BaragonServiceState> state);
}


