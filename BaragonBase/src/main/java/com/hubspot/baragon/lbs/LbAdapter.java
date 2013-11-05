package com.hubspot.baragon.lbs;

/**
 * A LbAdapter provides an interface for manipulating a load balancer.
 * @author tpetr
 *
 */
public interface LbAdapter {
  public void checkConfigs();
  public void reloadConfigs();
}
