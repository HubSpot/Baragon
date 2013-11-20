package com.hubspot.baragon.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.hubspot.jackson.datatype.protobuf.ProtobufModule;
import com.ning.http.client.AsyncHttpClient;

public class BaragonClientModule extends AbstractModule {
  public static final String BASE_URL_NAME = "baragon.client.baseUrl";
  public static final String OBJECT_MAPPER_NAME = "baragon.client.objectMapper";
  public static final String ASYNC_HTTP_CLIENT_NAME = "baragon.client.asyncHttpClient";

  @Override
  protected void configure() {
    bind(AsyncHttpClient.class).annotatedWith(Names.named(ASYNC_HTTP_CLIENT_NAME)).toInstance(new AsyncHttpClient());
    bind(ObjectMapper.class).annotatedWith(Names.named(OBJECT_MAPPER_NAME)).toInstance(new ObjectMapper()
      .setSerializationInclusion(JsonInclude.Include.NON_NULL)
      .registerModule(new ProtobufModule()));
  }
}
