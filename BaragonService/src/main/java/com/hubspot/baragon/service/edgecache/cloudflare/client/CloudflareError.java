package com.hubspot.baragon.service.edgecache.cloudflare.client;

public class CloudflareError {
  private final Integer code;
  private final String message;

  public CloudflareError(Integer code, String message) {
    this.code = code;
    this.message = message;
  }

  public Integer getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
