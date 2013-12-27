package com.hubspot.baragon.utils;

import com.ning.http.client.Response;

public class ResponseUtils {
  public static boolean isSuccess(Response response) {
    if (response == null) {
      return false;
    }

    return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
  }
}
