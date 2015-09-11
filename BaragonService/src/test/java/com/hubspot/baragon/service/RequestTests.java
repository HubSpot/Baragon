package com.hubspot.baragon.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hubspot.baragon.BaragonServiceTestModule;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.exceptions.InvalidRequestActionException;
import com.hubspot.baragon.exceptions.InvalidUpstreamsException;
import com.hubspot.baragon.exceptions.RequestAlreadyEnqueuedException;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.service.managers.RequestManager;
import com.hubspot.baragon.service.worker.BaragonRequestWorker;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JukitoRunner.class)
public class RequestTests {
  private static final Logger LOG = LoggerFactory.getLogger(RequestTests.class);

  public static final String REAL_LB_GROUP = "real";
  public static final String FAKE_LB_GROUP = "fake";

  public static class Module extends JukitoModule {
    @Override
    protected void configureTest() {
      install(new BaragonServiceTestModule());
    }
  }

  @Before
  public void setupLbGroups(BaragonLoadBalancerTestDatastore loadBalancerDatastore) {
    loadBalancerDatastore.setLoadBalancerGroupsOverride(Optional.of(Collections.singleton(REAL_LB_GROUP)));
  }

  @After
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

    assertTrue(String.format("Response for request %s exists", requestId), maybeResponse.isPresent());
    assertEquals(expected, maybeResponse.get().getLoadBalancerState());

    return maybeResponse;
  }

  private Optional<BaragonResponse> assertResponseStateAbsent(RequestManager requestManager, String requestId) {
    final Optional<BaragonResponse> maybeResponse = requestManager.getResponse(requestId);

    assertTrue(String.format("Response for request %s does not exist", requestId), !maybeResponse.isPresent());

    return maybeResponse;
  }

  private void assertSuccessfulRequestLifecycle(RequestManager requestManager, BaragonRequestWorker requestWorker, String requestId) {
    assertResponseStateExists(requestManager, requestId, BaragonRequestState.WAITING);  // PENDING

    requestWorker.run();

    assertResponseStateExists(requestManager, requestId, BaragonRequestState.WAITING);  // SEND REQUESTS

    requestWorker.run();

    assertResponseStateExists(requestManager, requestId, BaragonRequestState.WAITING);  // CHECK REQUESTS

    requestWorker.run();

    assertResponseStateExists(requestManager, requestId, BaragonRequestState.SUCCESS);  // SUCCESS
  }

  @Test
  public void removeNonExistentUpstream(RequestManager requestManager, BaragonRequestWorker requestWorker) {
    final String requestId = "test-125";
    Set<String> lbGroup = new HashSet<>();
    lbGroup.add(REAL_LB_GROUP);
    final BaragonService service = new BaragonService("testservice", Collections.<String>emptyList(), "/test", lbGroup, Collections.<String, Object>emptyMap());

    final UpstreamInfo fakeUpstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = new BaragonRequest(requestId, service, Collections.<UpstreamInfo>emptyList(), ImmutableList.of(fakeUpstream), Optional.<String>absent());

    Optional<BaragonResponse> maybeResponse;

    try {
      assertResponseStateAbsent(requestManager, requestId);

      LOG.info("Going to enqueue request: {}", request);
      requestManager.enqueueRequest(request);

      assertSuccessfulRequestLifecycle(requestManager, requestWorker, requestId);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Test
  public void addHttpUrlUpstream(RequestManager requestManager, BaragonRequestWorker requestWorker, BaragonStateDatastore stateDatastore) {
    final String requestId = "test-http-url-upstream-1";
    final String serviceId = "httpUrlUpstreamService";
    Set<String> lbGroup = new HashSet<>();
    lbGroup.add(REAL_LB_GROUP);

    final BaragonService service = new BaragonService(serviceId, Collections.<String>emptyList(), "/http-url-upstream",lbGroup, Collections.<String, Object>emptyMap());

    final UpstreamInfo httpUrlUpstream = new UpstreamInfo("http://test.com:8080/foo", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = new BaragonRequest(requestId, service, ImmutableList.of(httpUrlUpstream), Collections.<UpstreamInfo>emptyList(), Optional.<String>absent());

    try {
      assertResponseStateAbsent(requestManager, requestId);

      LOG.info("Going to enqueue request: {}", request);
      requestManager.enqueueRequest(request);

      assertSuccessfulRequestLifecycle(requestManager, requestWorker, requestId);

      assertEquals(ImmutableSet.of(httpUrlUpstream.getUpstream()), stateDatastore.getUpstreamsMap(serviceId).keySet());
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Test
  public void testNonExistentLoadBalancerGroup(RequestManager requestManager, BaragonRequestWorker requestWorker) {
    final String requestId = "test-126";
    Set<String> lbGroup = new HashSet<>();
    lbGroup.add(FAKE_LB_GROUP);
    final BaragonService service = new BaragonService("testservice", Collections.<String>emptyList(), "/test", lbGroup, Collections.<String, Object>emptyMap());

    final UpstreamInfo upstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = new BaragonRequest(requestId, service, ImmutableList.of(upstream), ImmutableList.<UpstreamInfo>of(), Optional.<String>absent());

    try {
      assertResponseStateAbsent(requestManager, requestId);

      LOG.info("Going to enqueue request: {}", request);
      requestManager.enqueueRequest(request);

      assertResponseStateExists(requestManager, requestId, BaragonRequestState.WAITING);

      requestWorker.run();

      assertResponseStateExists(requestManager, requestId, BaragonRequestState.FAILED);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Test(expected = RequestAlreadyEnqueuedException.class)
  public void testPreexistingResponse(RequestManager requestManager) throws RequestAlreadyEnqueuedException, InvalidRequestActionException, InvalidUpstreamsException {
    final String requestId = "test-127";
    Set<String> lbGroup = new HashSet<>();
    lbGroup.add(FAKE_LB_GROUP);
    final BaragonService service = new BaragonService("testservice", Collections.<String>emptyList(), "/test", lbGroup, Collections.<String, Object>emptyMap());

    final UpstreamInfo upstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = new BaragonRequest(requestId, service, ImmutableList.of(upstream), ImmutableList.<UpstreamInfo>of(), Optional.<String>absent());

    requestManager.enqueueRequest(request);
    requestManager.enqueueRequest(request);
  }

  @Test
  public void testBasePathConflicts(RequestManager requestManager, BaragonRequestWorker requestWorker, BaragonLoadBalancerDatastore loadBalancerDatastore) {
    loadBalancerDatastore.setBasePathServiceId(REAL_LB_GROUP, "/foo", "foo-service");
    final String requestId = "test-128";
    Set<String> lbGroup = new HashSet<>();
    lbGroup.add(REAL_LB_GROUP);

    final BaragonService service = new BaragonService("testservice", Collections.<String>emptyList(), "/foo", lbGroup, Collections.<String, Object>emptyMap());

    final UpstreamInfo upstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = new BaragonRequest(requestId, service, ImmutableList.of(upstream), ImmutableList.<UpstreamInfo>of(), Optional.<String>absent());

    try {
      assertResponseStateAbsent(requestManager, requestId);

      LOG.info("Going to enqueue request: {}", request);
      requestManager.enqueueRequest(request);

      assertResponseStateExists(requestManager, requestId, BaragonRequestState.WAITING);

      requestWorker.run();

      assertResponseStateExists(requestManager, requestId, BaragonRequestState.FAILED);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
