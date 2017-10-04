package com.hubspot.baragon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Stopwatch;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hubspot.baragon.client.BaragonServiceClient;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonRequest;
import com.hubspot.baragon.models.BaragonRequestState;
import com.hubspot.baragon.models.BaragonResponse;
import com.hubspot.baragon.models.BaragonService;
import com.hubspot.baragon.models.BaragonServiceState;
import com.hubspot.baragon.models.BaragonServiceStatus;
import com.hubspot.baragon.models.UpstreamInfo;

public class BaragonServiceTestIT {
  
  public static final String LOAD_BALANCER_GROUP = "docker-baragon";
  public static final String UPSTREAM = "example.com:80";
  public static final String SERVICE_BASE_PATH = "/testservice";
  public static final int MAX_REQUEST_RETRIES = 15;
  public static final List<BaragonRequestState> TERMINAL_STATES = (Arrays.asList(BaragonRequestState.SUCCESS, BaragonRequestState.INVALID_REQUEST_NOOP, BaragonRequestState.CANCELED));
	
  private BaragonServiceClient baragonServiceClient;
  
  private static final Logger LOG = LoggerFactory.getLogger(BaragonServiceTestIT.class);

  // Helpers --------------------------------------------
  
  private Injector getInjector() {
    return Guice.createInjector(new DockerTestModule());
  }
  
  private String setupTestService() {
    String requestId = "Service" + new Date().getTime();
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Collections.<String>emptyList(), Optional.<String>absent(), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
    
    return requestId;
  }
  
  private void setupTestService(String requestId, String serviceId, String basePath, List<String> additionalPaths, Optional<String> replaceServiceId, Optional<Map<String, Object>> options, Optional<String> template) {
    UpstreamInfo upstream = new UpstreamInfo(UPSTREAM, Optional.<String>absent(), Optional.<String>absent());
    BaragonService lbService = new BaragonService(serviceId, new ArrayList<String>(), basePath, additionalPaths,
        new HashSet<String>(Arrays.asList(LOAD_BALANCER_GROUP)), options.isPresent() ? options.get() : new HashMap<String, Object>(), template, Collections.<String>emptySet(), Optional.absent());
    
    BaragonRequest request = new BaragonRequest(requestId, lbService, Arrays.asList(upstream), new ArrayList<UpstreamInfo>(), replaceServiceId);
    baragonServiceClient.enqueueRequest(request);
  }
  
  private void removeService(String serviceId) throws Exception {
    LOG.info("Cleaning up...");
    
    if(!baragonServiceClient.getServiceState(serviceId).isPresent())
      return;

    BaragonResponse resp = baragonServiceClient.deleteService(serviceId).get();
    waitForStatus(resp.getLoadBalancerRequestId(), BaragonRequestState.SUCCESS);
    
    assertFalse(baragonServiceClient.getServiceState(serviceId).isPresent());
  }
  
  public void waitForStatus(String requestId, BaragonRequestState targetState) throws Exception {   
    LOG.info("Waiting...");
    
    int retries = 1;
    for(BaragonRequestState state = baragonServiceClient.getRequest(requestId).get().getLoadBalancerState(); 
        !state.equals(targetState)
        && !TERMINAL_STATES.contains(state)
        && retries <= MAX_REQUEST_RETRIES; 
        retries++) {
      
      Thread.sleep((long) (1000 + (Math.pow(1.8, retries) * 10)));
      
      state = baragonServiceClient.getRequest(requestId).get().getLoadBalancerState(); 
    }
    BaragonResponse resp = baragonServiceClient.getRequest(requestId).get();
    if (!resp.getLoadBalancerState().equals(targetState)) {
      throw new Exception("Request " + requestId + " stuck while waiting to reach " + targetState + ": \n" +
          resp.getLoadBalancerState() + " - " + resp.getMessage());
    }
  }
  
  // ----------------------------------------------------
  
  @Before
  public void setup() throws Exception {
    baragonServiceClient = getInjector().getInstance(BaragonServiceClient.class);
  }
  
