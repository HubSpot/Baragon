package com.hubspot.baragon.client;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.google.common.base.Optional;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.hubspot.horizon.HttpClient;
import com.hubspot.horizon.HttpConfig;
import com.hubspot.horizon.ning.NingHttpClient;


public class BaragonClientModule extends AbstractModule {
  public static final String HTTP_CLIENT_NAME = "baragon.http.client";

  // eg: http://localhost:5060,http://localhost:7000
  public static final String HOSTS_PROPERTY_NAME = "baragon.hosts";

  // bind this to provide the path for baragon eg: baragon/v2
  public static final String CONTEXT_PATH_PROPERTY_NAME = "baragon.context.path";

  // bind this to provide the authkey for baragon
  public static final String AUTHKEY_PROPERTY_NAME = "baragon.authkey";

  private final List<String> hosts;

  public BaragonClientModule() {
    this(null);
  }

  public BaragonClientModule(List<String> hosts) {
    this.hosts = hosts;
  }

  private ObjectMapper buildObjectMapper() {
    final ObjectMapper objectMapper = new ObjectMapper();

    objectMapper.registerModule(new GuavaModule());

    return objectMapper;
  }

  @Override
  protected void configure() {
    bind(BaragonServiceClient.class).toProvider(BaragonClientProvider.class).in(Scopes.SINGLETON);

    if (hosts != null) {
      bindHosts(binder()).toInstance(hosts);
    }
  }

  @Provides
  @Named(HTTP_CLIENT_NAME)
  @Singleton
  HttpClient providesHttpClient() {
    return new NingHttpClient(
        HttpConfig.newBuilder()
            .setObjectMapper(buildObjectMapper())
            .build()
    );
  }

  public static LinkedBindingBuilder<List<String>> bindHosts(Binder binder) {
    return binder.bind(new TypeLiteral<List<String>>() {}).annotatedWith(Names.named(HOSTS_PROPERTY_NAME));
  }

  public static LinkedBindingBuilder<String> bindContextPath(Binder binder) {
    return binder.bind(String.class).annotatedWith(Names.named(CONTEXT_PATH_PROPERTY_NAME));
  }

  public static LinkedBindingBuilder<Optional<String>> bindAuthkey(Binder binder) {
    return binder.bind(new TypeLiteral<Optional<String>>() {}).annotatedWith(Names.named(AUTHKEY_PROPERTY_NAME));
  }
}
