package com.hubspot.baragon.service.managers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.common.base.Optional;
import com.hubspot.baragon.exceptions.InvalidAgentMetadataStringException;
import com.hubspot.baragon.models.BaragonAgentEc2Metadata;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class KnownAgentTest {
  public static final String CLUSTER_NAME = "test-cluster";
  public static final String AGENT_ID = "123.123.123.123:8080";
  public static final String BASE_URI = "http://123.123.123.123:8080/baragon-agent/v2";
  public static final String DOMAIN = "test.com";

  @Test
  public void testKnownAgentString() {
    assertEquals(
      new BaragonAgentMetadata(
        BASE_URI,
        AGENT_ID,
        Optional.absent(),
        new BaragonAgentEc2Metadata(
          Optional.absent(),
          Optional.absent(),
          Optional.absent(),
          Optional.absent(),
          Optional.absent()
        ),
        Optional.absent(),
        Collections.emptyMap(),
        false
      ),
      BaragonAgentMetadata.fromString(BASE_URI)
    );
  }

  @Test
  public void testInvalidBaragonAgentString() {
    Assertions.assertThrows(
      InvalidAgentMetadataStringException.class,
      () -> BaragonAgentMetadata.fromString("a;ksdjalskdjhklasdjla")
    );
  }
}
