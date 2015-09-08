package com.hubspot.baragon.client;

import java.util.Collections;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Stage;
import org.junit.Test;

public class BaragonServiceClientModuleTest {

  @Inject
  BaragonServiceClient client;

  @Test
  public void testModuleWithHosts() {
    final Injector injector = Guice.createInjector(Stage.PRODUCTION,
            new GuiceDisableModule(),
            new BaragonClientModule(Collections.singletonList("http://example.com")));

    injector.injectMembers(this);
  }

  private static class GuiceDisableModule extends AbstractModule {
    @Override
    protected void configure()
    {
      binder().disableCircularProxies();
      binder().requireExplicitBindings();
    }
  }
}

