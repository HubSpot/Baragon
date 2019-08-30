package com.hubspot.baragon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.hubspot.baragon.BaragonServiceTestModule;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.exceptions.InvalidRequestActionException;
import com.hubspot.baragon.exceptions.InvalidUpstreamsException;
import com.hubspot.baragon.exceptions.RequestAlreadyEnqueuedException;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestBuilder;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceBuilder;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.service.managers.RequestManager;
import com.hubspot.baragon.service.worker.BaragonRequestWorker;

import name.falgout.jeffrey.testing.junit.guice.GuiceExtension;
import name.falgout.jeffrey.testing.junit.guice.IncludeModule;

@ExtendWith(GuiceExtension.class)
@IncludeModule(BaragonServiceTestModule.class)
public class RequestTest {
  private static final Logger LOG = LoggerFactory.getLogger(RequestTest.class);

  public static final String REAL_LB_GROUP = "real";
  public static final String FAKE_LB_GROUP = "fake";

  @BeforeEach
  public void setupLbGroups(BaragonLoadBalancerTestDatastore loadBalancerDatastore) {
    loadBalancerDatastore.setLoadBalancerGroupsOverride(Optional.of(Collections.singleton(REAL_LB_GROUP)));
    BaragonAgentMetadata agent1 = BaragonAgentMetadata.fromString("http://127.0.0.1:8080/baragon-agent/v2");
    Collection<BaragonAgentMetadata> agents = new ArrayList<>();
    agents.add(agent1);
    loadBalancerDatastore.setLoadBalancerAgentsOverride(Optional.of(agents));
  }

  @AfterEach
  public void clearBasePaths(BaragonLoadBalancerDatastore loadBalancerDatastore) {
    LOG.debug("Clearing base paths...");
    for (String loadBalancerGroup : loadBalancerDatastore.getLoadBalancerGroupNames()) {
      for (String basePath : loadBalancerDatastore.getBasePaths(loadBalancerGroup)) {
        LOG.debug(String.format("  Clearing %s on %s", basePath, loadBalancerGroup));
        loadBalancerDatastore.clearBasePath(loadBalancerGroup, basePath);
      }
    }
  }

  private Optional<BaragonResponse> assertResponseStateExists(RequestManager requestManager, String requestId, BaragonRequestState expected) {
    final Optional<BaragonResponse> maybeResponse = requestManager.getResponse(requestId);

    assertTrue(maybeResponse.isPresent(), String.format("Response for request %s exists", requestId));
    assertEquals(expected, maybeResponse.get().getLoadBalancerState());

    return maybeResponse;
  }

  private Optional<BaragonResponse> assertResponseStateAbsent(RequestManager requestManager, String requestId) {
    final Optional<BaragonResponse> maybeResponse = requestManager.getResponse(requestId);

    assertTrue(!maybeResponse.isPresent(), String.format("Response for request %s does not exist", requestId));

    return maybeResponse;
  }

