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
public class PreferSameRackWeightingBalancedTest {

  final List<String> availabilityZones = Arrays.asList("us-east-1a", "us-east-1a", "us-east-1b", "us-east-1c", "us-east-1e");
  private Multiset<UpstreamInfo> generateUpstreams (List<String> availabilityZones) {
    Multiset<UpstreamInfo> upstreams = HashMultiset.create();
    for (String availabilityZone: availabilityZones) {
      UpstreamInfo testingUpstream  = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      upstreams.add(testingUpstream);
    }
    return upstreams;
  }
  final Multiset<UpstreamInfo> upstreams = generateUpstreams(availabilityZones);

  private final BaragonAgentConfiguration configuration = new BaragonAgentConfiguration();
  private static final String AGENT_ID = "123.123.123.123:8080";
  private static final String BASE_URI = "http://123.123.123.123:8080/baragon-agent/v2";
  private static final String DOMAIN = "test.com";

  private BaragonAgentMetadata generateBaragonAgentMetadata (String testRack) {
    return new BaragonAgentMetadata(BASE_URI, AGENT_ID, Optional.of(DOMAIN),
        new BaragonAgentEc2Metadata(Optional.absent(), Optional.of(testRack), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections
        .emptyMap(), true);
  }

  @Test
  public void test1a() {
    List<String> results = new ArrayList<String>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1a");
    for (String AZ: availabilityZones) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(AZ));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(configuration, agentMetadata);
      CharSequence result = helper.preferSameRackWeightingBalanced(upstreams, currentUpstream, null);
      results.add(result.toString());
      System.out.println("result: " + result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "backup", "backup", "backup"), results);
    System.out.println();
  }

  @Test
  public void test1b() {
    List<String> results = new ArrayList<String>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1b");
    for (String AZ: availabilityZones) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(AZ));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(configuration, agentMetadata);
      CharSequence result = helper.preferSameRackWeightingBalanced(upstreams, currentUpstream, null);
      results.add(result.toString());
      System.out.println("result: " + result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "weight=4", "backup", "backup"), results);
    System.out.println();
  }

  @Test
  public void test1c() {
    List<String> results = new ArrayList<String>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1c");
    for (String AZ: availabilityZones) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(AZ));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(configuration, agentMetadata);
      CharSequence result = helper.preferSameRackWeightingBalanced(upstreams, currentUpstream, null);
      results.add(result.toString());
      System.out.println("result: " + result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "backup", "weight=4", "backup"), results);
    System.out.println();
  }

  @Test
  public void test1d() {
    List<String> results = new ArrayList<String>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1e");
    for (String AZ: availabilityZones) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of(AZ));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(configuration, agentMetadata);
      CharSequence result = helper.preferSameRackWeightingBalanced(upstreams, currentUpstream, null);
      results.add(result.toString());
      System.out.println("result: " + result.toString());
    }
    Assert.assertEquals(Arrays.asList("", "", "backup", "backup", "weight=4"), results);
    System.out.println();
  }
}
