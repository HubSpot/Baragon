package com.hubspot.baragon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.hubspot.baragon.client.BaragonServiceClient;
import com.hubspot.baragon.models.BaragonServiceStatus;
import org.jukito.JukitoModule;
import org.jukito.JukitoRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Optional;

@RunWith(JukitoRunner.class)
public class BaragonServiceTest {

  //@Inject BaragonServiceClient baragonServiceClient;

  public static class Module extends JukitoModule {
    @Override
    protected void configureTest() {
      install(new DockerTestModule());
    }
  }

  @Test
  public void testStatus(BaragonServiceClient baragonServiceClient) {
    BaragonServiceStatus status = baragonServiceClient.getAnyBaragonServiceStatus().get();
    assertTrue(status.isLeader());
    return;
  }
}
