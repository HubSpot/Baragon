package com.hubspot.baragon;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hubspot.baragon.client.BaragonServiceClient;
import com.hubspot.baragon.models.BaragonServiceStatus;
import org.jboss.netty.util.internal.SystemPropertyUtil;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;

import static org.junit.Assert.*;

public class BaragonServiceTestIT {

  //@Inject BaragonServiceClient baragonServiceClient;

  private Injector getInjector() {
    return Guice.createInjector(new DockerTestModule());
  }

  @Test
   public void testStatus() throws Exception {
    BaragonServiceClient baragonServiceClient = getInjector().getInstance(BaragonServiceClient.class);
    Optional<BaragonServiceStatus> status = baragonServiceClient.getAnyBaragonServiceStatus();
    assertTrue(status.get().isLeader());
  }

  @Test
  public void testState() throws Exception {
    //baragonServiceClient.getGlobalState();
    return;
  }

  /*@Test
  public void testGetService() throws Exception {
    BaragonServiceStatus status = baragonServiceClient.getAnyBaragonServiceStatus().get();
    status.
    return;
  }*/

  @Test
  public void testWorkers() throws Exception {
    BaragonServiceClient baragonServiceClient = getInjector().getInstance(BaragonServiceClient.class);
    assertFalse(baragonServiceClient.getBaragonServiceWorkers().isEmpty());
  }
}
