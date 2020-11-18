package com.hubspot.baragon.agent.managers;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AgentRequestManagerTest {

  @Test
  void getServiceBasePathWithoutLeadingSlash() {
    String baseUrlPath = "/test-service/path-part-2";
    assertEquals("test-service/path-part-2", AgentRequestManager.getServiceBasePathWithoutLeadingSlash(baseUrlPath));
  }

  @Test
  void getServiceBasePathWithoutLeadingSlashWithEmptyString() {
    String baseUrlPath = "";
    assertEquals("", AgentRequestManager.getServiceBasePathWithoutLeadingSlash(baseUrlPath));
  }
}