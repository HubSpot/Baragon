package com.hubspot.baragon.service.hollow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableCollection;
import com.hubspot.baragon.models.BaragonServiceState;

import com.hubspot.baragon.watcher.Baragon;
import com.hubspot.baragon.watcher.BaragonStateFetcher;
import com.hubspot.ringleader.watcher.Event;
import com.hubspot.ringleader.watcher.Event.Type;
import com.hubspot.ringleader.watcher.PersistentWatcher;

public class BaragonShipStateToHollowWorker {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonShipStateToHollowWorker.class);

  private static final int SLEEP_MS = 1_000;
  private static final int LOG_MS = 60_000;

  private final BlockingQueue<BaragonHollowState> stateQueue;
  private final BaragonStateFetcher stateFetcher;
  private final Stopwatch stopwatch;

  private AtomicInteger stateCount;
  private AtomicInteger skippedCount;
  private AtomicInteger errorCount;

  public BaragonShipStateToHollowWorker(BaragonStateFetcher stateFetcher,
                                        @Baragon PersistentWatcher watcher) {
    this.stateFetcher = stateFetcher;
    this.stopwatch = Stopwatch.createStarted();
    this.stateQueue = new LinkedBlockingQueue<>();
    this.stateCount = new AtomicInteger(0);
    this.skippedCount = new AtomicInteger(0);
    this.errorCount = new AtomicInteger(0);

    watcher.getEventListenable().addListener(this::handleEvent);

  }

  private synchronized void handleEvent(Event event) {
    if (event.getType() != Type.NODE_UPDATED) {
      return;
    }

    int version = event.getStat().getVersion();
    LOG.debug("Baragon version: {}", version);

    try {
      stateQueue.put(new BaragonHollowState(fetchBaragonServiceState(version), version));
    } catch (InterruptedException e) {
      LOG.error("Interrupted enqueuing state {} for replication", version);
      errorCount.incrementAndGet();
    }
  }

  private void processBaragonServiceStates() {
    while (true) {
      try {
        List<BaragonHollowState> baragonHollowStates = new ArrayList<>();
        baragonHollowStates.add(stateQueue.take());
        stateQueue.drainTo(baragonHollowStates);

        BaragonHollowState state = getBaragonHollowState(baragonHollowStates);
        LOG.debug("Processing Baragon state: {}", state.getVersion());

      } catch (InterruptedException e) {
        LOG.error("Interrupted processing states, stopping...");
        return;
      }
    }
  }

  private Collection<BaragonServiceState> fetchBaragonServiceState(int version) {
    final Collection<BaragonServiceState> baragonServiceStates = stateFetcher.fetchState(version);
    if (baragonServiceStates instanceof ImmutableCollection) {
      LOG.error("Received 404 from Baragon service state endpooint");
    }
    return baragonServiceStates;
  }

  private BaragonHollowState getBaragonHollowState(List<BaragonHollowState> states) {
    return states.stream()
        .max(Comparator.comparing(BaragonHollowState::getVersion))
        .orElseThrow(() -> new RuntimeException("This should never happen"));
  }

  public void start() {
    while (sleep(SLEEP_MS)) {
      log();
    }
  }

  private boolean sleep(long millis) {
    try {
      Thread.sleep(millis);
      return true;
    } catch (InterruptedException e) {
      return false;
    }
  }

  private void log() {
    long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    if (elapsed > LOG_MS) {
      int count = stateCount.get();
      int errors = errorCount.get();
      int skipped = skippedCount.get();
      int successCount = Math.max(0, count - errors);

      LOG.info("Wrote {} state updates (success: {}, failed: {}, skipped: {}) to hollow in the last {} ms",
          count,
          successCount,
          errors,
          skipped,
          elapsed);

      stateCount.set(0);
      errorCount.set(0);
      skippedCount.set(0);
      stopwatch.reset().start();
    }
  }

}
