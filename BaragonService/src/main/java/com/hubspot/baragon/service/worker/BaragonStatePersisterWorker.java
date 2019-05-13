package com.hubspot.baragon.service.worker;

import java.util.ServiceLoader;

import com.hubspot.baragon.state.persister.BaragonStatePersister;

public class BaragonStatePersisterWorker implements Runnable {

  private ServiceLoader<BaragonStatePersister> loader;

  private BaragonStatePersisterWorker() {
    loader = ServiceLoader.load(BaragonStatePersister.class);
  }

  @Override
  public void run() {
    for (BaragonStatePersister baragonStatePersister: loader) {
      //TODO: do the work
    }
  }

}
