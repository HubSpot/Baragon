package com.hubspot.baragon;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Scopes;
import com.hubspot.baragon.data.BaragonLoadBalancerDatastore;
import com.hubspot.baragon.data.BaragonStateDatastore;
import com.hubspot.baragon.managers.RequestManager;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.UpstreamInfo;
import com.hubspot.baragon.worker.BaragonRequestWorker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(JukitoRunner.class)
public class RequestTests {
  private static final Logger LOG = LoggerFactory.getLogger(RequestTests.class);

  public static final String REAL_LB_GROUP = "real";

  public static class Module extends JukitoModule {
    @Override
    protected void configureTest() {
      install(new BaragonDataTestModule());
      bindMock(BaragonLoadBalancerDatastore.class).in(Scopes.SINGLETON);
    }
  }

  @Before
  public void setupMocks(BaragonLoadBalancerDatastore loadBalancerDatastore) {
    when(loadBalancerDatastore.getClusters()).thenReturn(Collections.singleton(REAL_LB_GROUP));
    when(loadBalancerDatastore.getBasePaths(anyString())).thenReturn(Collections.<String>emptyList());
    when(loadBalancerDatastore.getBasePathServiceId(anyString(), anyString())).thenReturn(Optional.<String>absent());
  }

  @Test
  public void removeNonExistentUpstream(RequestManager requestManager, BaragonRequestWorker requestWorker) {
    final String requestId = "test-125";
    final BaragonService service = new BaragonService("testservice", Collections.<String>emptyList(), "/test", ImmutableList.of(REAL_LB_GROUP), Collections.<String, Object>emptyMap());

    final UpstreamInfo fakeUpstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = new BaragonRequest(requestId, service, Collections.<UpstreamInfo>emptyList(), ImmutableList.of(fakeUpstream));

    try {
      LOG.info("Going to enqueue request: {}", request);
      final BaragonResponse response = requestManager.enqueueRequest(request);

      assertEquals(BaragonRequestState.WAITING, response.getLoadBalancerState());
      requestWorker.run();

      assertEquals(BaragonRequestState.WAITING, response.getLoadBalancerState());
      requestWorker.run();

      final Optional<BaragonResponse> maybeNewResponse = requestManager.getResponse(requestId);
      LOG.info("Got response: {}", maybeNewResponse);
      
      assertTrue(maybeNewResponse.isPresent());
      assertEquals(maybeNewResponse.get().getLoadBalancerState(), BaragonRequestState.SUCCESS);
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  @Test
  public void addHttpUrlUpstream(RequestManager requestManager, BaragonRequestWorker requestWorker, BaragonStateDatastore stateDatastore) {
    final String requestId = "test-http-url-upstream-1";
    final String serviceId = "httpUrlUpstreamService";

    final BaragonService service = new BaragonService(serviceId, Collections.<String>emptyList(), "/http-url-upstream", ImmutableList.of(REAL_LB_GROUP), Collections.<String, Object>emptyMap());

    final UpstreamInfo httpUrlUpstream = new UpstreamInfo("http://test.com:8080/foo", Optional.of(requestId), Optional.<String>absent());

    final BaragonRequest request = new BaragonRequest(requestId, service, ImmutableList.of(httpUrlUpstream), Collections.<UpstreamInfo>emptyList());

    try {
      LOG.info("Going to enqueue request: {}", request);
      final BaragonResponse response = requestManager.enqueueRequest(request);

      assertEquals(BaragonRequestState.WAITING, response.getLoadBalancerState());
      requestWorker.run();

      assertEquals(BaragonRequestState.WAITING, response.getLoadBalancerState());
      requestWorker.run();

      final Optional<BaragonResponse> maybeNewResponse = requestManager.getResponse(requestId);
      LOG.info("Got response: {}", maybeNewResponse);

      assertTrue(maybeNewResponse.isPresent());
      assertEquals(BaragonRequestState.SUCCESS, maybeNewResponse.get().getLoadBalancerState());

      assertEquals(ImmutableSet.of(httpUrlUpstream.getUpstream()), stateDatastore.getUpstreamsMap(serviceId).keySet());
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
