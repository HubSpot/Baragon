package com.hubspot.baragon.service.history;

import java.util.Date;
import java.util.List;

import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;

@UseStringTemplate3StatementLocator
public interface HistoryJDBI {

  @SqlUpdate("INSERT INTO requestHistory (requestId, serviceId, status, createdAt, updatedAt, bytes) VALUES (:requestId, :serviceId, :status, :createdAt, :updatedAt, :response)")
  void insertRequestHistory(@Bind("requestId") String requestId, @Bind("serviceId") String serviceId, @Bind("createdAt") Date createdAt, @Bind("bytes") byte[] response);

  @SqlQuery("SELECT bytes FROM requestHistory WHERE requestId = :requestId")
  byte[] getRequestById(@Bind("taskId") String requestId);

  @SqlQuery("SELECT requestId FROM requestHistory ORDER BY createdAt DESC LIMIT :limitStart, :limitCount")
  List<String> getRequestIds(@Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

  @SqlQuery("SELECT requestId FROM requestHistory WHERE serviceId = :serviceId LIMIT :limitStart, :limitCount")
  List<String> getRequestsForService(@Bind("serviceId") String serviceId, @Bind("limitStart") Integer limitStart, @Bind("limitCount") Integer limitCount);

}
