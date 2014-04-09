package com.hubspot.baragon.service;

import com.google.common.collect.Queues;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.baragon.BaragonBaseModule;
import com.hubspot.baragon.config.ZooKeeperConfiguration;
import com.hubspot.baragon.service.config.BaragonConfiguration;
import com.hubspot.baragon.models.BaragonRequest;
import com.ning.http.client.AsyncHttpClient;

import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


public class BaragonServiceModule extends AbstractModule {
  public static final String BARAGON_SERVICE_HTTP_CLIENT = "baragon.service.http.client";
  public static final String BARAGON_SERVICE_SCHEDULED_EXECUTOR = "baragon.service.scheduledExecutor";
  public static final String BARAGON_SERVICE_QUEUE = "baragon.service.queue";

  @Override
  protected void configure() {
    install(new BaragonBaseModule());
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_HTTP_CLIENT)
  public AsyncHttpClient providesHttpClient() {
    AsyncHttpClient.BoundRequestBuilder
  }

  @Provides
  public ZooKeeperConfiguration provideZooKeeperConfiguration(BaragonConfiguration configuration) {
    return configuration.getZooKeeperConfiguration();
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_QUEUE)
  public Queue<BaragonRequest> providesQueue() {
    return Queues.newConcurrentLinkedQueue();
  }

  @Provides
  @Singleton
  @Named(BARAGON_SERVICE_SCHEDULED_EXECUTOR)
  public ScheduledExecutorService providesScheduledExecutor() {
    return Executors.newScheduledThreadPool(1);
  }
}