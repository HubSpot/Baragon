package com.hubspot.baragon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Stopwatch;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import com.google.common.base.Optional;

import static org.junit.Assert.*;

public class BaragonServiceTestIT {
  
  public static final String LOAD_BALANCER_GROUP = "docker-baragon";
  public static final String UPSTREAM = "example.com:80";
  public static final String SERVICE_BASE_PATH = "/testservice";
	
  private BaragonServiceClient baragonServiceClient;

  // Helpers --------------------------------------------
  
  private Injector getInjector() {
    return Guice.createInjector(new DockerTestModule());
  }
  
  private String setupTestService() {
    String requestId = "Service" + new Date().getTime();
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Optional.<String>absent(), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
    
    return requestId;
  }
  
  private void setupTestService(String requestId, String serviceId, String basePath, Optional<String> replaceServiceId, Optional<Map<String, Object>> options, Optional<String> template) {
    System.out.println("Requesting " + requestId + "...");
    UpstreamInfo upstream = new UpstreamInfo(UPSTREAM, Optional.<String>absent(), Optional.<String>absent());
    BaragonService lbService = new BaragonService(serviceId, new ArrayList<String>(), basePath, 
        new HashSet<String>(Arrays.asList(LOAD_BALANCER_GROUP)), options.isPresent() ? options.get() : new HashMap<String, Object>(), template);
    
    BaragonRequest request = new BaragonRequest(requestId, lbService, Arrays.asList(upstream), new ArrayList<UpstreamInfo>(), replaceServiceId);
    baragonServiceClient.enqueueRequest(request);
  }
  
