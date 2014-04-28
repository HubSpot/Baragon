package com.hubspot.baragon.service.worker;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.RequestState;
import com.hubspot.baragon.models.Service;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.utils.JavaUtils;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.curator.framework.recipes.leader.LeaderLatch;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class PendingRequestWorker implements Runnable {
  private static final Log LOG = LogFactory.getLog(PendingRequestWorker.class);

  private final BaragonRequestDatastore requestDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final AsyncHttpClient asyncHttpClient;
  private final String baragonAgentRequestUriFormat;
  private final LeaderLatch leaderLatch;

  public static final AsyncCompletionHandler<Boolean> IS_RESPONSE_SUCCESSFUL = new AsyncCompletionHandler<Boolean>() {
    @Override
    public Boolean onCompleted(Response response) throws Exception {
      return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
    }
  };

  @Inject
  public PendingRequestWorker(BaragonRequestDatastore requestDatastore,
                              BaragonStateDatastore stateDatastore,
                              BaragonLoadBalancerDatastore loadBalancerDatastore,
                              @Named(BaragonServiceModule.BARAGON_AGENT_REQUEST_URI_FORMAT) String baragonAgentRequestUriFormat,
                              @Named(BaragonServiceModule.BARAGON_SERVICE_LEADER_LATCH) LeaderLatch leaderLatch,
                              @Named(BaragonServiceModule.BARAGON_SERVICE_HTTP_CLIENT) AsyncHttpClient asyncHttpClient) {
    this.requestDatastore = requestDatastore;
    this.stateDatastore = stateDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.baragonAgentRequestUriFormat = baragonAgentRequestUriFormat;
    this.leaderLatch = leaderLatch;
    this.asyncHttpClient = asyncHttpClient;
  }

  @Override
  public void run() {
    if (!leaderLatch.hasLeadership()) {
      return;
    }

    final List<String> pendingRequestIds = requestDatastore.getPendingRequestIds();

    for (String pendingRequestId : pendingRequestIds) {
      LOG.info(String.format("Handling pending request: %s", pendingRequestId));

      final Optional<String> maybeRequestId = BaragonRequestDatastore.parsePendingRequestId(pendingRequestId);

      if (maybeRequestId.isPresent()) {
        final String requestId = maybeRequestId.get();
        final Optional<BaragonRequest> maybeRequest = requestDatastore.getRequest(requestId);

        if (maybeRequest.isPresent()) {
          final BaragonRequest request = maybeRequest.get();

          // Get base URIs for all participating Agents
          final Collection<String> baseUris = loadBalancerDatastore.getAllHosts(request.getLoadBalancerService().getLoadBalancerGroups());

          try {
            applyRequest(request, baseUris);
          } catch (InterruptedException e) {
            LOG.error(String.format("Caught InterruptedException while handling %s", request), e);
            continue;
          }
        }
      }

      // Remove pending request from queue
      requestDatastore.clearPendingRequest(pendingRequestId);
    }
  }

  private boolean areBaseUrisAvailable(BaragonRequest request) {
    final Service service = request.getLoadBalancerService();

    for (String loadBalancerGroup : service.getLoadBalancerGroups()) {
      final Optional<String> maybeServiceId = loadBalancerDatastore.getBaseUriServiceId(loadBalancerGroup, service.getLoadBalancerBaseUri());
      if (maybeServiceId.isPresent() && !maybeServiceId.equals(service.getServiceId())) {
        return false;
      }
    }

    return true;
  }

  private boolean requestedLoadBalancersExist(BaragonRequest request) {
    final Service service = request.getLoadBalancerService();
    final Collection<String> loadBalancerGroups = loadBalancerDatastore.getClusters();

    for (String loadBalancerGroup : service.getLoadBalancerGroups()) {
      if (!loadBalancerGroups.contains(loadBalancerGroup)) {
        return false;
      }
    }

    return true;
  }

  private void applyRequest(BaragonRequest request, Collection<String> baseUrls) throws InterruptedException {
    final Stopwatch stopwatch = new Stopwatch();

    // Check to see that the desired base URI is available on all desired load balancers
    if (!areBaseUrisAvailable(request)) {
      LOG.info(String.format("Request %s: Failed due to URI in use"));
      requestDatastore.updateResponse(request.getLoadBalancerRequestId(), RequestState.FAILED, Optional.of("Base URI is already in use"));
      return;
    }

    // Check to see that all desired load balancer groups exist
    if (!requestedLoadBalancersExist(request)) {
      LOG.info(String.format("Request %s: Failed due to some load balancers not existing"));
      requestDatastore.updateResponse(request.getLoadBalancerRequestId(), RequestState.FAILED, Optional.of("One or more load balancer groups do not exist"));
      return;
    }

    // If no load balancers defined, immediately apply request
    if (baseUrls.isEmpty()) {
      LOG.info(String.format("    No hosts defined for LB(s), directly applying %s...", request.getLoadBalancerRequestId()));
      stateDatastore.applyRequest(request);
      return;
    }

    // Relay request to agent(s)
    stopwatch.start();
    final boolean success = JavaUtils.reduceBooleanFutures(sendApplyRequests(request, baseUrls));
    stopwatch.stop();

    // Re-check state, since it could have been cancelled mid-apply
    final Optional<BaragonResponse> maybeResponse = requestDatastore.getResponse(request.getLoadBalancerRequestId());

    if (maybeResponse.isPresent() && maybeResponse.get().getLoadBalancerState() == RequestState.CANCELING) {
      LOG.info("Request %s: MARKED FOR CANCELLATION");
      sendRevertRequests(request, baseUrls);
      requestDatastore.updateResponse(request.getLoadBalancerRequestId(), RequestState.CANCELED, Optional.of("Reverted"));
    } else if (success) {
      LOG.info(String.format("Request %s: SUCCESS (%sms)", request.getLoadBalancerRequestId(), stopwatch.elapsed(TimeUnit.MILLISECONDS)));
      requestDatastore.updateResponse(request.getLoadBalancerRequestId(), RequestState.SUCCESS, Optional.<String>absent());

      stateDatastore.applyRequest(request);
      for (String loadBalancerGroup : request.getLoadBalancerService().getLoadBalancerGroups()) {
        loadBalancerDatastore.setBaseUriServiceId(loadBalancerGroup, request.getLoadBalancerService().getLoadBalancerBaseUri(), request.getLoadBalancerService().getServiceId());
      }
    } else {
      LOG.info(String.format("Request %s: FAILED (%sms)", request.getLoadBalancerRequestId(), stopwatch.elapsed(TimeUnit.MILLISECONDS)));
      requestDatastore.updateResponse(request.getLoadBalancerRequestId(), RequestState.FAILED, Optional.of("Failed"));
      sendRevertRequests(request, baseUrls);
    }
  }

  private Collection<Future<Boolean>> sendApplyRequests(BaragonRequest request, Collection<String> baseUrls) {
    final Collection<Future<Boolean>> futures = Lists.newArrayListWithCapacity(baseUrls.size());

    for (final String baseUrl : baseUrls) {
      try {
        futures.add(asyncHttpClient.preparePost(String.format(baragonAgentRequestUriFormat, baseUrl, request.getLoadBalancerRequestId())).execute(IS_RESPONSE_SUCCESSFUL));
      } catch (Exception e) {
        futures.add(Futures.immediateFuture(false));
      }
    }

    return futures;
  }

  private Collection<Future<Boolean>> sendRevertRequests(BaragonRequest request, Collection<String> baseUrls) {
    final Collection<Future<Boolean>> futures = Lists.newArrayListWithCapacity(baseUrls.size());

    for (final String baseUrl : baseUrls) {
      try {
        futures.add(asyncHttpClient.prepareDelete(String.format(baragonAgentRequestUriFormat, baseUrl, request.getLoadBalancerRequestId())).execute(IS_RESPONSE_SUCCESSFUL));
      } catch (IOException e) {
        futures.add(Futures.immediateFuture(false));
      }
    }

    return futures;
  }
}
