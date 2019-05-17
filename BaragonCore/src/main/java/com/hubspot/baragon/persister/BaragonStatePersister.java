package com.hubspot.baragon.persister;

import java.util.Collection;

import com.hubspot.baragon.models.BaragonServiceState;

public interface BaragonStatePersister {
  void persist(Collection<BaragonServiceState> state, int version);
}
