package com.hubspot.baragon.service.worker;

import java.util.concurrent.atomic.AtomicLong;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonDataModule;
import com.hubspot.baragon.service.managers.ElbManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BaragonElbSyncWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonElbSyncWorker.class);

  private final ElbManager elbManager;
  private final AtomicLong workerLastStartAt;

  @Inject
  public BaragonElbSyncWorker(ElbManager elbManager,
                              @Named(BaragonDataModule.BARAGON_ELB_WORKER_LAST_START) AtomicLong workerLastStartAt) {
    this.elbManager = elbManager;
    this.workerLastStartAt = workerLastStartAt;
  }

  @Override
  public void run() {
    workerLastStartAt.set(System.currentTimeMillis());
    elbManager.syncAll();
    LOG.info("Finished ELB Sync");
  }


}
