package com.hubspot.baragon.kubernetes;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.hubspot.baragon.client.BaragonServiceClient;

public class BaragonKubernetesIntegrationTest {
  private static final Logger LOG = LoggerFactory.getLogger(BaragonKubernetesIntegrationTest.class);

  private static BaragonServiceClient mockBaragonCliect = mock(BaragonServiceClient.class);;


  private Injector getInjector() {
    return Guice.createInjector(
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(BaragonServiceClient.class).toInstance(mockBaragonCliect);
          }
        },
        new BaragonKubernetesTestModule());
  }

  @Before
  public void setup() throws Exception {
  }


  @Test
  public void testListening() throws Exception {
    LOG.info("Testing");
    BaragonKubernetesManaged instance = getInjector().getInstance(BaragonKubernetesManaged.class);

    instance.start();
    
    Thread.sleep(30000);
  }
}
