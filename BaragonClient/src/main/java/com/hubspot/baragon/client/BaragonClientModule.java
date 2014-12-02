package com.hubspot.baragon.client;

import java.util.List;

import org.apache.curator.framework.CuratorFramework;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpConfig;
import com.hubspot.horizon.ning.NingHttpClient;
import com.hubspot.baragon.BaragonBaseModule;


public class BaragonClientModule extends AbstractModule {
  public static final String HTTP_CLIENT_NAME = "baragon.http.client";

  // eg: http://localhost:5060,http://localhost:7000
  public static final String HOSTS_PROPERTY_NAME = "baragon.hosts";

  // bind this to provide the path for baragon eg: baragon/v2
  public static final String CONTEXT_PATH = "baragon.context.path";

  private final List<String> hosts;

  public BaragonClientModule() {
    this(null);
  }

  public BaragonClientModule(List<String> hosts) {
    this.hosts = hosts;
  }

  @Override
  protected void configure() {
    ObjectMapper objectMapper = new ObjectMapper();

    HttpClient httpClient = new NingHttpClient(HttpConfig.newBuilder().setObjectMapper(objectMapper).build());
    bind(HttpClient.class).annotatedWith(Names.named(HTTP_CLIENT_NAME)).toInstance(httpClient);

    bind(BaragonClient.class).toProvider(BaragonClientProvider.class).in(Scopes.SINGLETON);

    if (hosts != null) {
      bindHosts(binder()).toInstance(hosts);
    }
  }

  public static LinkedBindingBuilder<List<String>> bindHosts(Binder binder) {
    return binder.bind(new TypeLiteral<List<String>>() {}).annotatedWith(Names.named(HOSTS_PROPERTY_NAME));
  }

  public static LinkedBindingBuilder<String> bindContextPath(Binder binder) {
    return binder.bind(String.class).annotatedWith(Names.named(CONTEXT_PATH));
  }
}
