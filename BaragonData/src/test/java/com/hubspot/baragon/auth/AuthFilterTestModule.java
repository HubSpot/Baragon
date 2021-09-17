package com.hubspot.baragon.auth;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.hubspot.baragon.managers.BaragonAuthManager;
import org.mockito.Mockito;

public class AuthFilterTestModule implements Module {

  @Override
  public void configure(Binder binder) {
    binder
      .bind(BaragonAuthManager.class)
      .toInstance(Mockito.mock(BaragonAuthManager.class));
  }
}
