package com.hubspot.baragon.agent.config;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.common.base.Optional;

public class WatchedDirectoryConfig {
  private String source;
  private Path sourcePath = null;
  private String destination;
  private Path destinationPath;
  private Optional<String> matcher;

  public String getSource() {
    return source;
  }

  public Path getSourceAsPath() {
    return sourcePath;
  }

  public void setSource(String source) {
    this.source = source;
    this.sourcePath = Paths.get(source);
  }

  public String getDestination() {
    return destination;
  }

  public Path getDestinationAsPath() {
    return destinationPath;
  }

  public void setDestination(String destination) {
    this.destination = destination;
    this.destinationPath = Paths.get(destination);
  }

  public Optional<String> getMatcher() {
    return matcher;
  }

  public void setMatcher(Optional<String> matcher) {
    this.matcher = matcher;
  }
}
