package com.hubspot.baragon.service.hollow.datamodel;

import java.util.Collection;

import com.hubspot.baragon.models.BaragonServiceState;
import com.netflix.hollow.core.write.objectmapper.HollowPrimaryKey;

@HollowPrimaryKey(fields = "version")
public class BaragonHollowState {

  private final Collection<BaragonServiceState> states;
  private final int version;

  public BaragonHollowState(Collection<BaragonServiceState> states, int version) {
    this.states = states;
    this.version = version;
  }

  public Collection<BaragonServiceState> getServiceStates() {
      return states;
    }

  public int getVersion() {
    return version;
  }

}
