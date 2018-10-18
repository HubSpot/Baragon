package com.hubspot.baragon.agent.handlebars;

import java.util.Collection;

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

  public CharSequence preferSameRackWeighting(Collection<UpstreamInfo> upstreams, UpstreamInfo currentUpstream, Options options) {
    if (agentMetadata.getEc2().getAvailabilityZone().isPresent() && currentUpstream.getRackId().isPresent()) {
      String currentRack = agentMetadata.getEc2().getAvailabilityZone().get();
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
      if (racks.count(currentRack) == 0) {
        return "";
      }

      if (racks.count(currentRack) == maxCount) {
        if (currentUpstream.getRackId().get().equals(currentRack)) {
          return "";
        } else {
          return configuration.getZeroWeightString();
        }
      } else {
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
      double myLoad = 1.0 / allRacks.count(rack);
      double myPendingLoad = capacity - myLoad;
      if (myPendingLoad > 0) {
        pendingLoad += myPendingLoad;
      }
    }
    return pendingLoad;
  }

  public CharSequence preferSameRackWeightingBalanced(Collection<UpstreamInfo> upstreams, UpstreamInfo currentUpstream, Options options) {
    if (agentMetadata.getEc2().getAvailabilityZone().isPresent() && currentUpstream.getRackId().isPresent()) {

      String currentRack = agentMetadata.getEc2().getAvailabilityZone().get();
      String testingRack = currentUpstream.getRackId().get();
      Multiset<String> allRacks = generateAllRacks(upstreams);

      double capacity = calculateCapacity(allRacks);
      double load = 1.0 / allRacks.count(currentRack);

      if (currentRack == testingRack) {
        if (load < capacity) { return ""; }
        return "weight=" + (int) Math.ceil(capacity * allRacks.size());
      }

      double pendingLoadInCurrentRack = load - capacity;
      if (pendingLoadInCurrentRack <= 0) { return "backup"; }

      double extraCapacityInTestingRack = capacity - (1.0 / (allRacks.count(testingRack)));
      if (extraCapacityInTestingRack <= 0) { return "backup"; }

      double totalPendingLoad = getTotalPendingLoad(allRacks, capacity);
      double pendingLoadFromCurrentRack = (extraCapacityInTestingRack / totalPendingLoad) * pendingLoadInCurrentRack;
      int weight = (int) Math.ceil(pendingLoadFromCurrentRack);
      if (weight == 1) { return ""; }
      return "weight=" + weight;
    }
    return null;

  }

}
