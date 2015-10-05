package com.hubspot.baragon.data;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.models.BaragonResponse;

@Singleton
public class BaragonResponseHistoryDatastore extends AbstractDataStore {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonResponseHistoryDatastore.class);

  public static final String RESPONSE_HISTORIES_FORMAT = "/responseHistory";
  public static final String RESPONSE_HISTORIES_FOR_SERVICE_FORMAT = RESPONSE_HISTORIES_FORMAT + "/%s";
  public static final String RESPONSE_HISTORY_FORMAT = RESPONSE_HISTORIES_FOR_SERVICE_FORMAT + "/%s";
  public static final String SERVICE_ID_FOR_REQUEST_FORMAT = RESPONSE_HISTORIES_FORMAT + "/requestIdMapping/%s";


  @Inject
  public BaragonResponseHistoryDatastore(CuratorFramework curatorFramework, ObjectMapper objectMapper, ZooKeeperConfiguration zooKeeperConfiguration) {
    super(curatorFramework, objectMapper, zooKeeperConfiguration);
  }

  @Timed
  public void addResponse(String serviceId, String requestId, BaragonResponse response) {
    writeToZk(String.format(RESPONSE_HISTORY_FORMAT, serviceId, requestId), response);
    writeToZk(String.format(SERVICE_ID_FOR_REQUEST_FORMAT, requestId), serviceId);
  }

  @Timed
  public Optional<String> getServiceIdForRequestId(String requestId) {
    return readFromZk(String.format(SERVICE_ID_FOR_REQUEST_FORMAT, requestId), String.class);
  }

  @Timed
  public Optional<BaragonResponse> getResponse(String serviceId, String requestId) {
    return readFromZk(String.format(RESPONSE_HISTORY_FORMAT, serviceId, requestId), BaragonResponse.class);
  }

  @Timed
  public List<BaragonResponse> getResponsesForService(String serviceId, int limit) {
    final List<String> nodes = getChildren(String.format(RESPONSE_HISTORIES_FOR_SERVICE_FORMAT, serviceId));
    final List<BaragonResponse> responses = Lists.newArrayListWithCapacity(Math.min(nodes.size(), limit));
    for (String requestId : nodes.subList(0, Math.min(nodes.size(), limit))) {
      try {
        responses.addAll(readFromZk(String.format(RESPONSE_HISTORY_FORMAT, serviceId, requestId), BaragonResponse.class).asSet());
      } catch (Exception e) {
        LOG.error(String.format("Could not fetch info for group %s due to error %s", requestId, e));
      }
    }
    return responses;
  }

  @Timed
  public Optional<Long> getRequestUpdatedAt(String serviceId, String requestId) {
    return getUpdatedAt(String.format(RESPONSE_HISTORY_FORMAT, serviceId, requestId));
  }

  @Timed
  public List<String> getServiceIds() {
    return getChildren(RESPONSE_HISTORIES_FORMAT);
  }

  @Timed
  public List<String> getRequestIdsForService(String serviceId) {
    return getChildren(String.format(RESPONSE_HISTORIES_FOR_SERVICE_FORMAT, serviceId));
  }

  @Timed
  public void deleteResponse(String serviceId, String requestId) {
    deleteNode(String.format(RESPONSE_HISTORY_FORMAT, serviceId, requestId));
    deleteNode(String.format(SERVICE_ID_FOR_REQUEST_FORMAT, requestId));
  }

  @Timed
  public void deleteResponses(String serviceId) {
    String path = String.format(RESPONSE_HISTORIES_FOR_SERVICE_FORMAT, serviceId);
    List<String> requestIds = getChildren(path);
    deleteNode(path, true);
    for (String requestId : requestIds) {
      deleteNode(String.format(SERVICE_ID_FOR_REQUEST_FORMAT, requestId));
    }
  }
}
