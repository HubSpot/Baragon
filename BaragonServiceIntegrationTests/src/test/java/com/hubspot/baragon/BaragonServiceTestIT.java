package com.hubspot.baragon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Optional.<String>absent());
    
    return requestId;
  }
  
  private void setupTestService(String requestId, String serviceId, String basePath, Optional<String> replaceServiceId) {
    System.out.println("Requesting " + serviceId + "...");
    UpstreamInfo upstream = new UpstreamInfo(UPSTREAM, Optional.<String>absent(), Optional.<String>absent());
    BaragonService lbService = new BaragonService(serviceId, new ArrayList<String>(), basePath, 
        new HashSet<String>(Arrays.asList(LOAD_BALANCER_GROUP)), new HashMap<String, Object>(), Optional.<String>absent());
    
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
      
      setupTestService(requestId + "-2", requestId, SERVICE_BASE_PATH + "-2", Optional.fromNullable(requestId));
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
    setupTestService(requestId, requestId, SERVICE_BASE_PATH, Optional.fromNullable("someotherservice"));
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
      setupTestService(serviceId2, serviceId2, SERVICE_BASE_PATH, Optional.fromNullable("someotherservice"));
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
      setupTestService(renameId, renameId, SERVICE_BASE_PATH + "-2", Optional.fromNullable(serviceId));
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
  
}
