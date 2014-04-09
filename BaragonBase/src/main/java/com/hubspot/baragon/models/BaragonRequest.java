package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;
import com.hubspot.baragon.models.Service;

import java.util.List;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonRequest {
  private final String requestId;

  private final Service service;

  private final List<String> add;
  private final List<String> remove;

  @JsonCreator
  public BaragonRequest(@JsonProperty("requestId") String requestId, @JsonProperty("service") Service service,
                        @JsonProperty("add") List<String> add, @JsonProperty("remove") List<String> remove) {
    this.requestId = requestId;
    this.service = service;
    this.add = add;
    this.remove = remove;
  }

  public String getRequestId() {
    return requestId;
  }

  public Service getService() {
    return service;
  }

  public List<String> getAdd() {
    return add;
  }

  public List<String> getRemove() {
    return remove;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("requestId", requestId)
        .add("service", service)
        .add("add", add)
        .add("remove", remove)
        .toString();
  }
}
