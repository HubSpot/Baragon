package com.hubspot.baragon.service.edgecache.cloudflare.client.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

@JsonIgnoreProperties( ignoreUnknown = true )
public class CloudflareError {
  private final Integer code;
  private final String message;

  @JsonCreator
  public CloudflareError(@JsonProperty("code") Integer code, @JsonProperty("message") String message) {
    this.code = code;
    this.message = message;
  }

  public Integer getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CloudflareError that = (CloudflareError) o;
    return Objects.equal(code, that.code) &&
        Objects.equal(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(code, message);
  }

  @Override
  public String toString() {
    return "CloudflareError{" +
        "code=" + code +
        ", message='" + message + '\'' +
        '}';
  }
}
