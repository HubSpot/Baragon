package com.hubspot.baragon.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.RequestState;
import org.apache.curator.framework.CuratorFramework;

import java.util.List;
import java.util.regex.Pattern;

@Singleton
public class BaragonRequestDatastore extends AbstractDataStore {
  public static final String REQUEST_FORMAT = "/singularity/request/%s";
  public static final String PENDING_REQUESTS_FORMAT = "/singularity/pending";
  public static final String CREATE_PENDING_REQUEST_FORMAT = "/singularity/pending/%s_";
  public static final String PENDING_REQUEST_FORMAT = "/singularity/pending/%s";
  public static final String RESPONSE_FORMAT = "/singularity/request/%s/response";

  public static final Pattern PENDING_REQUEST_ID_REGEX = Pattern.compile("^(.*?)_(\\d+)$");

  public static Optional<String> parsePendingRequestId(String pendingRequestId) {
    return Optional.fromNullable(PENDING_REQUEST_ID_REGEX.matcher(pendingRequestId).group(1));
  }

  @Inject
  public BaragonRequestDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper) {
    super(curatorFramework, objectMapper);
  }

  public Optional<BaragonResponse> getResponse(String requestId) {
    return readFromZk(String.format(RESPONSE_FORMAT, requestId), BaragonResponse.class);
  }

  public Optional<BaragonRequest> getRequest(String requestId) {
    return readFromZk(String.format(REQUEST_FORMAT, requestId), BaragonRequest.class);
  }

  public BaragonResponse updateResponse(String requestId, RequestState state, Optional<String> message) {
    writeToZk(String.format(RESPONSE_FORMAT, requestId), new BaragonResponse(requestId, state, message));
    return new BaragonResponse(requestId, state, message);
  }

  public Optional<BaragonResponse> cancelRequest(String requestId) {
    final Optional<BaragonResponse> maybeResponse = getResponse(requestId);

    if (maybeResponse.isPresent() && maybeResponse.get().getLoadBalancerState() == RequestState.WAITING) {
      return Optional.of(updateResponse(requestId, RequestState.CANCELING, Optional.<String>absent()));
    }

    return Optional.absent();
  }

  //
  // PENDING REQUESTS
  //

  public BaragonResponse addPendingRequest(BaragonRequest request) {
    final String requestId = request.getLoadBalancerRequestId();

    if (!nodeExists(String.format(REQUEST_FORMAT, requestId))) {
      writeToZk(String.format(REQUEST_FORMAT, requestId), request);
      writeToZk(String.format(RESPONSE_FORMAT, requestId), new BaragonResponse(requestId, RequestState.WAITING, Optional.<String>absent()));
      createPersistentSequentialNode(String.format(CREATE_PENDING_REQUEST_FORMAT, requestId));
    }

    return getResponse(request.getLoadBalancerRequestId()).get();  // very rare and worth throwing if absent
  }

  public List<String> getPendingRequestIds() {
    return getChildren(PENDING_REQUESTS_FORMAT);
  }

  public void clearPendingRequest(String pendingRequestId) {
    deleteNode(String.format(PENDING_REQUEST_FORMAT, pendingRequestId));
  }
}
