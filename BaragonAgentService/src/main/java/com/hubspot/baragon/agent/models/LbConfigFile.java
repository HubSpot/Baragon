package com.hubspot.baragon.agent.models;

import com.google.common.base.Objects;

public class LbConfigFile {
  private final String fullPath;
  private final String content;

  public LbConfigFile(final String fullPath, final String content) {
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
    return Objects.toStringHelper(LbConfigFile.class)
        .add("fullPath", fullPath)
        .add("content", content)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(fullPath, content);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    LbConfigFile that = (LbConfigFile) o;

    if (!content.equals(that.content)) return false;
    if (!fullPath.equals(that.fullPath)) return false;

    return true;
  }
}
