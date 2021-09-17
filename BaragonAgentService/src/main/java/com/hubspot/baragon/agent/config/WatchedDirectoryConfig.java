package com.hubspot.baragon.agent.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class WatchedDirectoryConfig {
  private String source;
  private Path sourcePath = null;
  private String destination;
  private Path destinationPath;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WatchedDirectoryConfig that = (WatchedDirectoryConfig) o;
    return (
      Objects.equals(source, that.source) && Objects.equals(destination, that.destination)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(source, destination);
  }

  @Override
  public String toString() {
    return (
      "WatchedDirectoryConfig{" +
      "source='" +
      source +
      '\'' +
      ", destination='" +
      destination +
      '\'' +
      '}'
    );
  }
}
