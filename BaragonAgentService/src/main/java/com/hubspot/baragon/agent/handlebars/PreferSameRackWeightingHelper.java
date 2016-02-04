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
}
