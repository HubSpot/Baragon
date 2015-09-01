package com.hubspot.baragon.service.history;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.inject.Singleton;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class JDBIHistoryManager implements HistoryManager {
  private static final Logger LOG = LoggerFactory.getLogger(JDBIHistoryManager.class);

  private final ObjectMapper objectMapper;
  private final HistoryJDBI history;

  public JDBIHistoryManager(ObjectMapper objectMapper, HistoryJDBI history) {
    this.objectMapper = objectMapper;
    this.history = history;
  }

  public boolean saveRequestHistory(BaragonResponse response) {
    try {
      byte[] responseBytes = objectMapper.writeValueAsBytes(response);
      String serviceId;
      if (response.getRequest().isPresent()) {
        serviceId = response.getRequest().get().getLoadBalancerService().getServiceId();
      } else {
        serviceId = "";
      }
      history.insertRequestHistory(response.getLoadBalancerRequestId(), serviceId, new Date(), responseBytes);
      return true;
    } catch (Exception e) {
      LOG.error("Could not write hsitory to database", e);
      return false;
    }
  }

  public Optional<BaragonRequest> getRequestById(String requestId) {
    try {
      byte[] responseBytes = history.getRequestById(requestId);
      if (responseBytes == null) {
        return Optional.absent();
      } else {
        BaragonResponse response = objectMapper.readValue(responseBytes, BaragonResponse.class);
        return response.getRequest();
      }
    } catch (Exception e) {
      LOG.warn(String.format("Could not get history for request id: %s", requestId), e);
      return Optional.absent();
    }
  }

  public Optional<BaragonResponse> getRequestResponse(String requestId) {
    try {
      byte[] responseBytes = history.getRequestById(requestId);
      if (responseBytes == null) {
        return Optional.absent();
      } else {
        return Optional.of(objectMapper.readValue(responseBytes, BaragonResponse.class));
      }
    } catch (Exception e) {
      LOG.warn(String.format("Could not get history for request id: %s", requestId), e);
      return Optional.absent();
    }
  }

  public List<String> getRequestIds(int limitStart, int limitCount) {
    return history.getRequestIds(limitStart, limitCount);
  }

  public List<String> getReuqestIdsForService(String serviceId, int limitStart, int limitCount) {
    return history.getRequestsForService(serviceId, limitStart, limitCount);
  }
}
