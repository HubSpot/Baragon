package com.hubspot.baragon.service.history;

import java.util.Date;
import java.util.List;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

@UseStringTemplate3StatementLocator
public interface HistoryJDBI {

  @SqlUpdate("INSERT INTO responseHistory (requestId, serviceId, bytes, updatedAt) VALUES (:requestId, :serviceId, :bytes, :updatedAt)")
  void insertRequestHistory(@Bind("requestId") String requestId, @Bind("serviceId") String serviceId, @Bind("updatedAt") Date updatedAt, @Bind("bytes") byte[] response);

  @SqlQuery("SELECT bytes FROM responseHistory WHERE requestId = :requestId")
  byte[] getRequestById(@Bind("requestId") String requestId);

  @SqlQuery("SELECT requestId FROM responseHistory ORDER BY updatedAt DESC LIMIT :limitStart, :limitCount")
  List<String> getRequestIds(@Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT requestId FROM responseHistory WHERE serviceId = :serviceId ORDER BY updatedAt DESC LIMIT :limitStart, :limitCount")
  List<String> getRequestsForService(@Bind("serviceId") String serviceId, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("DELETE FROM responseHistory WHERE updatedAt < :referenceDate")
  void deleteHistoryOlderThan(@Bind("referenceDate") Date referenceDate);
}
