package com.hubspot.baragon.service.worker;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.QueuedRequestId;
import com.hubspot.baragon.models.QueuedRequestWithState;
import com.hubspot.baragon.models.UpstreamInfo;

public class BaragonRequestWorkerTest {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonRequestWorkerTest.class);

  @Test
  public void testQueuedRequestComparator() throws Exception {
    List<QueuedRequestWithState> queuedRequestsWithState = Arrays.asList(
        new QueuedRequestWithState(
            new QueuedRequestId("serviceA", "requestIdA", 0),
            new BaragonRequest(
                "requestIdA", null,
                Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())),
                Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())),
                Collections.emptyList(), Optional.absent(), Optional.absent(), false, false, false, false),
            null
        ),
        new QueuedRequestWithState(
            new QueuedRequestId("serviceB", "requestIdB", 0),
            new BaragonRequest(
                "requestIdB", null,
                Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())),
                Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())),
                Collections.emptyList(), Optional.absent(), Optional.absent(), true, true, false, false),
            null
        ),
        new QueuedRequestWithState(
            new QueuedRequestId("serviceC", "requestIdC", 0),
            new BaragonRequest(
                "requestIdC", null,
                Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())),
                Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())),
                Collections.emptyList(), Optional.absent(), Optional.absent(), true, false, false, false),
            null
        ),
        new QueuedRequestWithState(
            new QueuedRequestId("serviceC", "requestIdC", 0),
            new BaragonRequest(
                "requestIdC", null,
                Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())),
                Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())),
                Collections.emptyList(), Optional.absent(), Optional.absent(), false, true, false, false),
            null
        )
    );

    List<QueuedRequestWithState> sortedQueuedRequestsWithState = new ArrayList<>(queuedRequestsWithState);
    sortedQueuedRequestsWithState.sort(BaragonRequestWorker.queuedRequestComparator());

    assertEquals(queuedRequestsWithState.get(1), sortedQueuedRequestsWithState.get(0));
    assertEquals(queuedRequestsWithState.get(0), sortedQueuedRequestsWithState.get(3));
  }
}
