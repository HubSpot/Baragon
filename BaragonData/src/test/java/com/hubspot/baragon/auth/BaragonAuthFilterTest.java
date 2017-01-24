package com.hubspot.baragon.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.inject.Inject;
import com.hubspot.baragon.auth.BaragonAuthFeature.BaragonAuthFilter;
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
  public void itCanBuildBaragonAuthManagerInstances(BaragonAuthManager baragonAuthManager) {
    assertThat(baragonAuthManager)
        .isNotNull();
  }

  @Test
  @Inject
  public void itCanBuildBaragonAuthFilter(BaragonAuthFilter baragonAuthFilter) {
    assertThat(baragonAuthFilter)
        .isNotNull();
  }

  @Test
  @Inject
  public void itCanBuildBaragonAuthFeatureInstances(BaragonAuthFeature baragonAuthFeature) {
    assertThat(baragonAuthFeature)
        .isNotNull();
  }
}
