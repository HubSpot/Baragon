package com.hubspot.baragon.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.jukito.JukitoRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.handlebars.PreferSameRackWeightingHelper;
import com.hubspot.baragon.models.BaragonAgentEc2Metadata;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.UpstreamInfo;


@RunWith(JukitoRunner.class)
public class PreferSameRackWeightingTest {

  private static final Collection<String> AVAILABILITY_ZONES = Arrays.asList("us-east-1a", "us-east-1a", "us-east-1b", "us-east-1c", "us-east-1e");
  private static final Collection<UpstreamInfo> UPSTREAMS = AVAILABILITY_ZONES.stream().map((availabilityZone) -> new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone))).collect(Collectors.toList());
  private static final BaragonAgentConfiguration CONFIGURATION = new BaragonAgentConfiguration();
  private static final String AGENT_ID = "123.123.123.123:8080";
  private static final String BASE_URI = "http://123.123.123.123:8080/baragon-agent/v2";
  private static final String DOMAIN = "test.com";

  private BaragonAgentMetadata generateBaragonAgentMetadata (String availabilityZone) {
    final BaragonAgentMetadata agentMetadata = new BaragonAgentMetadata(BASE_URI, AGENT_ID, Optional.of(DOMAIN),
        new BaragonAgentEc2Metadata(Optional.absent(), Optional.of(availabilityZone), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections
        .emptyMap(), true);
    return agentMetadata;
  }
  @Test
  public void test1a() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1a");
    final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
    for (String availabilityZone: AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      CharSequence result = helper.preferSameRackWeightingOriginal(UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "backup", "backup", "backup"), results);
  }

  @Test
  public void test1b() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1b");
    final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
    for (String availabilityZone: AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      CharSequence result = helper.preferSameRackWeightingOriginal(UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "weight=2", "", ""), results);
  }

  @Test
  public void test1c() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1c");
    final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
    for (String availabilityZone: AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      CharSequence result = helper.preferSameRackWeightingOriginal(UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "", "weight=2", ""), results);
  }

  @Test
  public void test1e() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1e");
    final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
    for (String availabilityZone: AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of(availabilityZone));
      CharSequence result = helper.preferSameRackWeightingOriginal(UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "", "", "weight=2"), results);
  }

}
