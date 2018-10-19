package com.hubspot.baragon.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jukito.JukitoRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.handlebars.PreferSameRackWeightingHelper;
import com.hubspot.baragon.models.BaragonAgentEc2Metadata;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.UpstreamInfo;


@RunWith(JukitoRunner.class)
public class PreferSameRackWeightingTest {

  final List<String> availabilityZones = Arrays.asList("us-east-1a", "us-east-1a", "us-east-1b", "us-east-1c", "us-east-1e");

  private Multiset<UpstreamInfo> generateUpstreams (List<String> availabilityZones) {
    Multiset<UpstreamInfo> upstreams = HashMultiset.create();
    for (String availabilityZone: availabilityZones) {
      UpstreamInfo testingUpstream  = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      upstreams.add(testingUpstream);
    }
    return upstreams;
  }

  private final Multiset<UpstreamInfo> upstreams = generateUpstreams(availabilityZones);

  private static final BaragonAgentConfiguration configuration = new BaragonAgentConfiguration();
  private static final String AGENT_ID = "123.123.123.123:8080";
  private static final String BASE_URI = "http://123.123.123.123:8080/baragon-agent/v2";
  private static final String DOMAIN = "test.com";

  @Test
  public void test1a() {
    List<String> results = new ArrayList<String>();
    final BaragonAgentMetadata agentMetadata = new BaragonAgentMetadata(BASE_URI, AGENT_ID, Optional.of(DOMAIN),
        new BaragonAgentEc2Metadata(Optional.absent(), Optional.of("us-east-1a"), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections
        .emptyMap(), true);
    for (String AZ: availabilityZones) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(AZ));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(configuration, agentMetadata);
      CharSequence result = helper.preferSameRackWeighting(upstreams, currentUpstream, null);
      results.add(result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "backup", "backup", "backup"), results);
  }

  @Test
  public void test1b() {
    List<String> results = new ArrayList<String>();
    final BaragonAgentMetadata agentMetadata = new BaragonAgentMetadata(BASE_URI, AGENT_ID, Optional.of(DOMAIN),
        new BaragonAgentEc2Metadata(Optional.absent(), Optional.of("us-east-1b"), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections
        .emptyMap(), true);
    for (String AZ: availabilityZones) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(AZ));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(configuration, agentMetadata);
      CharSequence result = helper.preferSameRackWeighting(upstreams, currentUpstream, null);
      results.add(result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "weight=2", "", ""), results);
  }

  @Test
  public void test1c() {
    List<String> results = new ArrayList<String>();
    final BaragonAgentMetadata agentMetadata = new BaragonAgentMetadata(BASE_URI, AGENT_ID, Optional.of(DOMAIN),
        new BaragonAgentEc2Metadata(Optional.absent(), Optional.of("us-east-1c"), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections
        .emptyMap(), true);
    for (String AZ: availabilityZones) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(AZ));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(configuration, agentMetadata);
      CharSequence result = helper.preferSameRackWeighting(upstreams, currentUpstream, null);
      results.add(result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "", "weight=2", ""), results);
  }

  @Test
  public void test1e() {
    List<String> results = new ArrayList<String>();
    final BaragonAgentMetadata agentMetadata = new BaragonAgentMetadata(BASE_URI, AGENT_ID, Optional.of(DOMAIN),
        new BaragonAgentEc2Metadata(Optional.absent(), Optional.of("us-east-1e"), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections
        .emptyMap(), true);
    for (String AZ: availabilityZones) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of(AZ));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(configuration, agentMetadata);
      CharSequence result = helper.preferSameRackWeighting(upstreams, currentUpstream, null);
      results.add(result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "", "", "weight=2"), results);
  }
}
