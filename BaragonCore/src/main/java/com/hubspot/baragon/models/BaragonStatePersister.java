package com.hubspot.baragon.models;

import java.util.Collection;

public interface BaragonStatePersister {
  void persist(Collection<BaragonServiceState> state, int version);
}
