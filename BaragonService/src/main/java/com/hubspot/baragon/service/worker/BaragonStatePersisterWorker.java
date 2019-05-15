package com.hubspot.baragon.service.worker;

import java.util.Collection;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.state.persister.BaragonStatePersister;

public class BaragonStatePersisterWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonStatePersisterWorker.class);

  private final ServiceLoader<BaragonStatePersister> loader;
  private final BaragonStateDatastore stateDatastore;

  private BaragonStatePersisterWorker(BaragonStateDatastore stateDatastore) {
    this.loader = ServiceLoader.load(BaragonStatePersister.class);
    this.stateDatastore = stateDatastore;
  }

  @Override
  public void run() {
    Collection<BaragonServiceState> state = stateDatastore.getGlobalState();
    int version = stateDatastore.getStateVersion().or(-1);

    LOG.info("Baragon version: {}", version);
    for (BaragonStatePersister baragonStatePersister: loader) {
      baragonStatePersister.persist(state, version);
    }

  }
}