  private void removeService(String serviceId) {
    System.out.println("Cleaning up...");
    BaragonResponse resp = baragonServiceClient.deleteService(serviceId).get();
    while(baragonServiceClient.getRequest(resp.getLoadBalancerRequestId()).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    assertFalse(baragonServiceClient.getServiceState(serviceId).isPresent());
  }
  
  // ----------------------------------------------------
  
  @Before
  public void setup() throws Exception {
    baragonServiceClient = getInjector().getInstance(BaragonServiceClient.class);
  }
  
  @Rule
  public TestRule watcher = new TestWatcher() {
     protected void starting(Description description) {
        System.out.println("Starting: " + description.getMethodName());
     }
     protected void succeeded(Description description) {
       System.out.println("\u001B[32mTest passed\u001B[0m");
     }
     protected void failed(Throwable e, Description description) {
       System.out.println("\u001B[31mTest failed\u001B[0m");
     }
  };
  
  
  @Rule
  public Stopwatch stopwatch = new Stopwatch() {
      @Override
      protected void finished(long nanos, Description description) {
        System.out.println("(" + nanos / 1000000000.0 + " s)");
      }
  };
  
  @After
  public void teardown() {
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
    BaragonAgentMetadata metadata = baragonServiceClient.getLoadBalancerGroupAgentMetadata(LOAD_BALANCER_GROUP).toArray(new BaragonAgentMetadata[0])[0];
    assertTrue(metadata.getBaseAgentUri() != null && !metadata.getBaseAgentUri().isEmpty());
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
    while(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    if(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
      testGetService();
      testState();
      testClusterBasePaths();
      
      removeService(serviceId);
    } 
    else {
      BaragonResponse resp = baragonServiceClient.getRequest(serviceId).get();
      throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
          + "\nState: " + resp.getLoadBalancerState());
    }
  }
  
  public void testClusterBasePaths() {
    String groupName = baragonServiceClient.getLoadBalancerGroups().toArray(new String[0])[0];
    String[] basePaths = baragonServiceClient.getOccupiedBasePaths(groupName).toArray(new String[0]);
    assertTrue(basePaths.length > 0);
    BaragonService service = baragonServiceClient.getServiceForBasePath(LOAD_BALANCER_GROUP, basePaths[0]).get();
    assertTrue(service.getServiceId() != null);
  }
  
  public void testGetService() {
    BaragonServiceState state = baragonServiceClient.getGlobalState().iterator().next();
    String serviceId = state.getService().getServiceId();
    assertNotNull(serviceId);
    BaragonServiceState serviceState = baragonServiceClient.getServiceState(serviceId).get();
    assertNotNull(serviceState.getService().getServiceId());
  }
  
  public void testState() {
    BaragonServiceState state = baragonServiceClient.getGlobalState().iterator().next();
    assertNotNull(state.getService().getServiceId());
  }
  
  // ----------------------------------------------------
  
  @Test
  public void testValidPathChange() throws Exception {
    
    String requestId = setupTestService();
    while(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    if(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
      
      setupTestService(requestId + "-2", requestId, SERVICE_BASE_PATH + "-2", Optional.fromNullable(requestId), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
      while(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
      
      if(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
        assertFalse(baragonServiceClient.getOccupiedBasePaths(LOAD_BALANCER_GROUP).contains("/" + SERVICE_BASE_PATH + "-2"));
      }
      else {
        BaragonResponse resp = baragonServiceClient.getRequest(requestId).get();
        throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
            + "\nState: " + resp.getLoadBalancerState());
      }
      
      removeService(requestId);
    } 
    else {
      BaragonResponse resp = baragonServiceClient.getRequest(requestId).get();
      throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
          + "\nState: " + resp.getLoadBalancerState());
    }     
  }
  
  @Test
  public void testValidRequestNonexistantReplaceId() throws Exception {
    String requestId = "Service" + new Date().getTime();
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Optional.fromNullable("someotherservice"), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
    while(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    if(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
      removeService(requestId);
    }
    else {
      BaragonResponse resp = baragonServiceClient.getRequest(requestId).get();
      throw new Exception("Request did not go as expected: " + resp.getMessage().get().toString() 
          + "\nState: " + resp.getLoadBalancerState());
    }
  }
  
  @Test
  public void testBasePathConflict() throws Exception {
    String serviceId = setupTestService();
    while(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    if(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
      // Good
    } 
    else {
      BaragonResponse resp = baragonServiceClient.getRequest(serviceId).get();
      throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
          + "\nState: " + resp.getLoadBalancerState());
    }
    
    String serviceId2 = setupTestService();
    while(baragonServiceClient.getRequest(serviceId2).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    if(baragonServiceClient.getRequest(serviceId2).get().getLoadBalancerState().equals(BaragonRequestState.INVALID_REQUEST_NOOP)) {
      // Good
    }
    else {
      BaragonResponse resp = baragonServiceClient.getRequest(serviceId2).get();
      throw new Exception("Request did not go as expected: " + resp.getMessage().get().toString() 
          + "\nState: " + resp.getLoadBalancerState());
    }
    removeService(serviceId);
  }
  
  @Test
  public void testBasePathConflictInvalidReplaceId() throws Exception {
    String serviceId = setupTestService();
    while(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    if(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
      String serviceId2 = serviceId + "-2";
      setupTestService(serviceId2, serviceId2, SERVICE_BASE_PATH, Optional.fromNullable("someotherservice"), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
      while(baragonServiceClient.getRequest(serviceId2).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
      
      if(baragonServiceClient.getRequest(serviceId2).get().getLoadBalancerState().equals(BaragonRequestState.INVALID_REQUEST_NOOP)) {
        // Good
      }
      else {
        BaragonResponse resp = baragonServiceClient.getRequest(serviceId2).get();
        throw new Exception("Request did not go as expected: " + resp.getMessage().get().toString() 
            + "\nState: " + resp.getLoadBalancerState());
      }
      removeService(serviceId);
    } 
    else {
      BaragonResponse resp = baragonServiceClient.getRequest(serviceId).get();
      throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
          + "\nState: " + resp.getLoadBalancerState());
    }
  }

  @Test
  public void testValidBasePathRename() throws Exception {
    String serviceId = setupTestService();
    while(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    if(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
      
      String renameId = serviceId + "-2";
      setupTestService(renameId, renameId, SERVICE_BASE_PATH + "-2", Optional.fromNullable(serviceId), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
      while(baragonServiceClient.getRequest(renameId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
      
      if(baragonServiceClient.getRequest(renameId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
        assertTrue(baragonServiceClient.getOccupiedBasePaths(LOAD_BALANCER_GROUP).iterator().next().equals(SERVICE_BASE_PATH + "-2"));
        
        removeService(renameId);
      } 
      else {
        removeService(serviceId);
        BaragonResponse resp = baragonServiceClient.getRequest(renameId).get();
        throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
            + "\nState: " + resp.getLoadBalancerState());
      }
    } 
    else {
      BaragonResponse resp = baragonServiceClient.getRequest(serviceId).get();
      throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
          + "\nState: " + resp.getLoadBalancerState());
    }
  }
  
  @Test
  public void testValidBasePathServiceIdChange() throws Exception {
    String serviceId = setupTestService();
    while(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    if(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
      String renameId = serviceId + "-2";
      setupTestService(renameId, renameId, SERVICE_BASE_PATH, Optional.fromNullable(serviceId), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
      while(baragonServiceClient.getRequest(renameId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
      
      if(baragonServiceClient.getRequest(renameId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
        assertTrue(baragonServiceClient.getServiceState(renameId).get().getService().getServiceBasePath().equals(SERVICE_BASE_PATH));
        removeService(renameId);
      } 
      else {
        removeService(serviceId);
        BaragonResponse resp = baragonServiceClient.getRequest(serviceId).get();
        throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
            + "\nState: " + resp.getLoadBalancerState());
      }
    } 
    else {
      BaragonResponse resp = baragonServiceClient.getRequest(serviceId).get();
      throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
          + "\nState: " + resp.getLoadBalancerState());
    }
  }

  @Test
  public void testInvalidRequest() throws Exception {
    String requestId = "Service" + new Date().getTime();
    HashMap<String, Object> options = new HashMap<String, Object>();
    options.put("nginxExtraConfigs", new String[] {"rewrite /this_is_invalid_yo"});
    
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Optional.<String>absent(), Optional.<Map<String, Object>>fromNullable(options), Optional.<String>absent());
    while(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    BaragonResponse response = baragonServiceClient.getRequest(requestId).get();
    assertEquals(response.getLoadBalancerState(), BaragonRequestState.FAILED);
    assertEquals(response.getAgentResponses().get().get("REVERT").iterator().next().getStatusCode().get(), new Integer(200));
  }

  @Test
  public void testInvalidRequestNonexistantReplaceId() throws Exception {
    String requestId = "Service" + new Date().getTime();
    HashMap<String, Object> options = new HashMap<String, Object>();
    options.put("nginxExtraConfigs", new String[] {"rewrite /this_is_invalid_yo"});
    
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Optional.fromNullable("someotherservice"), Optional.<Map<String, Object>>fromNullable(options), Optional.<String>absent());
    while(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    BaragonResponse response = baragonServiceClient.getRequest(requestId).get();
    assertEquals(response.getLoadBalancerState(), BaragonRequestState.FAILED);
    assertEquals(response.getAgentResponses().get().get("REVERT").iterator().next().getStatusCode().get(), new Integer(200));
  }

  @Test
  public void testInvalidRequestWithBasePathChange() throws Exception {
    String requestId = setupTestService();
    while(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    BaragonResponse response = baragonServiceClient.getRequest(requestId).get();
    
    if(response.getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
      String requestId2 = "Service" + new Date().getTime();
      HashMap<String, Object> options = new HashMap<String, Object>();
      options.put("nginxExtraConfigs", new String[] {"rewrite /this_is_invalid_yo"});
      
      setupTestService(requestId2, requestId2, SERVICE_BASE_PATH, Optional.fromNullable(requestId), Optional.<Map<String, Object>>fromNullable(options), Optional.<String>absent());
      while(baragonServiceClient.getRequest(requestId2).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
      
      BaragonResponse response2 = baragonServiceClient.getRequest(requestId2).get();
      assertEquals(response2.getLoadBalancerState(), BaragonRequestState.FAILED);
      assertEquals(response2.getAgentResponses().get().get("REVERT").iterator().next().getStatusCode().get(), new Integer(200));
      
      removeService(requestId);
    } 
    else {
      BaragonResponse resp = baragonServiceClient.getRequest(requestId).get();
      throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
          + "\nState: " + resp.getLoadBalancerState());
    }
  }
  
  @Test
  public void testInvalidTemplateName() throws Exception {
    String requestId = "Service" + new Date().getTime();
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Optional.<String>absent(), Optional.<Map<String, Object>>absent(), Optional.fromNullable("invalidtemplatename"));
    while(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    assertTrue(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.INVALID_REQUEST_NOOP));
    
    if(baragonServiceClient.getRequest(requestId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS))
      removeService(requestId);
  }
  
  @Test
  public void testCancelledRequest() throws Exception {
    String serviceId = setupTestService();
    while(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    if(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
      String newId = serviceId + "-2";
      setupTestService(newId, serviceId, SERVICE_BASE_PATH, Optional.<String>absent(), Optional.<Map<String, Object>>absent(), Optional.<String>absent());
      
      BaragonResponse response = baragonServiceClient.getRequest(newId).get();
      assertEquals(response.getLoadBalancerState(), BaragonRequestState.WAITING);
      
      baragonServiceClient.cancelRequest(newId); 
      while(baragonServiceClient.getRequest(newId).get().getLoadBalancerState().equals(BaragonRequestState.CANCELING)) {}
      
      assertEquals(baragonServiceClient.getRequest(newId).get().getLoadBalancerState(), BaragonRequestState.CANCELED);
      assertEquals(baragonServiceClient.getServiceState(serviceId).get().getUpstreams().iterator().next().getUpstream(), UPSTREAM);
      
      if(response.getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
        removeService(newId);
      }
      removeService(serviceId);
    }
    else {
      BaragonResponse resp = baragonServiceClient.getRequest(serviceId).get();
      throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
          + "\nState: " + resp.getLoadBalancerState());
    }
  }
  
  // ----------------------------------------------------
  
  @Test
  public void testDeleteBasePath() throws Exception {
    assertFalse(baragonServiceClient.getOccupiedBasePaths(LOAD_BALANCER_GROUP).contains(SERVICE_BASE_PATH));
    String serviceId = setupTestService();
    while(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.WAITING)) { }
    
    if(baragonServiceClient.getRequest(serviceId).get().getLoadBalancerState().equals(BaragonRequestState.SUCCESS)) {
      assertTrue(baragonServiceClient.getOccupiedBasePaths(LOAD_BALANCER_GROUP).contains(SERVICE_BASE_PATH));
      
      baragonServiceClient.clearBasePath(LOAD_BALANCER_GROUP, SERVICE_BASE_PATH);
      
      assertFalse(baragonServiceClient.getOccupiedBasePaths(LOAD_BALANCER_GROUP).contains(SERVICE_BASE_PATH));
      
      removeService(serviceId);
    } 
    else {
      BaragonResponse resp = baragonServiceClient.getRequest(serviceId).get();
      throw new Exception("Request did not succeed: " + resp.getMessage().get().toString() 
          + "\nState: " + resp.getLoadBalancerState());
    }
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

