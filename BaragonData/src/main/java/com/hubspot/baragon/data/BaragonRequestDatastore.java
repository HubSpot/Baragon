package com.hubspot.baragon.data;

import java.util.Collections;
import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.QueuedRequestId;

@Singleton
public class BaragonRequestDatastore extends AbstractDataStore {
  public static final String REQUESTS_FORMAT = "/request";
  public static final String REQUEST_FORMAT = REQUESTS_FORMAT + "/%s";
  public static final String REQUEST_STATE_FORMAT = REQUEST_FORMAT + "/status";
  public static final String REQUEST_MESSAGE_FORMAT = REQUEST_FORMAT + "/message";

  public static final String REQUEST_QUEUE_FORMAT = "/queue";
  public static final String REQUEST_ENQUEUE_FORMAT = REQUEST_QUEUE_FORMAT + "/%s|%s|";
  public static final String REQUEST_QUEUE_ITEM_FORMAT = REQUEST_QUEUE_FORMAT + "/%s";

  @Inject
  public BaragonRequestDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper, ZooKeeperConfiguration zooKeeperConfiguration) {
    super(curatorFramework, objectMapper, zooKeeperConfiguration);
  }

  //
  // REQUEST DATA
  //
  @Timed
  public void addRequest(BaragonRequest request) {
    writeToZk(String.format(REQUEST_FORMAT, request.getLoadBalancerRequestId()), request);
  }

  @Timed
  public Optional<BaragonRequest> getRequest(String requestId) {
    return readFromZk(String.format(REQUEST_FORMAT, requestId), BaragonRequest.class);
  }

  @Timed
  public Optional<BaragonRequest> deleteRequest(String requestId) {
    final Optional<BaragonRequest> maybeRequest = getRequest(requestId);

    if (maybeRequest.isPresent()) {
      deleteNode(String.format(REQUEST_FORMAT, requestId), true);
    }

    return maybeRequest;
  }

  @Timed
  public List<String> getAllRequestIds() {
    return getChildren(REQUESTS_FORMAT);
  }

  @Timed
  public Optional<Long> getRequestUpdatedAt(String requestId) {
    return getUpdatedAt(String.format(String.format(REQUEST_STATE_FORMAT, requestId)));
  }

  //
  // REQUEST STATE
  //
  public boolean activeRequestExists(String requestId) {
    return nodeExists(String.format(REQUEST_FORMAT, requestId));
  }

  @Timed
  public Optional<InternalRequestStates> getRequestState(String requestId) {
    return readFromZk(String.format(REQUEST_STATE_FORMAT, requestId), InternalRequestStates.class);
  }

  @Timed
  public void setRequestState(String requestId, InternalRequestStates state) {
    writeToZk(String.format(REQUEST_STATE_FORMAT, requestId), state);
  }

  // REQUEST MESSAGE
  @Timed
  public Optional<String> getRequestMessage(String requestId) {
    return readFromZk(String.format(REQUEST_MESSAGE_FORMAT, requestId), String.class);
  }

  @Timed
  public void setRequestMessage(String requestId, String message) {
    writeToZk(String.format(REQUEST_MESSAGE_FORMAT, requestId), message);
  }

  //
  // REQUEST QUEUING
  //
  @Timed
  public QueuedRequestId enqueueRequest(BaragonRequest request) {
    final String path = createPersistentSequentialNode(String.format(REQUEST_ENQUEUE_FORMAT, request.getLoadBalancerService().getServiceId(), request.getLoadBalancerRequestId()));

    return QueuedRequestId.fromString(ZKPaths.getNodeFromPath(path));
  }

  @Timed
  public List<QueuedRequestId> getQueuedRequestIds() {
    final List<String> nodes = getChildren(REQUEST_QUEUE_FORMAT);

    Collections.sort(nodes, SEQUENCE_NODE_COMPARATOR_LOW_TO_HIGH);

    final List<QueuedRequestId> queuedRequestIds = Lists.newArrayListWithCapacity(nodes.size());

    for (String node : nodes) {
      queuedRequestIds.add(QueuedRequestId.fromString(node));
    }

    return queuedRequestIds;
  }

  @Timed
  public int getQueuedRequestCount() {
    return getChildren(REQUEST_QUEUE_FORMAT).size();
  }

  @Timed
  public void removeQueuedRequest(QueuedRequestId queuedRequestId) {
    deleteNode(String.format(REQUEST_QUEUE_ITEM_FORMAT, queuedRequestId.buildZkPath()));
  }
}
