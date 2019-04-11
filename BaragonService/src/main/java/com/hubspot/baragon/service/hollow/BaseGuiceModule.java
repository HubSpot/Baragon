package com.hubspot.baragon.service.hollow;

import com.google.inject.AbstractModule;

public abstract class BaseGuiceModule extends AbstractModule {
  public BaseGuiceModule() {

  }

  protected abstract void configure();

  public boolean equals(Object o) {
    return o != null && this.getClass().equals(o.getClass());
  }

  public int hashCode() {
    return this.getClass().hashCode();
  }
}
