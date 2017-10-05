package com.hubspot.baragon.service.edgecache.cloudflare.client.models;

import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CloudflareResponse<?> that = (CloudflareResponse<?>) o;
    return Objects.equal(success, that.success) &&
        Objects.equal(errors, that.errors) &&
        Objects.equal(messages, that.messages) &&
        Objects.equal(resultInfo, that.resultInfo) &&
        Objects.equal(result, that.result);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(success, errors, messages, resultInfo, result);
  }

  @Override
  public String toString() {
    return "CloudflareResponse{" +
        "success=" + success +
        ", errors=" + errors +
        ", messages=" + messages +
        ", resultInfo=" + resultInfo +
        ", result=" + result +
        '}';
  }
}
