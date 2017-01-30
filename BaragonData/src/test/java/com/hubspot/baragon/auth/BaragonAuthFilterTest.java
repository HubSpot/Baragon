package com.hubspot.baragon.auth;

import static org.junit.Assert.assertNotNull;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.hubspot.baragon.managers.BaragonAuthManager;

@RunWith(JukitoRunner.class)
public class BaragonAuthFilterTest {

  public static class DataTestModule extends JukitoModule {
    @Override
    protected void configureTest() {
      bindMock(BaragonAuthManager.class);
    }
  }

  @Test
  @Inject
  public void itCanBuildBaragonAuthFilter(BaragonAuthFilter baragonAuthFilter) {
    assertNotNull(baragonAuthFilter);
  }

  @Test
  @Inject
  public void itCanBuildBaragonAuthFeatureInstances(BaragonAuthFeature baragonAuthFeature) {
    assertNotNull(baragonAuthFeature);
  }
}
