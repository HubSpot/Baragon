package com.hubspot.baragon.service.managed;

import com.google.inject.Inject;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.service.worker.BaragonWorker;
import io.dropwizard.lifecycle.Managed;

import javax.inject.Named;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BaragonWorkerManaged implements Managed {
  private final ScheduledExecutorService executorService;
  private final BaragonWorker worker;

  @Inject
  public BaragonWorkerManaged(@Named(BaragonServiceModule.BARAGON_SERVICE_SCHEDULED_EXECUTOR) ScheduledExecutorService executorService,
                              BaragonWorker worker) {
    this.executorService = executorService;
    this.worker = worker;
  }

  @Override
  public void start() throws Exception {
    executorService.scheduleAtFixedRate(worker, 1, 1, TimeUnit.SECONDS);
  }

  @Override
  public void stop() throws Exception {
    executorService.shutdown();
  }
}
