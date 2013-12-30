package com.hubspot.baragon.agent.poller;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.agent.BaragonAgentManager;
import com.hubspot.baragon.config.LoadBalancerConfiguration;
import com.hubspot.baragon.data.BaragonDataStore;
import com.hubspot.baragon.healthchecks.HealthCheckClient;
import com.hubspot.baragon.models.ServiceInfo;
import com.hubspot.baragon.utils.LogUtils;
import com.hubspot.baragon.utils.ResponseUtils;
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

@Singleton
public class PollerRunnable implements Runnable {
  private static final Log LOG = LogFactory.getLog(PollerRunnable.class);

  private final BaragonDataStore datastore;
  private final LoadBalancerConfiguration loadBalancerConfiguration;
  private final AsyncHttpClient asyncHttpClient;
  private final WebhookNotifier webhookNotifier;
  private final BaragonAgentManager agentManager;

  @Inject
  public PollerRunnable(BaragonDataStore datastore, LoadBalancerConfiguration loadBalancerConfiguration,
                        @HealthCheckClient AsyncHttpClient asyncHttpClient, WebhookNotifier webhookNotifier,
                        BaragonAgentManager agentManager) {
    this.datastore = datastore;
    this.loadBalancerConfiguration = loadBalancerConfiguration;
    this.asyncHttpClient = asyncHttpClient;
    this.webhookNotifier = webhookNotifier;
    this.agentManager = agentManager;
  }

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
                if (!ResponseUtils.isSuccess(response)) {
                  LogUtils.serviceInfoMessage(LOG, serviceInfo, "%s is now UNHEALTHY (HTTP %d)", upstream, response.getStatusCode());
                  datastore.makeUpstreamUnhealthy(serviceInfo.getName(), serviceInfo.getId(), upstream);
                  webhookNotifier.notify(new WebhookEvent(WebhookEvent.EventType.UPSTREAM_UNHEALTHY, loadBalancerConfiguration.getName(), serviceInfo, upstream));
                  return true;
                }
                return false;
              }

              @Override
              public void onThrowable(Throwable t) {
                LogUtils.serviceInfoMessage(LOG, serviceInfo, "%s is now UNHEALTHY (Exception: %s)", upstream, t.getMessage());
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
                if (ResponseUtils.isSuccess(response)) {
                  LogUtils.serviceInfoMessage(LOG, serviceInfo, "%s is now HEALTHY", upstream);
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
    for (String serviceName : datastore.getPendingServices()) {
      Optional<ServiceInfo> maybeServiceInfo = datastore.getPendingService(serviceName);

      if (!maybeServiceInfo.isPresent()) {
        LOG.warn(String.format("%s is listed as a pending service, but no service info exists!", serviceName));
        continue;
      }

      if (maybeServiceInfo.get().getLbs().contains(loadBalancerConfiguration.getName())) {
        checkService(maybeServiceInfo.get(), false);
      }
    }

    for (String serviceName : datastore.getActiveServices()) {
      Optional<ServiceInfo> maybeServiceInfo = datastore.getActiveService(serviceName);

      if (!maybeServiceInfo.isPresent()) {
        LOG.warn(String.format("%s is listed as an active service, but no service info exists!", serviceName));
        continue;
      }

      if (maybeServiceInfo.get().getLbs().contains(loadBalancerConfiguration.getName())) {
        checkService(maybeServiceInfo.get(), true);
      }
    }
  }
}