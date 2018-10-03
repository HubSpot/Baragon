package com.hubspot.baragon.service.edgecache.cloudflare.client.models;


import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.google.common.base.Objects;

@JsonInclude(Include.NON_EMPTY)
public class CloudflarePurgeRequest {
  private final List<String> files;
  private final List<String> tags;

  public CloudflarePurgeRequest(List<String> files, List<String> tags) {
    this.files = files;
    this.tags = tags;
  }

  public List<String> getFiles() {
    return files;
  }

  public List<String> getTags() {
    return tags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    CloudflarePurgeRequest that = (CloudflarePurgeRequest) o;
    return java.util.Objects.equals(files, that.files) &&
        java.util.Objects.equals(tags, that.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(files, tags);
  }

  @Override
  public String toString() {
    return "CloudflarePurgeRequest{" +
        "files=" + files +
        ", tags=" + tags +
        '}';
  }
}
