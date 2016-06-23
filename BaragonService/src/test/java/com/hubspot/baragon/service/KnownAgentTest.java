package com.hubspot.baragon.service;

import java.util.Collections;

import com.google.common.base.Optional;
import com.hubspot.baragon.BaragonServiceTestModule;
import com.hubspot.baragon.data.BaragonKnownAgentsDatastore;
import com.hubspot.baragon.exceptions.InvalidAgentMetadataStringException;
import com.hubspot.baragon.models.BaragonAgentEc2Metadata;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.BaragonKnownAgentMetadata;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertEquals;

@RunWith(JukitoRunner.class)
public class KnownAgentTest {
  public static final String CLUSTER_NAME = "test-cluster";
  public static final String AGENT_ID = "123.123.123.123:8080";
  public static final String BASE_URI = "http://123.123.123.123:8080/baragon-agent/v2";
  public static final String DOMAIN = "test.com";


  public static class Module extends JukitoModule {

    @Override
    protected void configureTest() {
      install(new BaragonServiceTestModule());
    }
  }

  @Test
  public void testKnownAgentBaragonMetadata(BaragonKnownAgentsDatastore datastore) {
    final BaragonKnownAgentMetadata metadata = new BaragonKnownAgentMetadata(BASE_URI, AGENT_ID, Optional.of(DOMAIN), new BaragonAgentEc2Metadata(Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent()), Collections.<String, String>emptyMap(), true, System.currentTimeMillis());
    datastore.addKnownAgent(CLUSTER_NAME, metadata);

    assertEquals(Collections.singletonList(metadata), datastore.getKnownAgentsMetadata(CLUSTER_NAME));
  }

  @Test
  public void testKnownAgentString() {
    assertEquals(new BaragonAgentMetadata(BASE_URI, AGENT_ID, Optional.<String>absent(), new BaragonAgentEc2Metadata(Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent(), Optional.<String>absent()), Collections.<String, String>emptyMap(), false), BaragonAgentMetadata.fromString(BASE_URI));
  }

  @Test( expected = InvalidAgentMetadataStringException.class )
  public void testInvalidBaragonAgentString() {
    BaragonAgentMetadata.fromString("a;ksdjalskdjhklasdjla");
  }
}