  @Rule
  public TestRule watcher = new TestWatcher() {
     protected void starting(Description description) {
        LOG.info("\nStarting: " + description.getMethodName());
     }
     protected void succeeded(Description description) {
       LOG.info("\u001B[32mTest passed\u001B[0m");
     }
     protected void failed(Throwable e, Description description) {
       LOG.info("\u001B[31mTest failed\u001B[0m");
     }
  };
  
  @Rule
  public Stopwatch stopwatch = new Stopwatch() {
      @Override
      protected void finished(long nanos, Description description) {
        LOG.info("(" + nanos / 1000000000.0 + " s)");
      }
  };
  
  @After
  public void teardown() throws Exception {
    for(BaragonServiceState state :baragonServiceClient.getGlobalState()) {
      removeService(state.getService().getServiceId());
    }
    baragonServiceClient = null;
  }

  // ----------------------------------------------------
  
  @Test
  public void testStatus() throws Exception {
    Optional<BaragonServiceStatus> status = baragonServiceClient.getAnyBaragonServiceStatus();
    assertTrue(status.get().isLeader());
  }

  @Test
  public void testWorkers() throws Exception {
    assertFalse(baragonServiceClient.getBaragonServiceWorkers().isEmpty());
  }
  
  @Test
  public void testLoadBalancers() throws Exception {
    assertFalse(baragonServiceClient.getLoadBalancerGroups().isEmpty());
  }
  
  @Test
  public void testClusterAgents() throws Exception {
    BaragonAgentMetadata[] metadata = baragonServiceClient.getLoadBalancerGroupAgentMetadata(LOAD_BALANCER_GROUP).toArray(new BaragonAgentMetadata[0]);
    for(BaragonAgentMetadata md : metadata)
      assertTrue(md.getBaseAgentUri() != null && !md.getBaseAgentUri().isEmpty());
  }
  
  @Test
  public void testClusterKnownAgents() throws Exception {
    BaragonAgentMetadata metadata = baragonServiceClient.getLoadBalancerGroupKnownAgentMetadata(LOAD_BALANCER_GROUP).toArray(new BaragonAgentMetadata[0])[0];
    assertTrue(metadata.getBaseAgentUri() != null && !metadata.getBaseAgentUri().isEmpty());
    
    testDeleteKnownAgent();
  }
  
   // ----------------------------------------------------
  
  @Test
  public void testValidRequest() throws Exception {
    String serviceId = setupTestService();
    waitForStatus(serviceId, BaragonRequestState.SUCCESS);
    
    testGetService(serviceId);
    testState(serviceId);
    testClusterBasePaths(serviceId);
  }
  
  public void testClusterBasePaths(String serviceId) {
    Collection<String> basePaths = baragonServiceClient.getOccupiedBasePaths(LOAD_BALANCER_GROUP);
    assertTrue(basePaths.size() > 0);
    Optional<BaragonService> service = baragonServiceClient.getServiceForBasePath(LOAD_BALANCER_GROUP, basePaths.iterator().next());
    assertTrue(service.isPresent());
    assertEquals(service.get().getServiceId(), serviceId);
  }
  
  public void testGetService(String serviceId) {
    Optional<BaragonService> service = baragonServiceClient.getServiceForBasePath(LOAD_BALANCER_GROUP, SERVICE_BASE_PATH);
    assertTrue(service.isPresent());
    assertEquals(service.get().getServiceId(), serviceId);
  }
  
  public void testState(String serviceId) {
    Optional<BaragonServiceState> serviceState = baragonServiceClient.getServiceState(serviceId);
    assertTrue(serviceState.isPresent());
    assertEquals(serviceState.get().getService().getServiceBasePath(), SERVICE_BASE_PATH);
  }
  
  // ----------------------------------------------------
  
