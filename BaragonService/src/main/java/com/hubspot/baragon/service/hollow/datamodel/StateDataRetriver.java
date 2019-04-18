package com.hubspot.baragon.service.hol.datamodel;


import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableCollection;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.watcher.Baragon;
import com.hubspot.baragon.watcher.BaragonStateFetcher;
import com.hubspot.ringleader.watcher.Event;
import com.hubspot.ringleader.watcher.EventListener;
import com.hubspot.ringleader.watcher.PersistentWatcher;

public class StateDataRetriver {
  private static final Logger LOG = LoggerFactory.getLogger(StateDataRetriver.class);

  private final BaragonStateFetcher stateFetcher;
  private final ExecutorService executor;
  private Optional<BaragonHollowState> baragonHollowState;
  private final BlockingQueue<Integer> versionQueue;

  public StateDataRetriver(BaragonStateFetcher stateFetcher,
                           @Baragon PersistentWatcher persistentWatcher,
                           BlockingQueue<Integer> versionQueue) {
    this.stateFetcher = stateFetcher;
    this.executor = newExecutor();
    this.versionQueue = versionQueue;
    this.baragonHollowState = Optional.empty();

    persistentWatcher.getEventListenable().addListener(new EventListener() {
      @Override
      public void newEvent(Event event) {
        switch (event.getType()) {
          case NODE_UPDATED:
            int version = event.getStat().getVersion();
            versionQueue.add(version);
            executor.submit(new Runnable() {
              @Override
              public void run() {
                updateToLatestVersionAndState();
              }
            });
        }
      }
    }, executor);

    persistentWatcher.start();
  }

  private Collection<BaragonServiceState> fetchBaragonServiceState(int version) {
    final Collection<BaragonServiceState> baragonServiceStates = stateFetcher.fetchState(version);
    if (baragonServiceStates instanceof ImmutableCollection) {
      LOG.error("Received 404 from Baragon service state endpooint");
    }
    return baragonServiceStates;
  }

  private void updateToLatestVersionAndState() {
    Deque<Integer> versions = new ArrayDeque<>();
    versionQueue.drainTo(versions);

    if (!versions.isEmpty()) {
      int latestVersion = versions.getLast();
      Collection<BaragonServiceState> baragonServiceStates = fetchBaragonServiceState(latestVersion);
      baragonHollowState = Optional.of(new BaragonHollowState(baragonServiceStates, latestVersion));
    }
  }

  public Optional<BaragonHollowState> getBaragonHollowState() {
    return baragonHollowState;
  }

  private ExecutorService newExecutor() {
    return Executors.newSingleThreadExecutor(new ThreadFactory() {

      @Override
      public Thread newThread(Runnable r) {
        Thread thread = Executors.defaultThreadFactory().newThread(r);
        thread.setName("BaragonStateWatcher");
        thread.setDaemon(true);
        return thread;
      }
    });

  }

}
