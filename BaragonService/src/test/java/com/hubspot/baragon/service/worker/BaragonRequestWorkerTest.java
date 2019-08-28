package com.hubspot.baragon.service.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.hubspot.baragon.models.BaragonRequestBuilder;
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
            new BaragonRequestBuilder().setLoadBalancerRequestId("requestIdA")
                .setLoadBalancerService(null)
                .setAddUpstreams(Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())))
                .setRemoveUpstreams(Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())))
                .setReplaceUpstreams(Collections.emptyList())
                .setAction(Optional.absent())
                .setNoValidate(false)
                .setNoReload(false)
                .setUpstreamUpdateOnly(false)
                .setNoDuplicateUpstreams(false)
                .build(),
            null
        ),
        new QueuedRequestWithState(
            new QueuedRequestId("serviceB", "requestIdB", 0),
            new BaragonRequestBuilder().setLoadBalancerRequestId("requestIdB")
                .setLoadBalancerService(null)
                .setAddUpstreams(Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())))
                .setRemoveUpstreams(Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())))
                .setReplaceUpstreams(Collections.emptyList())
                .setAction(Optional.absent())
                .setNoValidate(true)
                .setNoReload(true)
                .setUpstreamUpdateOnly(false)
                .setNoDuplicateUpstreams(false)
                .build(),
            null
        ),
        new QueuedRequestWithState(
            new QueuedRequestId("serviceC", "requestIdC", 0),
            new BaragonRequestBuilder().setLoadBalancerRequestId("requestIdC")
                .setLoadBalancerService(null)
                .setAddUpstreams(Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())))
                .setRemoveUpstreams(Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())))
                .setReplaceUpstreams(Collections.emptyList())
                .setAction(Optional.absent())
                .setNoValidate(true)
                .setNoReload(false)
                .setUpstreamUpdateOnly(false)
                .setNoDuplicateUpstreams(false)
                .build(),
            null
        ),
        new QueuedRequestWithState(
            new QueuedRequestId("serviceC", "requestIdC", 0),
            new BaragonRequestBuilder().setLoadBalancerRequestId("requestIdC")
                .setLoadBalancerService(null)
                .setAddUpstreams(Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())))
                .setRemoveUpstreams(Collections.singletonList(new UpstreamInfo(null, Optional.absent(), Optional.absent())))
                .setReplaceUpstreams(Collections.emptyList())
                .setAction(Optional.absent())
                .setNoValidate(false)
                .setNoReload(true)
                .setUpstreamUpdateOnly(false)
                .setNoDuplicateUpstreams(false)
                .build(),
            null
        )
    );

    List<QueuedRequestWithState> sortedQueuedRequestsWithState = new ArrayList<>(queuedRequestsWithState);
    sortedQueuedRequestsWithState.sort(BaragonRequestWorker.queuedRequestComparator());

    assertEquals(queuedRequestsWithState.get(1), sortedQueuedRequestsWithState.get(0));
    assertEquals(queuedRequestsWithState.get(0), sortedQueuedRequestsWithState.get(3));
  }
}
