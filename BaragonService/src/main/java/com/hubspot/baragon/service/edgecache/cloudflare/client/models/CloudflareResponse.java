package com.hubspot.baragon.service.edgecache.cloudflare.client.models;

import java.util.List;

import javax.annotation.Nullable;

public abstract class CloudflareResponse<T> {
  private final Boolean success;
  private final List<CloudflareError> errors;
  private final List<String> messages;
  private final CloudflareResultInfo resultInfo;
  private final T result;

  public CloudflareResponse(Boolean success,
                            List<CloudflareError> errors,
                            List<String> messages,
                            CloudflareResultInfo resultInfo,
                            T result) {
    this.success = success;
    this.errors = errors;
    this.messages = messages;
    this.resultInfo = resultInfo;
    this.result = result;
  }

  public Boolean isSuccess() {
    return success;
  }

  public List<CloudflareError> getErrors() {
    return errors;
  }

  public List<String> getMessages() {
    return messages;
  }

  @Nullable
  public CloudflareResultInfo getResultInfo() {
    return resultInfo;
  }

  public T getResult() {
    return result;
  }
}
