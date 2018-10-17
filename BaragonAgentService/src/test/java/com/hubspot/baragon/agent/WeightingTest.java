package com.hubspot.baragon.agent;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.handlebars.PreferSameRackWeightingHelper;
import com.hubspot.baragon.models.BaragonAgentEc2Metadata;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.UpstreamInfo;


@RunWith(JukitoRunner.class)
public class WeightingTest {

  final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of("us-east-5"));

  final UpstreamInfo testUpstream1 = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of("us-east-1"));
  final UpstreamInfo testUpstream2 = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of("us-east-1"));
  final UpstreamInfo testUpstream3 = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of("us-east-2"));
  final UpstreamInfo testUpstream4 = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of("us-east-3"));
  final UpstreamInfo testUpstream6 = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of("us-east-3"));
  final UpstreamInfo testUpstream5 = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of("us-east-5"));
  final List<UpstreamInfo> upstreams = Arrays.asList(testUpstream1, testUpstream2, testUpstream3, testUpstream4, testUpstream5, testUpstream6);

  private final BaragonAgentConfiguration configuration = new BaragonAgentConfiguration();
  public static final String AGENT_ID = "123.123.123.123:8080";
  public static final String BASE_URI = "http://123.123.123.123:8080/baragon-agent/v2";
  public static final String DOMAIN = "test.com";
  final BaragonAgentMetadata agentMetadata = new BaragonAgentMetadata(BASE_URI, AGENT_ID, Optional.of(DOMAIN),
      new BaragonAgentEc2Metadata(Optional.absent(), Optional.of("us-east-5"), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections
      .emptyMap(), true);

  final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(configuration, agentMetadata);


  @Test
  public void simpleTest() {
    CharSequence result = helper.preferSameRackWeighting(upstreams, currentUpstream, null);
    System.out.println("result: " + result.toString());
  }


}
