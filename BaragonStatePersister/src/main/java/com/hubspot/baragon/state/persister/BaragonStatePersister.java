package com.hubspot.baragon.state.persister;

import java.util.Collection;

import com.hubspot.baragon.models.BaragonServiceState;

public interface BaragonStatePersister {
  void persist(Collection<BaragonServiceState> state, int version);

  void start() throws Exception;

  void stop() throws Exception;
}
