package com.hubspot.baragon.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

@JsonIgnoreProperties( ignoreUnknown = true )
public class BaragonConfigFile {
  private final String fullPath;
  private final String content;

  public BaragonConfigFile(@JsonProperty("fullPath") String fullPath,
                           @JsonProperty("content") String content) {
    this.fullPath = fullPath;
    this.content = content;
  }

  public String getFullPath() {
    return fullPath;
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(BaragonConfigFile.class)
        .add("fullPath", fullPath)
        .add("content", content)
        .toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    BaragonConfigFile that = (BaragonConfigFile) o;

    if (!content.equals(that.content)) {
      return false;
    }
    if (!fullPath.equals(that.fullPath)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = fullPath.hashCode();
    result = 31 * result + content.hashCode();
    return result;
  }
}
