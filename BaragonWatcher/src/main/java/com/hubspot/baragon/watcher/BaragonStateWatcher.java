package com.hubspot.baragon.watcher;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadFactory;

import org.apache.curator.framework.listen.ListenerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.ringleader.watcher.Event;
import com.hubspot.ringleader.watcher.EventListener;
import com.hubspot.ringleader.watcher.PersistentWatcher;

@Singleton
public class BaragonStateWatcher implements Closeable {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonStateWatcher.class);

  private final BaragonStateFetcher stateFetcher;
  private final ListenerContainer<BaragonStateListener> listenerContainer;
  private final ExecutorService executor;
  private final BlockingQueue<Integer> versionQueue;

  @Inject
  public BaragonStateWatcher(final BaragonStateFetcher stateFetcher,
                             Set<BaragonStateListener> listeners,
                             @Baragon PersistentWatcher watcher) {
    this.stateFetcher = stateFetcher;
    this.listenerContainer = new ListenerContainer<>();
    this.executor = newExecutor();
    this.versionQueue = new LinkedTransferQueue<>();
    for (BaragonStateListener listener : listeners) {
      listenerContainer.addListener(listener);
    }

    watcher.getEventListenable().addListener(new EventListener() {

      @Override
      public void newEvent(Event event) {
        switch (event.getType()) {
          case NODE_UPDATED:
            int version = event.getStat().getVersion();
            versionQueue.add(version);
            executor.submit(new Runnable() {

              @Override
              public void run() {
                updateToLatestVersion();
              }
            });
            break;
          case NODE_DELETED:
            LOG.warn("Baragon state node was deleted");
            break;
          default:
            LOG.warn("Unrecognized event type {}", event.getType());
            break;
        }
      }
    }, executor);

    watcher.start();
  }

  @Override
  public void close() throws IOException {
    executor.shutdown();
  }

  private void updateToLatestVersion() {
    Deque<Integer> versions = new ArrayDeque<>();
    versionQueue.drainTo(versions);

    if (!versions.isEmpty()) {
      int latestVersion = versions.getLast();
      final Collection<BaragonServiceState> newState = stateFetcher.fetchState(latestVersion);

      listenerContainer.forEach(new Function<BaragonStateListener, Void>() {

        @Override
        public Void apply(BaragonStateListener listener) {
          listener.stateChanged(newState);
          return null;
        }
      });
    }
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
