package com.hubspot.baragon.service.listeners;

import org.apache.curator.framework.recipes.leader.LeaderLatchListener;

public abstract class AbstractLatchListener implements LeaderLatchListener {
  public abstract boolean isEnabled();
}
