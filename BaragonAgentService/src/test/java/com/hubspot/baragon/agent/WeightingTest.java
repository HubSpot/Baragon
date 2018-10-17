package com.hubspot.baragon.agent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.github.jknack.handlebars.Handlebars;
import com.google.common.base.Optional;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.handlebars.PreferSameRackWeightingHelper;
import com.hubspot.baragon.models.BaragonAgentEc2Metadata;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.UpstreamInfo;


@RunWith(JukitoRunner.class)
public class WeightingTest {

  final String requestId = "test-126";
  final String requestId1 = "test-127";
  final String requestId2 = "test-128";
  final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.of(requestId), Optional.<String>absent());
  final UpstreamInfo testUpstream1 = new UpstreamInfo("testhost:8082", Optional.of(requestId1), Optional.<String>absent());
  final UpstreamInfo testUpstream2 = new UpstreamInfo("testhost:8081", Optional.of(requestId2), Optional.<String>absent());
  final List<UpstreamInfo> upstreams = Arrays.asList(testUpstream1, testUpstream2);
  final Object[] params = new Object[1];
  final Handlebars handlebars = new Handlebars();
//  final Options options = new Options.Builder(handlebars, "PreferSameRankWeightingHelper",).build();
  private final BaragonAgentConfiguration configuration = new BaragonAgentConfiguration();
  public static final String AGENT_ID = "123.123.123.123:8080";
  public static final String BASE_URI = "http://123.123.123.123:8080/baragon-agent/v2";
  public static final String DOMAIN = "test.com";
  final BaragonAgentMetadata agentMetadata = new BaragonAgentMetadata(BASE_URI, AGENT_ID, Optional.of(DOMAIN), new BaragonAgentEc2Metadata(Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections
      .emptyMap(), true);
  final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(configuration, agentMetadata);



  @Test
  public void simpleTest() {
//    CharSequence result = helper.preferSameRackWeighting(upstreams, currentUpstream, options);
    System.out.println("yes");

  }


}
