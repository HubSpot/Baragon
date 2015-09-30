package com.hubspot.baragon.watcher;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.ringleader.watcher.Event;
import com.hubspot.ringleader.watcher.EventListener;
import com.hubspot.ringleader.watcher.PersistentWatcher;
import org.apache.curator.framework.listen.ListenerContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class BaragonStateWatcher {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonStateWatcher.class);

  @Inject
  public BaragonStateWatcher(final BaragonStateFetcher stateFetcher,
                             Set<BaragonStateListener> listeners,
                             @Baragon PersistentWatcher watcher) {
    ExecutorService executor = newExecutor();

    final ListenerContainer<BaragonStateListener> listenerContainer = new ListenerContainer<>();
    for (BaragonStateListener listener : listeners) {
      listenerContainer.addListener(listener, executor);
    }

    watcher.getEventListenable().addListener(new EventListener() {

      @Override
      public void newEvent(Event event) {
        final Collection<BaragonServiceState> newState;

        switch (event.getType()) {
          case NODE_UPDATED:
            int version = event.getStat().getVersion();
            newState = stateFetcher.fetchState(version);
            break;
          case NODE_DELETED:
            newState = Collections.emptyList();
            break;
          default:
            LOG.warn("Unrecognized event type {}", event.getType());
            return;
        }

        listenerContainer.forEach(new Function<BaragonStateListener, Void>() {

          @Override
          public Void apply(BaragonStateListener listener) {
            listener.stateChanged(newState);
            return null;
          }
        });
      }
    }, executor);

    watcher.start();
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
