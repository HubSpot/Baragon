package com.hubspot.baragon.healthchecks;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.*;

public class HealthCheckService {
  private final AsyncHttpClient asyncHttpClient;

  @Inject
  public HealthCheckService(@HealthCheckClient final AsyncHttpClient asyncHttpClient) {
    this.asyncHttpClient = asyncHttpClient;
  }

  public void runHealthChecksAndThrow(final Collection<String> upstreams, final String healthCheckUrl, final int timeoutSeconds) {
    // TODO: need to fix the retry here
    Collection<Future<Boolean>> futureResponses = Lists.newArrayList();
    final Map<String, String> serverErrors = Maps.newConcurrentMap();

    try {
      for (final String upstream : upstreams) {
        futureResponses.add(asyncHttpClient.prepareGet(String.format("http://%s%s", upstream, healthCheckUrl)).execute(new AsyncCompletionHandler<Boolean>() {
          @Override
          public Boolean onCompleted(Response response) throws Exception {
            boolean success = response.getStatusCode() >= 200 && response.getStatusCode() < 300;
            if (!success) {
              serverErrors.put(upstream, response.getResponseBody());
            }
            return success;
          }
        }));
      }

      for (Future<Boolean> futureResponse : futureResponses) {
        try {
          futureResponse.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          throw Throwables.propagate(e);
        } catch (InterruptedException e) {
          throw Throwables.propagate(e);
        } catch (ExecutionException e) {
          throw Throwables.propagate(e);
        }
      }
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

    if (!serverErrors.isEmpty()) {
      throw new HealthCheckException(serverErrors);
    }
  }
}
