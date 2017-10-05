package com.hubspot.baragon.service.edgecache.cloudflare.client;

public class CloudflareClientException extends Exception {

  public CloudflareClientException(String message, Throwable t) {
    super(message, t);
  }

  public CloudflareClientException(String message) {
    super(message);
  }

}
