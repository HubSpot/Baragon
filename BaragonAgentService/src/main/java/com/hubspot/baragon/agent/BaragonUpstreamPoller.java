package com.hubspot.baragon.agent;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonDataStore;
import com.hubspot.baragon.models.ServiceInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BaragonUpstreamPoller {
  private static final Log LOG = LogFactory.getLog(BaragonUpstreamPoller.class);

  private final ScheduledExecutorService scheduledExecutorService;
  private final BaragonDataStore datastore;

  @Inject
  public BaragonUpstreamPoller(ScheduledExecutorService scheduledExecutorService, BaragonDataStore datastore) {
    this.scheduledExecutorService = scheduledExecutorService;
    this.datastore = datastore;
  }

  public void start() {
    scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

      @Override
      public void run() {
        for (String serviceName : datastore.getActiveServices()) {
          Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(serviceName);

          if (!maybeServiceInfo.isPresent()) {
            continue; // wtf
          }

          LOG.info("TODO: check upstreams");  // TODO: check upstreams
        }
      }
    }, 30000, 30000, TimeUnit.MILLISECONDS);
  }

  public void stop() {
    try {
      scheduledExecutorService.shutdownNow();
      scheduledExecutorService.awaitTermination(1, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
