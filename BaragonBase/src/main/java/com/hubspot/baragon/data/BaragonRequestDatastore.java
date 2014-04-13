package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.RequestState;
import org.apache.curator.framework.CuratorFramework;

@Singleton
public class BaragonRequestDatastore extends AbstractDataStore {
  public static final String REQUEST_FORMAT = "/singularity/request/%s";
  public static final String REQUEST_STATE_FORMAT = "/singularity/request/%s/state";

  @Inject
  public BaragonRequestDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public Optional<RequestState> getRequestState(String requestId) {
    return readFromZk(String.format(REQUEST_STATE_FORMAT, requestId), RequestState.class);
  }

  public Optional<BaragonRequest> getRequest(String requestId) {
    return readFromZk(String.format(REQUEST_FORMAT, requestId), BaragonRequest.class);
  }

  public BaragonResponse addRequest(BaragonRequest request) {
    if (!nodeExists(String.format(REQUEST_FORMAT, request.getLoadBalancerRequestId()))) {
      writeToZk(String.format(REQUEST_FORMAT, request.getLoadBalancerRequestId()), request);
      writeToZk(String.format(REQUEST_STATE_FORMAT, request.getLoadBalancerRequestId()), RequestState.WAITING);
    }

    final Optional<RequestState> maybeState = getRequestState(request.getLoadBalancerRequestId());

    return new BaragonResponse(request.getLoadBalancerRequestId(), maybeState.get());  // very rare and worth throwing if maybeState is empty
  }

  public void setRequestState(String requestId, RequestState state) {
    writeToZk(String.format(REQUEST_STATE_FORMAT, requestId), state);
  }

  public Optional<BaragonResponse> cancelRequest(String requestId) {
    final Optional<RequestState> maybeRequestState = getRequestState(requestId);

    if (!maybeRequestState.isPresent()) {
      return Optional.absent();
    }

    if (maybeRequestState.get() == RequestState.WAITING) {
      writeToZk(String.format(REQUEST_STATE_FORMAT, requestId), RequestState.CANCELING);
      return Optional.of(new BaragonResponse(requestId, RequestState.CANCELING));
    } else {
      return Optional.of(new BaragonResponse(requestId, maybeRequestState.get()));
    }
  }
}
