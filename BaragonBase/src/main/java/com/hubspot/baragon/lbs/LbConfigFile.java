package com.hubspot.baragon.lbs;

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
  
  public String toString() {
    return String.format("<LbConfigFile:%s>", getFullPath());
  }
}
