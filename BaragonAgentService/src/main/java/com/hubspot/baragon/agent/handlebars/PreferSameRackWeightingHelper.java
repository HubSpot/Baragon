package com.hubspot.baragon.agent.handlebars;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.github.jknack.handlebars.Options;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.models.BaragonAgentMetadata;
import com.hubspot.baragon.models.UpstreamInfo;

public class PreferSameRackWeightingHelper {
  private final BaragonAgentConfiguration configuration;
  private final BaragonAgentMetadata agentMetadata;

  public PreferSameRackWeightingHelper(BaragonAgentConfiguration configuration, BaragonAgentMetadata agentMetadata) {
    this.configuration = configuration;
    this.agentMetadata = agentMetadata;
  }

  public CharSequence preferSameRackWeightingOriginal(Collection<UpstreamInfo> upstreams, UpstreamInfo currentUpstream, Options options) {
    if (agentMetadata.getEc2().getAvailabilityZone().isPresent() && currentUpstream.getRackId().isPresent()) {
      String currentRack = agentMetadata.getEc2().getAvailabilityZone().get();
      System.out.println("Current rack: " + currentRack);
      System.out.println("Current upstream: "+ currentUpstream.getRackId().get());
      int maxCount = 0;
      Multiset<String> racks = HashMultiset.create();
      for (UpstreamInfo upstreamInfo : upstreams) {
        if (upstreamInfo.getRackId().isPresent()) {
          racks.add(upstreamInfo.getRackId().get());
          if (racks.count(upstreamInfo.getRackId().get()) > maxCount) {
            maxCount = racks.count(upstreamInfo.getRackId().get());
          }
        }
      }
      System.out.println("max count: " + maxCount);
      System.out.println("all racks: " + racks);
      if (racks.count(currentRack) == 0) {
        System.out.println("racks.count(currentRack) == 0");
        return "";
      }

      if (racks.count(currentRack) == maxCount) {
        System.out.println("racks.count(currentRack) == maxCount");
        if (currentUpstream.getRackId().get().equals(currentRack)) {
          return "";
        } else {
          return configuration.getZeroWeightString();
        }
      } else {
        System.out.println("racks.count(currentRack) != maxCount");
        if (currentUpstream.getRackId().get().equals(currentRack)) {
          return String.format(configuration.getWeightingFormat(), configuration.getSameRackMultiplier());
        }
      }
    }

    return "";
  }

  private Multiset<String> generateAllRacks (Collection<UpstreamInfo> upstreams) {
    Multiset<String> allUpstreams = HashMultiset.create();
    for (UpstreamInfo upstreamInfo : upstreams) {
      if (upstreamInfo.getRackId().isPresent()) {
        allUpstreams.add(upstreamInfo.getRackId().get());
      }
    }
    return allUpstreams;
  }

  private double calculateCapacity(Multiset<String> allRacks) {
    return allRacks.elementSet().size() / (double) allRacks.size();
  }

  private double getTotalPendingLoad(Multiset<String> allRacks, double capacity) {
    double pendingLoad = 0;
    for (String rack: allRacks) {
      final double myLoad = 1.0 / allRacks.count(rack);
      double myPendingLoad = capacity - myLoad;
      if (myPendingLoad > 0) {
        pendingLoad += myPendingLoad;
      }
    }
    return pendingLoad;
  }

  public CharSequence preferSameRackWeightingBalanced(Collection<UpstreamInfo> upstreams, UpstreamInfo currentUpstream, Options options) {
    if (agentMetadata.getEc2().getAvailabilityZone().isPresent() && currentUpstream.getRackId().isPresent()) {

      final String currentRack = agentMetadata.getEc2().getAvailabilityZone().get();
      final String testingRack = currentUpstream.getRackId().get();
      final Multiset<String> allRacks = generateAllRacks(upstreams);

      final double capacity = calculateCapacity(allRacks);
      final double load = 1.0 / allRacks.count(currentRack);

      if (currentRack.equals(testingRack)) {
        if (load < capacity) { return ""; }
        return "weight=" + (int) Math.ceil(capacity * allRacks.size());
      }

      final double pendingLoadInCurrentRack = load - capacity;
      if (pendingLoadInCurrentRack <= 0) { return "backup"; }

      final double extraCapacityInTestingRack = capacity - (1.0 / (allRacks.count(testingRack)));
      if (extraCapacityInTestingRack <= 0) { return "backup"; }

      final double totalPendingLoad = getTotalPendingLoad(allRacks, capacity);
      final double pendingLoadFromCurrentRack = (extraCapacityInTestingRack / totalPendingLoad) * pendingLoadInCurrentRack;
      final int weight = (int) Math.ceil(pendingLoadFromCurrentRack);
      if (weight == 1) { return ""; }
      return "weight=" + weight;
    }
    return null;

  }
}
