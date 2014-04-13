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
import com.hubspot.baragon.models.RequestState;
import com.hubspot.baragon.service.BaragonServiceModule;
import com.hubspot.baragon.utils.ResponseUtils;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BaragonWorker implements Runnable {
  private static final Log LOG = LogFactory.getLog(BaragonWorker.class);

  private final BaragonRequestDatastore requestDatastore;
  private final BaragonStateDatastore stateDatastore;
  private final BaragonLoadBalancerDatastore loadBalancerDatastore;
  private final Queue<BaragonRequest> queue;
  private final AsyncHttpClient asyncHttpClient;

  public static final AsyncCompletionHandler<Boolean> SUCCESSFUL_RESPONSE = new AsyncCompletionHandler<Boolean>() {
    @Override
    public Boolean onCompleted(Response response) throws Exception {
      return ResponseUtils.isSuccess(response);
    }
  };

  @Inject
  public BaragonWorker(BaragonRequestDatastore requestDatastore, BaragonStateDatastore stateDatastore, BaragonLoadBalancerDatastore loadBalancerDatastore,
                       @Named(BaragonServiceModule.BARAGON_SERVICE_QUEUE) Queue<BaragonRequest> queue,
                       AsyncHttpClient asyncHttpClient) {
    this.requestDatastore = requestDatastore;
    this.stateDatastore = stateDatastore;
    this.loadBalancerDatastore = loadBalancerDatastore;
    this.queue = queue;
    this.asyncHttpClient = asyncHttpClient;
  }

  @Override
  public void run() {
    while (!queue.isEmpty()) {
      final BaragonRequest request = queue.remove();

      LOG.info(String.format("Handling %s", request));

      final Collection<String> baseUrls = loadBalancerDatastore.getAllHosts(request.getLoadBalancerService().getLbs());

      if (baseUrls.isEmpty()) {
        LOG.info(String.format("    No hosts defined for LB(s), directly applying %s...", request.getLoadBalancerRequestId()));
        stateDatastore.applyRequest(request);
      } else {
        LOG.info(String.format("    Applying %s to %d host(s)...", request.getLoadBalancerRequestId(), baseUrls.size()));
        applyRequest(request, baseUrls);
      }
    }
  }

  private void applyRequest(BaragonRequest request, Collection<String> baseUrls) {
    final Collection<Future<Boolean>> futures = Lists.newArrayListWithCapacity(baseUrls.size());

    final Stopwatch stopwatch = new Stopwatch();

    stopwatch.start();
    for (final String baseUrl : baseUrls) {
      try {
        futures.add(asyncHttpClient.preparePost(String.format("%s/request/%s", baseUrl, request.getLoadBalancerRequestId())).execute(SUCCESSFUL_RESPONSE));
      } catch (Exception e) {
        futures.add(Futures.immediateFuture(false));
      }
    }

    boolean success = true;

    for (Future<Boolean> future : futures) {
      try {
        success = success && future.get();
      } catch (Exception e) {
        success = false;
      }
    }

    stopwatch.stop();

    // re-check state, since it could have been cancelled mid-apply
    final Optional<RequestState> maybeNewState = requestDatastore.getRequestState(request.getLoadBalancerRequestId());

    if (maybeNewState.isPresent() && maybeNewState.get() == RequestState.CANCELING) {
      LOG.info("Request %s: MARKED FOR CANCELLATION");
      revertRequest(request, baseUrls);
      requestDatastore.setRequestState(request.getLoadBalancerRequestId(), RequestState.CANCELED);
    } else if (success) {
      LOG.info(String.format("Request %s: SUCCESS (%sms)", request.getLoadBalancerRequestId(), stopwatch.elapsed(TimeUnit.MILLISECONDS)));
      requestDatastore.setRequestState(request.getLoadBalancerRequestId(), RequestState.SUCCESS);

      stateDatastore.applyRequest(request);
    } else {
      LOG.info(String.format("Request %s: FAILED (%sms)", request.getLoadBalancerRequestId(), stopwatch.elapsed(TimeUnit.MILLISECONDS)));
      requestDatastore.setRequestState(request.getLoadBalancerRequestId(), RequestState.FAILED);
      revertRequest(request, baseUrls);
    }
  }

  private Collection<Future<Boolean>> revertRequest(BaragonRequest request, Collection<String> baseUrls) {
    final Collection<Future<Boolean>> futures = Lists.newArrayListWithCapacity(baseUrls.size());

    for (final String baseUrl : baseUrls) {
      try {
        futures.add(asyncHttpClient.prepareDelete(String.format("%s/request/%s", baseUrl, request.getLoadBalancerRequestId())).execute(SUCCESSFUL_RESPONSE));
      } catch (IOException e) {
        futures.add(Futures.immediateFuture(false));
      }
    }

    return futures;
  }
}
