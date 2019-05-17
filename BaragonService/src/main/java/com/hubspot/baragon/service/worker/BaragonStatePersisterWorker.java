package com.hubspot.baragon.service.worker;

import java.util.Collection;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.service.exceptions.BaragonExceptionNotifier;
import com.hubspot.baragon.state.persister.BaragonStatePersister;

public class BaragonStatePersisterWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonStatePersisterWorker.class);

  private final ServiceLoader<BaragonStatePersister> loader;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonExceptionNotifier exceptionNotifier;

  private BaragonStatePersisterWorker(BaragonStateDatastore stateDatastore,
                                      BaragonExceptionNotifier exceptionNotifier) {
    this.loader = ServiceLoader.load(BaragonStatePersister.class);
    this.stateDatastore = stateDatastore;
    this.exceptionNotifier = exceptionNotifier;
  }

  @Override
  public void run() {
    try {
      Collection<BaragonServiceState> state = stateDatastore.getGlobalState();
      Optional<Integer> maybeVersion = stateDatastore.getStateVersion();
      if (maybeVersion.isPresent()) {
        int version = maybeVersion.get();
        LOG.info("Baragon version: {}", version);
        for (BaragonStatePersister baragonStatePersister: loader) {
          baragonStatePersister.persist(state, version);
        }
      }
    } catch (Exception e) {
      LOG.error("Caught exception when persisting baragon service state", e);
      exceptionNotifier.notify(e, null);
    }
  }
}
