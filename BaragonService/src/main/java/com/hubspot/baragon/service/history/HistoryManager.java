package com.hubspot.baragon.service.history;

import java.util.Date;
import java.util.List;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonResponse;

public interface HistoryManager {

  boolean saveRequestHistory(BaragonResponse response, Date updatedAt);

  void deleteRequestHistoryOlderThan(Date referenceDate);

  Optional<BaragonRequest> getRequestById(String requestId);

  Optional<BaragonResponse> getRequestResponse(String requestId);

  List<String> getRequestIds(int limitStart, int limitCount);

  List<String> getReuqestIdsForService(String serviceId, int limitStart, int limitCount);

}
