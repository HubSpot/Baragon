package com.hubspot.baragon.agent.handlebars;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.github.jknack.handlebars.Options;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.hubspot.baragon.agent.config.BaragonAgentConfiguration;
import com.hubspot.baragon.agent.handlebars.RackMethodsHelper.RackMethods;
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

  /**
   *
   * @param weight
   * @return a string representing the weight, based on the big decimal weight input
   */
  public String getWeight(BigDecimal weight) {
    if (weight.intValue() == 1) {
      return "";
    }
    if (weight.intValue() == 0) {
      return configuration.getZeroWeightString();
    }
    return String.format(configuration.getWeightingFormat(), weight.intValue());
  }


  public CharSequence preferSameRackWeighting(Collection<UpstreamInfo> upstreams, UpstreamInfo currentUpstream, Options options) {
    final RackMethods rackHelper = new RackMethodsHelper.RackMethods(upstreams);
    final List<String> allRacks = rackHelper.generateAllRacks();
    final BigDecimal totalPendingLoad = rackHelper.getTotalPendingLoad(allRacks);
    final BigDecimal capacity = rackHelper.calculateCapacity(allRacks);
    return preferSameRackWeightingOperation(upstreams, currentUpstream, allRacks, capacity, totalPendingLoad, null);
  }

  /**
   *
   * @param upstreams
   * @param currentUpstream
   * @param options
   * @return the weight of the current upstream relative to the current rack such that each upstream carries an equal load.
   *
   * Addressing github issue: https://github.com/HubSpot/Baragon/pull/270
   * Calculating weights for services such that, even if AZ distribution is uneven among upstreams, they still get an even distribution of traffic
   */
  public CharSequence preferSameRackWeightingOperation(Collection<UpstreamInfo> upstreams,
                                                      UpstreamInfo currentUpstream,
                                                      List<String> allRacks,
                                                      BigDecimal capacity,
                                                      BigDecimal totalPendingLoad,
                                                      Options options) {

    if (agentMetadata.getEc2().getAvailabilityZone().isPresent() && currentUpstream.getRackId().isPresent()) {

      final RackMethods rackHelper = new RackMethodsHelper.RackMethods(upstreams);

      final String currentRack = agentMetadata.getEc2().getAvailabilityZone().get();
      final String testingRack = currentUpstream.getRackId().get();

      final BigDecimal countOfAllRacks = new BigDecimal(allRacks.size());
      final BigDecimal countOfCurrentRack = new BigDecimal(Collections.frequency(allRacks, currentRack));
      final BigDecimal countOfTestingRack = new BigDecimal((Collections.frequency(allRacks, testingRack))); // assume this is always in upstream

      if (countOfCurrentRack.intValue() == 0) { // distribute equally to all testing racks if currentRack is not in upstreams
        return "";
      }

      final BigDecimal load = rackHelper.getReciprocal(countOfCurrentRack);

      if (currentRack.equals(testingRack)) {
        if (load.compareTo(capacity) == -1) { // load is less than capacity
          return "";
        }
        final BigDecimal weight = capacity.multiply(countOfAllRacks).multiply(countOfTestingRack);
        return getWeight(weight);
      }

      final BigDecimal pendingLoadInCurrentRack = load.subtract(capacity);
      if (pendingLoadInCurrentRack.compareTo(BigDecimal.ZERO) < 1) {  // pendingLoadInCurrentRack <= 0
        return configuration.getZeroWeightString();
      }

      final BigDecimal loadInTestingRackFromItself = rackHelper.getReciprocal(countOfTestingRack);
      final BigDecimal extraCapacityInTestingRack = capacity.subtract(loadInTestingRackFromItself);
      if (extraCapacityInTestingRack.compareTo(BigDecimal.ZERO) < 1) { //extraCapacityInTestingRack <= 0
        return configuration.getZeroWeightString();
      }

      final BigDecimal pendingLoadFromCurrentRackToTestingRack = (extraCapacityInTestingRack.divide(totalPendingLoad, 10, BigDecimal.ROUND_HALF_UP)).multiply(pendingLoadInCurrentRack);
      final BigDecimal weight = pendingLoadFromCurrentRackToTestingRack.multiply(countOfAllRacks).multiply(countOfTestingRack);
      return getWeight(weight);
    }
    return ""; // If the required data isn't present for some reason, send even traffic everywhere (i.e. everything has a weight of 1)
  }

}
