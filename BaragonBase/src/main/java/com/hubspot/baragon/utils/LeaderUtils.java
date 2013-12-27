package com.hubspot.baragon.utils;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.apache.curator.framework.recipes.leader.Participant;

import java.util.Collection;

public class LeaderUtils {
  private LeaderUtils() { }

  public static Collection<String> getParticipantIds(LeaderLatch leaderLatch){
    try {
      final Collection<Participant> participants = leaderLatch.getParticipants();

      final Collection<String> results = Lists.newArrayListWithCapacity(participants.size());

      for (Participant p : participants) {
        results.add(p.getId());
      }

      return results;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