  @Test
  public void testValidPathChange() throws Exception {
    String requestId = setupTestService();
    waitForStatus(requestId, BaragonRequestState.SUCCESS);
      
    setupTestService(requestId + "-2", requestId, SERVICE_BASE_PATH + "-2", Collections.<String>emptyList(), Optional.of(requestId), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
    waitForStatus(requestId + "-2", BaragonRequestState.SUCCESS);
    assertTrue(baragonServiceClient.getOccupiedBasePaths(LOAD_BALANCER_GROUP).contains(SERVICE_BASE_PATH + "-2"));
  }
  
  @Test
  public void testValidRequestNonexistantReplaceId() throws Exception {
    String requestId = "Service" + new Date().getTime();
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Collections.<String>emptyList(), Optional.of("someotherservice"), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
    waitForStatus(requestId, BaragonRequestState.SUCCESS);
  }
  
  @Test
  public void testBasePathConflict() throws Exception {
    String serviceId = setupTestService();
    waitForStatus(serviceId, BaragonRequestState.SUCCESS);
    
    String serviceId2 = setupTestService();
    waitForStatus(serviceId2, BaragonRequestState.INVALID_REQUEST_NOOP);
  }
  
  @Test
  public void testBasePathConflictInvalidReplaceId() throws Exception {
    String serviceId = setupTestService();
    waitForStatus(serviceId, BaragonRequestState.SUCCESS);
    
    String serviceId2 = serviceId + "-2";
    setupTestService(serviceId2, serviceId2, SERVICE_BASE_PATH, Collections.<String>emptyList(), Optional.of("someotherservice"), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
    waitForStatus(serviceId2, BaragonRequestState.INVALID_REQUEST_NOOP);
  }

  @Test
  public void testAdditionalPathConflict() throws Exception {
    String serviceId = setupTestService();
    waitForStatus(serviceId, BaragonRequestState.SUCCESS);

    String serviceId2 = serviceId + "-2";
    setupTestService(serviceId2, serviceId2, "/some-other-path", Collections.singletonList(SERVICE_BASE_PATH), Optional.of("someotherservice"), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
    waitForStatus(serviceId2, BaragonRequestState.INVALID_REQUEST_NOOP);
  }

  @Test
  public void testValidBasePathRename() throws Exception {
    String serviceId = setupTestService();
    waitForStatus(serviceId, BaragonRequestState.SUCCESS);
      
    String renameId = serviceId + "-2";
    setupTestService(renameId, renameId, SERVICE_BASE_PATH + "-2", Collections.<String>emptyList(), Optional.of(serviceId), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
    waitForStatus(renameId, BaragonRequestState.SUCCESS);
    assertTrue(baragonServiceClient.getOccupiedBasePaths(LOAD_BALANCER_GROUP).iterator().next().equals(SERVICE_BASE_PATH + "-2"));
  }
  
  @Test
  public void testValidBasePathServiceIdChange() throws Exception {
    String serviceId = setupTestService();
    waitForStatus(serviceId, BaragonRequestState.SUCCESS);
    
    String renameId = serviceId + "-2";
    setupTestService(renameId, renameId, SERVICE_BASE_PATH, Collections.<String>emptyList(), Optional.of(serviceId), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
    waitForStatus(renameId, BaragonRequestState.SUCCESS);
    
    assertTrue(baragonServiceClient.getServiceState(renameId).get().getService().getServiceBasePath().equals(SERVICE_BASE_PATH));
  }

  @Test
  public void testInvalidRequest() throws Exception {
    String requestId = "Service" + new Date().getTime();
    HashMap<String, Object> options = new HashMap<String, Object>();
    options.put("nginxExtraConfigs", new String[] {"rewrite /this_is_invalid_yo"});
    
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Collections.<String>emptyList(), Optional.<String>absent(), Optional.<Map<String, Object>>of(options), Optional.<String>absent());
    waitForStatus(requestId, BaragonRequestState.FAILED);
    
    BaragonResponse response = baragonServiceClient.getRequest(requestId).get();
    assertEquals(response.getLoadBalancerState(), BaragonRequestState.FAILED);
    assertEquals(response.getAgentResponses().get().get("REVERT").iterator().next().getStatusCode().get(), new Integer(200));
  }

  @Test
  public void testInvalidRequestNonexistantReplaceId() throws Exception {
    String requestId = "Service" + new Date().getTime();
    HashMap<String, Object> options = new HashMap<String, Object>();
    options.put("nginxExtraConfigs", new String[] {"rewrite /this_is_invalid_yo"});
    
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Collections.<String>emptyList(), Optional.of("someotherservice"), Optional.<Map<String, Object>>of(options), Optional.<String>absent());
    waitForStatus(requestId, BaragonRequestState.FAILED);
    
    BaragonResponse response = baragonServiceClient.getRequest(requestId).get();
    assertEquals(response.getLoadBalancerState(), BaragonRequestState.FAILED);
    assertEquals(response.getAgentResponses().get().get("REVERT").iterator().next().getStatusCode().get(), new Integer(200));
  }

  @Test
  public void testInvalidRequestWithBasePathChange() throws Exception {
    String requestId = setupTestService();
    waitForStatus(requestId, BaragonRequestState.SUCCESS);
    
    String requestId2 = "Service" + new Date().getTime();
    HashMap<String, Object> options = new HashMap<String, Object>();
    options.put("nginxExtraConfigs", new String[] {"rewrite /this_is_invalid_yo"});
    
    setupTestService(requestId2, requestId2, SERVICE_BASE_PATH, Collections.<String>emptyList(), Optional.of(requestId), Optional.<Map<String, Object>>of(options), Optional.<String>absent());
    waitForStatus(requestId2, BaragonRequestState.FAILED);
    
    BaragonResponse response2 = baragonServiceClient.getRequest(requestId2).get();
    assertEquals(response2.getLoadBalancerState(), BaragonRequestState.FAILED);
    assertEquals(response2.getAgentResponses().get().get("REVERT").iterator().next().getStatusCode().get(), new Integer(200));
  }
  
  @Test
  public void testInvalidTemplateName() throws Exception {
    String requestId = "Service" + new Date().getTime();
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Collections.<String>emptyList(), Optional.<String>absent(), Optional.<Map<String, Object>>absent(), Optional.of("invalidtemplatename"));
    waitForStatus(requestId, BaragonRequestState.INVALID_REQUEST_NOOP);
    
    assertTrue(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.INVALID_REQUEST_NOOP));
  }
  
  @Test
  public void testCancelledRequest() throws Exception {
    String serviceId = setupTestService();
    waitForStatus(serviceId, BaragonRequestState.SUCCESS);
    
    String newId = serviceId + "-2";
    setupTestService(newId, serviceId, SERVICE_BASE_PATH, Collections.<String>emptyList(), Optional.<String>absent(), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
    
    BaragonResponse response = baragonServiceClient.getRequest(newId).get();
    assertEquals(response.getLoadBalancerState(), BaragonRequestState.WAITING);
    
    baragonServiceClient.cancelRequest(newId); 
    waitForStatus(newId, BaragonRequestState.CANCELED);
    
    assertEquals(baragonServiceClient.getRequest(newId).get().getLoadBalancerState(), BaragonRequestState.CANCELED);
    assertEquals(baragonServiceClient.getServiceState(serviceId).get().getUpstreams().iterator().next().getUpstream(), UPSTREAM);
  }
  
  // ----------------------------------------------------
  
  @Test
  public void testDeleteBasePath() throws Exception {
    assertFalse(baragonServiceClient.getOccupiedBasePaths(LOAD_BALANCER_GROUP).contains(SERVICE_BASE_PATH));
    String serviceId = setupTestService();
    waitForStatus(serviceId, BaragonRequestState.SUCCESS);
    
    assertTrue(baragonServiceClient.getOccupiedBasePaths(LOAD_BALANCER_GROUP).contains(SERVICE_BASE_PATH));
    
    baragonServiceClient.clearBasePath(LOAD_BALANCER_GROUP, SERVICE_BASE_PATH);
    
    assertFalse(baragonServiceClient.getOccupiedBasePaths(LOAD_BALANCER_GROUP).contains(SERVICE_BASE_PATH));
  }
  
  //@Test -- No way to add the agent back after, need to run after testClusterKnownAgents for now.
  public void testDeleteKnownAgent() {
    Collection<BaragonAgentMetadata> knownAgents = baragonServiceClient.getLoadBalancerGroupKnownAgentMetadata(LOAD_BALANCER_GROUP);
    assertTrue(knownAgents.size() > 0);
    
    BaragonAgentMetadata agentToRemove = knownAgents.iterator().next();
    
    baragonServiceClient.deleteLoadBalancerGroupKnownAgent(LOAD_BALANCER_GROUP, agentToRemove.getAgentId());
    
    assertFalse(baragonServiceClient.getLoadBalancerGroupKnownAgentMetadata(LOAD_BALANCER_GROUP).contains(agentToRemove));
  }
  
}
