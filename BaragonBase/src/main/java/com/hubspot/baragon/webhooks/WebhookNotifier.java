package com.hubspot.baragon.webhooks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hubspot.baragon.data.BaragonDataStore;
import com.ning.http.client.AsyncHttpClient;

@Singleton
public class WebhookNotifier {
  private final AsyncHttpClient asyncHttpClient;
  private final BaragonDataStore datastore;
  private final ObjectMapper objectMapper;

  @Inject
  public WebhookNotifier(AsyncHttpClient asyncHttpClient, BaragonDataStore datastore, ObjectMapper objectMapper) {
    this.asyncHttpClient = asyncHttpClient;
    this.datastore = datastore;
    this.objectMapper = objectMapper;
  }

  public void notify(WebhookEvent event) {
    try {
      byte[] data = objectMapper.writeValueAsBytes(event);

      for (String url : datastore.getWebhooks()) {
        asyncHttpClient.preparePost(url)
            .addHeader("Content-Type", "application/json")
            .setBody(data)
            .execute();
      }
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }
}
