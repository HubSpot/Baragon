package com.hubspot.baragon.auth;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.google.inject.Inject;
import name.falgout.jeffrey.testing.junit.guice.GuiceExtension;
import name.falgout.jeffrey.testing.junit.guice.IncludeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(GuiceExtension.class)
@IncludeModule(AuthFilterTestModule.class)
public class BaragonAuthFilterTest {

  @Test
  @Inject
  public void itCanBuildBaragonAuthFilter(BaragonAuthFilter baragonAuthFilter) {
    assertNotNull(baragonAuthFilter);
  }

  @Test
  @Inject
  public void itCanBuildBaragonAuthFeatureInstances(
    BaragonAuthFeature baragonAuthFeature
  ) {
    assertNotNull(baragonAuthFeature);
  }
}
