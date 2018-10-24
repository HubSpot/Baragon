package com.hubspot.baragon.agent.handlebars;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.hubspot.baragon.models.UpstreamInfo;

public class RackMethodsHelper {

  /**
   * @return the rack ids of all the upstreams
   */
  public List<String> generateAllRacks(Collection<UpstreamInfo> upstreams) {
    List<String> allRacks = new ArrayList<>();
    for (UpstreamInfo upstreamInfo : upstreams) {
      if (upstreamInfo.getRackId().isPresent()) {
        allRacks.add(upstreamInfo.getRackId().get());
      }
    }
    return allRacks;
  }

  /**
   * @param allRacks
   * @return the capacity that each upstream should handle
   */
  public BigDecimal calculateCapacity(List<String> allRacks) {
    /* capacity is the proportion of unique upstreams to all the upstreams - this gives the amount of load that each upstreams should carry*/
    return new BigDecimal((new HashSet<>(allRacks)).size()).divide(new BigDecimal(allRacks.size()), 10, BigDecimal.ROUND_HALF_UP);
  }

  /**
   * @param allRacks
   * @return the total pending load that have to be distributed to other upstreams
   */
  public BigDecimal getTotalPendingLoad(List<String> allRacks) {
    final BigDecimal capacity = calculateCapacity(allRacks);
    BigDecimal pendingLoad = BigDecimal.ZERO;
    for (String rack : allRacks) {
      final BigDecimal myLoad = getReciprocal(new BigDecimal(Collections.frequency(allRacks, rack)));
      final BigDecimal myPendingLoad = capacity.subtract(myLoad);
      if (myPendingLoad.compareTo(BigDecimal.ZERO) == 1) { // pending load is greater than 0
        pendingLoad = pendingLoad.add(myPendingLoad);
      }
    }
    return pendingLoad;
  }

  public BigDecimal getReciprocal(BigDecimal number) {
    return (new BigDecimal(1)).divide(number, 10, BigDecimal.ROUND_HALF_UP);
  }
}
