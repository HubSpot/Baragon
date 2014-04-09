package com.hubspot.baragon.lbs;

import com.hubspot.baragon.exceptions.InvalidConfigException;

public interface LbAdapter {
  public void checkConfigs() throws InvalidConfigException;
  public void reloadConfigs();
}
