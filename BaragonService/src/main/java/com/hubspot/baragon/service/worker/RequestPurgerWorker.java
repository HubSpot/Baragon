package com.hubspot.baragon.service.worker;

import java.util.Date;
import java.util.List;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonRequestDatastore;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.service.history.HistoryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestPurgerWorker implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(RequestPurgerWorker.class);

  private static final long DAY_IN_MS = 1000 * 60 * 60 * 24;

  private final BaragonRequestDatastore requestDatastore;
  private final HistoryManager historyManager;
  private final BaragonConfiguration configuration;

  @Inject
  public RequestPurgerWorker(BaragonRequestDatastore requestDatastore,
                             HistoryManager historyManager,
                             BaragonConfiguration configuration) {
    this.requestDatastore = requestDatastore;
    this.historyManager = historyManager;
    this.configuration = configuration;
  }

  @Override
  public void run() {
    purgeFromZK();
    if (configuration.getDatabaseConfiguration().isPresent()) {
      purgeFromDB();
    }
  }

  private void purgeFromZK() {
    List<String> allRequestIds = requestDatastore.getAllRequestIds();
    Date referenceDate = new Date(System.currentTimeMillis() - (configuration.getHistoryConfiguration().getPurgeOldRequestsAfterDays() * DAY_IN_MS));
    for (String requestId : allRequestIds) {
      Optional<Date> maybeUpdatedAt = requestDatastore.getRequestUpdatedAt(requestId);
      if (maybeUpdatedAt.isPresent() && maybeUpdatedAt.get().before(referenceDate) || !maybeUpdatedAt.isPresent() && configuration.getHistoryConfiguration().isPurgeWhenDateNotFound()) {
        requestDatastore.deleteRequest(requestId);
      }
    }
  }

  private void purgeFromDB() {
    Date referenceDate = new Date(System.currentTimeMillis() - (configuration.getHistoryConfiguration().getPurgeOldRequestsAfterDays() * DAY_IN_MS));
    historyManager.deleteRequestHistoryOlderThan(referenceDate);
  }
}
