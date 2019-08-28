package com.hubspot.baragon.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.base.Optional;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.handlebars.PreferSameRackWeightingHelper;
import com.hubspot.baragon.models.BaragonAgentEc2Metadata;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.UpstreamInfo;


public class PreferSameRackWeightingBalancedTest {

  private static final Collection<String> AVAILABILITY_ZONES = Arrays.asList("us-east-1a", "us-east-1a", "us-east-1b", "us-east-1c", "us-east-1e");
  private static final Collection<UpstreamInfo> UPSTREAMS = AVAILABILITY_ZONES.stream()
      .map((availabilityZone) -> new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone)))
      .collect(Collectors.toList());
  private static final BaragonAgentConfiguration CONFIGURATION = new BaragonAgentConfiguration();
  private static final String AGENT_ID = "123.123.123.123:8080";
  private static final String BASE_URI = "http://123.123.123.123:8080/baragon-agent/v2";
  private static final String DOMAIN = "test.com";

  private BaragonAgentMetadata generateBaragonAgentMetadata(String availabilityZone) {
    final BaragonAgentMetadata agentMetadata = new BaragonAgentMetadata(BASE_URI, AGENT_ID, Optional.of(DOMAIN),
        new BaragonAgentEc2Metadata(Optional.absent(), Optional.of(availabilityZone), Optional.absent(), Optional.absent(), Optional.absent()), Optional.absent(), Collections
        .emptyMap(), true);
    return agentMetadata;
  }


  @Test
  public void testSimpleCase1A() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1a");
    final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
    for (String availabilityZone : AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      CharSequence result = helper.preferSameRackWeighting(UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("", "", "backup", "backup", "backup"), results);
  }

  @Test
  public void testSimpleCase1B() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1b");
    for (String availabilityZone : AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
      CharSequence result = helper.preferSameRackWeighting(UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("weight=2", "weight=2", "weight=16", "backup", "backup"), results);
  }

  @Test
  public void testSimpleCase1C() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1c");
    for (String availabilityZone : AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
      CharSequence result = helper.preferSameRackWeighting(UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("weight=2", "weight=2", "backup", "weight=16", "backup"), results);
  }

  @Test
  public void testSimpleCase1E() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1e");
    for (String availabilityZone : AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of(availabilityZone));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
      CharSequence result = helper.preferSameRackWeighting(UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("weight=2", "weight=2", "backup", "backup", "weight=16"), results);
  }

  @Test
  public void testSimpleCase1D() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1d");
    for (String availabilityZone : AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of(availabilityZone));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
      CharSequence result = helper.preferSameRackWeighting(UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("", "", "", "", ""), results);
  }


  private static final Collection<String> LARGER_AVAILABILITY_ZONES = Arrays.asList("us-east-1a", "us-east-1a", "us-east-1b", "us-east-1b", "us-east-1b", "us-east-1b", "us-east-1c", "us-east-1c", "us-east-1c", "us-east-1c", "us-east-1e", "us-east-1e");
  private static final Collection<UpstreamInfo> MANY_UPSTREAMS = LARGER_AVAILABILITY_ZONES.stream()
      .map((availabilityZone) -> new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone)))
      .collect(Collectors.toList());


  @Test
  public void testLargerCase1A() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1a");
    final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
    for (String availabilityZone : LARGER_AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      CharSequence result = helper.preferSameRackWeighting(MANY_UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("weight=16", "weight=16", "weight=2", "weight=2", "weight=2", "weight=2", "weight=2", "weight=2", "weight=2", "weight=2", "backup", "backup"), results);
  }

  @Test
  public void testLargerCase1B() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1b");
    final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
    for (String availabilityZone : LARGER_AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      CharSequence result = helper.preferSameRackWeighting(MANY_UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("backup", "backup", "", "", "", "", "backup", "backup", "backup", "backup", "backup", "backup"), results);
  }

  @Test
  public void testLargerCase1C() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1c");
    final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
    for (String availabilityZone : LARGER_AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      CharSequence result = helper.preferSameRackWeighting(MANY_UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("backup", "backup", "backup", "backup", "backup", "backup", "", "", "", "", "backup", "backup"), results);
  }

  @Test
  public void testLargerCase1D() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1d");
    final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
    for (String availabilityZone : LARGER_AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of(availabilityZone));
      CharSequence result = helper.preferSameRackWeighting(MANY_UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("", "", "", "", "", "", "", "", "", "", "", ""), results);
  }

  @Test
  public void testLargerCase1E() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1e");
    final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
    for (String availabilityZone : LARGER_AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.of(availabilityZone));
      CharSequence result = helper.preferSameRackWeighting(MANY_UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("backup", "backup", "weight=2", "weight=2", "weight=2", "weight=2", "weight=2", "weight=2", "weight=2", "weight=2", "weight=16", "weight=16"), results);
  }

  private static final Collection<String> NULL_AVAILABILITY_ZONES = Arrays.asList(null, null, null, null);
  private static final Collection<UpstreamInfo> NULL_UPSTREAMS = NULL_AVAILABILITY_ZONES.stream()
      .map((availabilityZone) -> new UpstreamInfo("testhost:8080", Optional.absent(), Optional.absent()))
      .collect(Collectors.toList());

  @Test
  public void testNullCase1D() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1d");
    final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
    for (String availabilityZone : NULL_AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.of("test-126"), Optional.absent());
      CharSequence result = helper.preferSameRackWeighting(NULL_UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("", "", "", ""), results);
  }

  private static final Collection<String> NEW_AVAILABILITY_ZONES = Arrays.asList("us-east-1b", "us-east-1b", "us-east-1e", "us-east-1e", "us-east-1e", "us-east-1a", "us-east-1a", "us-east-1a");
  private static final Collection<UpstreamInfo> NEW_UPSTREAMS = NEW_AVAILABILITY_ZONES.stream()
      .map((availabilityZone) -> new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone)))
      .collect(Collectors.toList());

  @Test
  public void testBigDecimalToIntegerCase1B() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1b");
    for (String availabilityZone : NEW_AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
      CharSequence result = helper.preferSameRackWeighting(NEW_UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("weight=9", "weight=9", "", "", "", "", "", ""), results);
  }

  @Test
  public void testBigDecimalToIntegerCase1E() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1e");
    for (String availabilityZone : NEW_AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
      CharSequence result = helper.preferSameRackWeighting(NEW_UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("backup", "backup", "", "", "", "backup", "backup", "backup"), results);
  }

  @Test
  public void testBigDecimalToIntegerCase1A() {
    List<String> results = new ArrayList<>();
    final BaragonAgentMetadata agentMetadata = generateBaragonAgentMetadata("us-east-1a");
    for (String availabilityZone : NEW_AVAILABILITY_ZONES) {
      final UpstreamInfo currentUpstream = new UpstreamInfo("testhost:8080", Optional.absent(), Optional.of(availabilityZone));
      final PreferSameRackWeightingHelper helper = new PreferSameRackWeightingHelper(CONFIGURATION, agentMetadata);
      CharSequence result = helper.preferSameRackWeighting(NEW_UPSTREAMS, currentUpstream, null);
      results.add(result.toString());
    }
    Assertions.assertEquals(Arrays.asList("backup", "backup", "backup", "backup", "backup", "", "", ""), results);
  }
}
