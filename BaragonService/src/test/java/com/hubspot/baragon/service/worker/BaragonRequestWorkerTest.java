package com.hubspot.baragon.service.worker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.hubspot.baragon.models.AgentRequestType;
import com.hubspot.baragon.models.BaragonRequestBuilder;
import com.hubspot.baragon.models.InternalRequestStates;
import com.hubspot.baragon.models.QueuedRequestId;
import com.hubspot.baragon.models.QueuedRequestWithState;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.service.BaragonServiceTestBase;
import com.hubspot.baragon.service.managers.RequestManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BaragonRequestWorkerTest extends BaragonServiceTestBase {
  private static final String TEST_LB_GROUP = "test";

  @Inject
  RequestManager requestManager;

  @Inject
  BaragonRequestWorker requestWorker;

  @BeforeEach
  public void beforeEach() {
    // need to clean up requestManager so that the requests to don't persist
    // between test runs
    requestManager.deleteRequest("request1");
    requestManager.deleteRequest("request2");
  }

  @Test
  public void testQueuedRequestsAreBatchedForAgent() throws Exception {
    String agentUrl = "http://agent1";
    startAgent(agentUrl, TEST_LB_GROUP);
    requestManager.enqueueRequest(
      createBaseRequest("request1", "service1", ImmutableSet.of(TEST_LB_GROUP)).build()
    );
    requestManager.enqueueRequest(
      createBaseRequest("request2", "service2", ImmutableSet.of(TEST_LB_GROUP)).build()
    );

    requestWorker.run(); // move from pending -> send apply
    requestWorker.run(); // actually send
    Assertions.assertEquals(2, testAgentManager.getRecentBatches().get(agentUrl).size());
  }

  @Test
  public void testServiceBatchBoundaryIsRespected() throws Exception {
    String agentUrl = "http://agent2";
    startAgent(agentUrl, TEST_LB_GROUP);
    requestManager.enqueueRequest(
      createBaseRequest("request1", "service1", ImmutableSet.of(TEST_LB_GROUP)).build()
    );
    requestManager.enqueueRequest(
      createBaseRequest(
          "request2",
          "service1",
          ImmutableSet.of(TEST_LB_GROUP),
          ImmutableMap.of("1", "2")
        )
        .build()
    );

    requestWorker.run(); // move from pending -> send apply
    requestWorker.run(); // actually send
    Assertions.assertEquals(1, testAgentManager.getRecentBatches().get(agentUrl).size());
  }

  @Test
  public void testInFlightRequestsAreRespected() throws Exception {
    String agentUrl = "http://agent2";
    startAgent(agentUrl, TEST_LB_GROUP);
    requestManager.enqueueRequest(
      createBaseRequest("request1", "service1", ImmutableSet.of(TEST_LB_GROUP)).build()
    );
    requestWorker.run(); // move from pending -> send apply
    requestWorker.run(); // actually send
    Assertions.assertEquals(1, testAgentManager.getRecentBatches().get(agentUrl).size());

    requestManager.enqueueRequest(
      createBaseRequest("request2", "service1", ImmutableSet.of(TEST_LB_GROUP)).build()
    );
    Assertions.assertEquals(1, testAgentManager.getRecentBatches().get(agentUrl).size());
    Assertions.assertEquals(
      "request1",
      testAgentManager.getRecentBatches().get(agentUrl).get(0).getRequestId()
    );
  }

  @Test
  public void testFailedRetriedRequestsAreConsideredInFlight() throws Exception {
    String agentUrl = "http://agent2";
    startAgent(agentUrl, TEST_LB_GROUP);
    requestManager.enqueueRequest(
      createBaseRequest("request1", "service1", ImmutableSet.of(TEST_LB_GROUP)).build()
    );
    requestWorker.run(); // move from pending -> send apply
    requestWorker.run(); // actually send
    Assertions.assertEquals(1, testAgentManager.getRecentBatches().get(agentUrl).size());
    testAgentManager.completeRequestWithFailure(
      agentUrl,
      agentUrl,
      "request1",
      AgentRequestType.APPLY
    );
    requestWorker.run(); // see the failed response, move to retry
    Assertions.assertEquals(
      InternalRequestStates.SEND_APPLY_REQUESTS,
      requestManager.getRequestState("request1").get()
    );

    // second request for same service should not send a request batch
    requestManager.enqueueRequest(
      createBaseRequest("request2", "service1", ImmutableSet.of(TEST_LB_GROUP)).build()
    );
    Assertions.assertEquals(1, testAgentManager.getRecentBatches().get(agentUrl).size());
    Assertions.assertEquals(
      "request1",
      testAgentManager.getRecentBatches().get(agentUrl).get(0).getRequestId()
    );
  }

  @Test
  public void testQueuedRequestComparator() {
    List<QueuedRequestWithState> queuedRequestsWithState = Arrays.asList(
      new QueuedRequestWithState(
        new QueuedRequestId("serviceA", "requestIdA", 0),
        new BaragonRequestBuilder()
          .setLoadBalancerRequestId("requestIdA")
          .setLoadBalancerService(null)
          .setAddUpstreams(
            Collections.singletonList(
              new UpstreamInfo(null, Optional.absent(), Optional.absent())
            )
          )
          .setRemoveUpstreams(
            Collections.singletonList(
              new UpstreamInfo(null, Optional.absent(), Optional.absent())
            )
          )
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
        new BaragonRequestBuilder()
          .setLoadBalancerRequestId("requestIdB")
          .setLoadBalancerService(null)
          .setAddUpstreams(
            Collections.singletonList(
              new UpstreamInfo(null, Optional.absent(), Optional.absent())
            )
          )
          .setRemoveUpstreams(
            Collections.singletonList(
              new UpstreamInfo(null, Optional.absent(), Optional.absent())
            )
          )
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
        new BaragonRequestBuilder()
          .setLoadBalancerRequestId("requestIdC")
          .setLoadBalancerService(null)
          .setAddUpstreams(
            Collections.singletonList(
              new UpstreamInfo(null, Optional.absent(), Optional.absent())
            )
          )
          .setRemoveUpstreams(
            Collections.singletonList(
              new UpstreamInfo(null, Optional.absent(), Optional.absent())
            )
          )
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
        new BaragonRequestBuilder()
          .setLoadBalancerRequestId("requestIdC")
          .setLoadBalancerService(null)
          .setAddUpstreams(
            Collections.singletonList(
              new UpstreamInfo(null, Optional.absent(), Optional.absent())
            )
          )
          .setRemoveUpstreams(
            Collections.singletonList(
              new UpstreamInfo(null, Optional.absent(), Optional.absent())
            )
          )
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

    List<QueuedRequestWithState> sortedQueuedRequestsWithState = new ArrayList<>(
      queuedRequestsWithState
    );
    sortedQueuedRequestsWithState.sort(BaragonRequestWorker.queuedRequestComparator());

    assertEquals(queuedRequestsWithState.get(1), sortedQueuedRequestsWithState.get(0));
    assertEquals(queuedRequestsWithState.get(0), sortedQueuedRequestsWithState.get(3));
  }
}
