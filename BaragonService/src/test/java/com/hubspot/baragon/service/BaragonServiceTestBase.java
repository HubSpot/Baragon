package com.hubspot.baragon.service;

import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonRequestBuilder;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.service.managers.TestAgentManager;
import com.squarespace.jersey2.guice.JerseyGuiceUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.Participant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Timeout(value = 60)
@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class BaragonServiceTestBase {
  private List<LeaderLatch> activeLeaderLatch = new ArrayList<>();

  @Inject
  protected BaragonLoadBalancerDatastore loadBalancerDatastore;

  @Inject
  protected TestAgentManager testAgentManager;

  @BeforeAll
  public void setup() throws Exception {
    JerseyGuiceUtils.reset();
    new BaragonServiceTestModule().getInjector().injectMembers(this);
  }

  @AfterEach
  public void cleanup() {
    activeLeaderLatch.forEach(
      l -> {
        try {
          l.close();
        } catch (Throwable t) {
          throw new RuntimeException(t);
        }
      }
    );
    activeLeaderLatch.clear();
  }

  protected void startAgent(String baseUrl, String group) {
    try {
      BaragonAgentMetadata agentMetadata = new BaragonAgentMetadata(
        baseUrl,
        UUID.randomUUID().toString(),
        Optional.absent(),
        null,
        Optional.absent(),
        null,
        true
      );
      LeaderLatch leaderLatch = loadBalancerDatastore.createLeaderLatch(
        group,
        agentMetadata
      );
      String id = leaderLatch.getId();
      leaderLatch.start();
      activeLeaderLatch.add(leaderLatch);
      while (
        leaderLatch
          .getParticipants()
          .stream()
          .map(Participant::getId)
          .noneMatch(id::equals)
      ) {
        Thread.sleep(5);
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
  }

  protected static BaragonRequestBuilder createBaseRequest(
    String requestId,
    String serviceId,
    Set<String> lbGroups
  ) {
    return createBaseRequest(requestId, serviceId, lbGroups, Collections.emptyMap());
  }

  protected static BaragonRequestBuilder createBaseRequest(
    String requestId,
    String serviceId,
    Set<String> lbGroups,
    Map<String, Object> options
  ) {
    return new BaragonRequestBuilder()
      .setLoadBalancerRequestId(requestId)
      .setLoadBalancerService(
        new BaragonService(
          serviceId,
          Collections.emptySet(),
          serviceId,
          lbGroups,
          options
        )
      )
      .setAddUpstreams(
        Collections.singletonList(
          new UpstreamInfo("localhost:8080", Optional.absent(), Optional.absent())
        )
      )
      .setRemoveUpstreams(
        Collections.singletonList(
          new UpstreamInfo("localhost:8081", Optional.absent(), Optional.absent())
        )
      )
      .setReplaceUpstreams(Collections.emptyList())
      .setAction(Optional.absent())
      .setNoValidate(false)
      .setNoReload(false)
      .setUpstreamUpdateOnly(false)
      .setNoDuplicateUpstreams(false);
  }
}
