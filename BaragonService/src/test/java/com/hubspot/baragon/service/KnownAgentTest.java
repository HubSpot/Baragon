package com.hubspot.baragon.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.google.common.base.Optional;
import com.hubspot.baragon.BaragonServiceTestModule;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.exceptions.InvalidAgentMetadataStringException;
import com.hubspot.baragon.models.BaragonAgentEc2Metadata;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonKnownAgentMetadata;

import name.falgout.jeffrey.testing.junit.guice.GuiceExtension;
import name.falgout.jeffrey.testing.junit.guice.IncludeModule;

@ExtendWith(GuiceExtension.class)
@IncludeModule(BaragonServiceTestModule.class)
public class KnownAgentTest {
  public static final String CLUSTER_NAME = "test-cluster";
  public static final String AGENT_ID = "123.123.123.123:8080";
  public static final String BASE_URI = "http://123.123.123.123:8080/baragon-agent/v2";
  public static final String DOMAIN = "test.com";

  @Test
  public void testKnownAgentBaragonMetadata(BaragonKnownAgentsDatastore datastore) {
    final BaragonKnownAgentMetadata metadata = new BaragonKnownAgentMetadata(BASE_URI, AGENT_ID, Optional.absent(), new BaragonAgentEc2Metadata(Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections.emptyMap(), true, System.currentTimeMillis());
    datastore.addKnownAgent(CLUSTER_NAME, metadata);

    assertEquals(Collections.singletonList(metadata), datastore.getKnownAgentsMetadata(CLUSTER_NAME));
  }

  @Test
  public void testKnownAgentString() {
    assertEquals(new BaragonAgentMetadata(BASE_URI, AGENT_ID, Optional.absent(), new BaragonAgentEc2Metadata(Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections.emptyMap(), false), BaragonAgentMetadata.fromString(BASE_URI));
  }

  @Test
  public void testInvalidBaragonAgentString() {
    Assertions.assertThrows(InvalidAgentMetadataStringException.class, () -> BaragonAgentMetadata.fromString("a;ksdjalskdjhklasdjla"));
  }
}
