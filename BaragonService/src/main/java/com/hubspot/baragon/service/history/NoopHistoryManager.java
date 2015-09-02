package com.hubspot.baragon.service.history;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;

public class NoopHistoryManager implements HistoryManager {

  public boolean saveRequestHistory(BaragonResponse response, Date updatedAt) {
    throw new UnsupportedOperationException("NoopHistoryManager can not save");
  }

  public void deleteRequestHistoryOlderThan(Date updatedAt) {
    throw new UnsupportedOperationException("NoopHistoryManager can not delete");
  }

  public Optional<BaragonRequest> getRequestById(String requestId) {
    return Optional.absent();
  }

  public Optional<BaragonResponse> getRequestResponse(String requestId) {
    return Optional.absent();
  }

  public List<String> getRequestIds(int limitStart, int limitCount) {
    return Collections.emptyList();
  }

  public List<String> getReuqestIdsForService(String serviceId, int limitStart, int limitCount) {
    return Collections.emptyList();
  }
}
