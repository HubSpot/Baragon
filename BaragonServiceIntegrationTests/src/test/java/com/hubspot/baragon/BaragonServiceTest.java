package com.hubspot.baragon;

import static org.junit.Assert.assertEquals;

import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;

@RunWith(JukitoRunner.class)
public class BaragonServiceTest {
  public static class Module extends JukitoModule {
    @Override
    protected void configureTest() {
      install(new DockerTestModule());
    }
  }

  @Test
  public void testStateEndpoint() {
    return;
  }
}
