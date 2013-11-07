package com.hubspot.baragon.agent;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.data.BaragonDataStore;
import com.hubspot.baragon.healthchecks.HealthCheckClient;
import com.hubspot.baragon.models.ServiceInfo;
import com.hubspot.baragon.webhooks.WebhookEvent;
import com.hubspot.baragon.webhooks.WebhookNotifier;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BaragonUpstreamPoller {
  private static final Log LOG = LogFactory.getLog(BaragonUpstreamPoller.class);

  private final ScheduledExecutorService scheduledExecutorService;
  private final BaragonDataStore datastore;
  private final BaragonAgentManager agentManager;
  private final AsyncHttpClient asyncHttpClient;
  private final WebhookNotifier webhookNotifier;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final int pollInterval;

  @Inject
  public BaragonUpstreamPoller(@HealthCheckClient AsyncHttpClient asyncHttpClient,
                               ScheduledExecutorService scheduledExecutorService, BaragonDataStore datastore,
                               @Named(BaragonAgentServiceModule.UPSTREAM_POLL_INTERVAL_PROPERTY) int pollInterval,
                               BaragonAgentManager agentManager, WebhookNotifier webhookNotifier,
                               LoadBalancerConfiguration loadBalancerConfiguration) {
    this.asyncHttpClient = asyncHttpClient;
    this.scheduledExecutorService = scheduledExecutorService;
    this.datastore = datastore;
    this.pollInterval = pollInterval;
    this.agentManager = agentManager;
    this.webhookNotifier = webhookNotifier;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
  }

  private boolean isSuccess(Response response) {
    return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
  }

  public void start() {
    LOG.info(String.format("Elected leader! Polling upstreams every %sms.", pollInterval));

    scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
      private void checkService(final ServiceInfo serviceInfo, boolean applyConfigs) {
        final Collection<String> healthy = datastore.getHealthyUpstreams(serviceInfo.getName(), serviceInfo.getId());
        final Collection<String> unhealthy = datastore.getUnhealthyUpstreams(serviceInfo.getName(), serviceInfo.getId());
        final Collection<Future<Boolean>> futures = Lists.newLinkedList();

        for (final String upstream : healthy) {
          try {
            futures.add(asyncHttpClient.prepareGet(String.format("http://%s%s", upstream, serviceInfo.getHealthCheck()))
                .execute(new AsyncCompletionHandler<Boolean>() {
                  @Override
                  public Boolean onCompleted(Response response) throws Exception {
                    if (!isSuccess(response)) {
                      LOG.info(String.format("%s-%s %s is now UNHEALTHY (HTTP %d)", serviceInfo.getName(), serviceInfo.getId(), upstream, response.getStatusCode()));
                      datastore.makeUpstreamUnhealthy(serviceInfo.getName(), serviceInfo.getId(), upstream);
                      webhookNotifier.notify(new WebhookEvent(WebhookEvent.EventType.UPSTREAM_UNHEALTHY, loadBalancerConfiguration.getName(), serviceInfo, upstream));
                      return true;
                    }
                    return false;
                  }

                  @Override
                  public void onThrowable(Throwable t) {
                    LOG.info(String.format("%s-%s %s is now UNHEALTHY (%s)", serviceInfo.getName(), serviceInfo.getId(), upstream, t.getMessage()));
                    datastore.makeUpstreamUnhealthy(serviceInfo.getName(), serviceInfo.getId(), upstream);
                    webhookNotifier.notify(new WebhookEvent(WebhookEvent.EventType.UPSTREAM_UNHEALTHY, loadBalancerConfiguration.getName(), serviceInfo, upstream));
                  }
                }));
          } catch (IOException e) {
            // wat do
          }
        }

        for (final String upstream : unhealthy) {
          try {
            futures.add(asyncHttpClient.prepareGet(String.format("http://%s%s", upstream, serviceInfo.getHealthCheck()))
                .execute(new AsyncCompletionHandler<Boolean>() {
                  @Override
                  public Boolean onCompleted(Response response) throws Exception {
                    if (isSuccess(response)) {
                      LOG.info(String.format("%s-%s %s is now HEALTHY", serviceInfo.getName(), serviceInfo.getId(), upstream));
                      datastore.makeUpstreamHealthy(serviceInfo.getName(), serviceInfo.getId(), upstream);
                      webhookNotifier.notify(new WebhookEvent(WebhookEvent.EventType.UPSTREAM_HEALTHY, loadBalancerConfiguration.getName(), serviceInfo, upstream));
                      return true;
                    }
                    return false;
                  }
                }));
          } catch (IOException e) {
            // no worries.
          }
        }

        boolean shouldUpdateProject = false;

        for (Future<Boolean> future : futures) {
          try {
            shouldUpdateProject = shouldUpdateProject || future.get();
          } catch (Exception e) {
            // dont care
          }
        }

        if (applyConfigs && shouldUpdateProject) {
          agentManager.apply(serviceInfo, datastore.getHealthyUpstreams(serviceInfo.getName(), serviceInfo.getId()));
        }
      }

      @Override
      public void run() {
        for (String serviceName: datastore.getPendingServices()) {
          Optional<ServiceInfo> maybeServiceInfo = datastore.getPendingService(serviceName);

          if (maybeServiceInfo.isPresent()) {
            checkService(maybeServiceInfo.get(), false);
          }
        }

        for (String serviceName : datastore.getActiveServices()) {
          Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(serviceName);

          if (maybeServiceInfo.isPresent()) {
            checkService(maybeServiceInfo.get(), true);
          }
        }
      }
    }, pollInterval, pollInterval, TimeUnit.MILLISECONDS);
  }

  public void stop() {
    try {
      LOG.info("Lost leadership. Stopping upstream poller...");
      scheduledExecutorService.shutdownNow();
      scheduledExecutorService.awaitTermination(1, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