  @Test
  public void testNonExistentLoadBalancerGroup(RequestManager requestManager, BaragonRequestWorker requestWorker) {
    final String requestId = "test-126";
    Set<String> lbGroup = new HashSet<>();
    lbGroup.add(FAKE_LB_GROUP);
    final BaragonService service = new BaragonServiceBuilder().setServiceId("testservice1")
        .setOwners(Collections.<String>emptyList())
        .setServiceBasePath("/test")
        .setLoadBalancerGroups(lbGroup)
        .setOptions(Collections.<String, Object>emptyMap())
        .build();

    final UpstreamInfo upstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = new BaragonRequestBuilder().setLoadBalancerRequestId(requestId)
        .setLoadBalancerService(service)
        .setAddUpstreams(ImmutableList.of(upstream))
        .setRemoveUpstreams(ImmutableList.<UpstreamInfo>of())
        .build();

    try {
      assertResponseStateAbsent(requestManager, requestId);

      LOG.info("Going to enqueue request: {}", request);
      requestManager.enqueueRequest(request);

      assertResponseStateExists(requestManager, requestId, BaragonRequestState.WAITING);

      requestWorker.run();

      assertResponseStateExists(requestManager, requestId, BaragonRequestState.INVALID_REQUEST_NOOP);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Test
  public void testPreexistingResponse(RequestManager requestManager) throws RequestAlreadyEnqueuedException, InvalidRequestActionException, InvalidUpstreamsException {
    final String requestId = "test-127";
    Set<String> lbGroup = new HashSet<>();
    lbGroup.add(FAKE_LB_GROUP);
    final BaragonService service = new BaragonServiceBuilder().setServiceId("testservice2")
        .setOwners(Collections.<String>emptyList())
        .setServiceBasePath("/test")
        .setLoadBalancerGroups(lbGroup)
        .setOptions(Collections.<String, Object>emptyMap())
        .build();

    final UpstreamInfo upstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = new BaragonRequestBuilder().setLoadBalancerRequestId(requestId)
        .setLoadBalancerService(service)
        .setAddUpstreams(ImmutableList.of(upstream))
        .setRemoveUpstreams(ImmutableList.<UpstreamInfo>of())
        .build();

    BaragonResponse response = requestManager.enqueueRequest(request);
    BaragonResponse repeatResponse = requestManager.enqueueRequest(request);
    assertEquals(repeatResponse, response);
  }

  @Test
  public void testBasePathConflicts(RequestManager requestManager, BaragonRequestWorker requestWorker, BaragonLoadBalancerDatastore loadBalancerDatastore) {
    loadBalancerDatastore.setBasePathServiceId(REAL_LB_GROUP, "/foo", "foo-service");
    final String requestId = "test-128";
    Set<String> lbGroup = new HashSet<>();
    lbGroup.add(REAL_LB_GROUP);

    final BaragonService service = new BaragonServiceBuilder().setServiceId("testservice3")
        .setOwners(Collections.<String>emptyList())
        .setServiceBasePath("/foo")
        .setLoadBalancerGroups(lbGroup)
        .setOptions(Collections.<String, Object>emptyMap())
        .build();

    final UpstreamInfo upstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = new BaragonRequestBuilder().setLoadBalancerRequestId(requestId)
        .setLoadBalancerService(service)
        .setAddUpstreams(ImmutableList.of(upstream))
        .setRemoveUpstreams(ImmutableList.<UpstreamInfo>of())
        .build();

    try {
      assertResponseStateAbsent(requestManager, requestId);

      LOG.info("Going to enqueue request: {}", request);
      requestManager.enqueueRequest(request);

      assertResponseStateExists(requestManager, requestId, BaragonRequestState.WAITING);

      requestWorker.run();

      assertResponseStateExists(requestManager, requestId, BaragonRequestState.INVALID_REQUEST_NOOP);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Test
  public void testAdditionalPathConflicts(RequestManager requestManager, BaragonRequestWorker requestWorker, BaragonLoadBalancerDatastore loadBalancerDatastore) {
    loadBalancerDatastore.setBasePathServiceId(REAL_LB_GROUP, "/some-other-path", "foo-service");
    final String requestId = "test-129";
    Set<String> lbGroup = new HashSet<>();
    lbGroup.add(REAL_LB_GROUP);

    final BaragonService service = new BaragonServiceBuilder().setServiceId("testservice4")
        .setOwners(Collections.<String>emptyList())
        .setServiceBasePath("/foo")
        .setAdditionalPaths(Collections.singletonList("/some-other-path"))
        .setLoadBalancerGroups(lbGroup)
        .setOptions(Collections.<String, Object>emptyMap())
        .setTemplateName(Optional.<String>absent())
        .setDomains(Collections.<String>emptySet())
        .setEdgeCacheDNS(Optional.absent())
        .build();

    final UpstreamInfo upstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = new BaragonRequestBuilder().setLoadBalancerRequestId(requestId)
        .setLoadBalancerService(service)
        .setAddUpstreams(ImmutableList.of(upstream))
        .setRemoveUpstreams(ImmutableList.<UpstreamInfo>of())
        .build();

    try {
      assertResponseStateAbsent(requestManager, requestId);

      LOG.info("Going to enqueue request: {}", request);
      requestManager.enqueueRequest(request);

      assertResponseStateExists(requestManager, requestId, BaragonRequestState.WAITING);

      requestWorker.run();

      assertResponseStateExists(requestManager, requestId, BaragonRequestState.INVALID_REQUEST_NOOP);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
