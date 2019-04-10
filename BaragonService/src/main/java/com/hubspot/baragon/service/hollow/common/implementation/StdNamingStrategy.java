package com.hubspot.baragon.service.hollow.common.implementation;

import java.nio.file.Paths;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hubspot.baragon.service.hollow.common.NamingStrategy;
import com.netflix.hollow.core.memory.encoding.HashCodes;

public class StdNamingStrategy implements NamingStrategy {
  private static final String SNAPSHOT = "snapshot";
  private static final String DELTA = "delta";
  private static final String REVERSE_DELTA = "reverse-delta";
  private static final String VERSION_REGEX = "/([0-9a-f]+)-(\\d+)";
  private static final String TYPE_REGEX = "(" + SNAPSHOT + "|" + DELTA + "|" + REVERSE_DELTA + ")";

  private final Pattern snapshotPattern;
  private final Pattern deltaPattern;
  private final Pattern reverseDeltaPattern;
  private final Pattern findVersionPattern;
  private final String prefix;

  public StdNamingStrategy() {
    this("");
  }

  public StdNamingStrategy(String prefix) {
    this.prefix = prefix;
    this.snapshotPattern = Pattern.compile(Paths.get(prefix, SNAPSHOT) + VERSION_REGEX);
    this.deltaPattern = Pattern.compile(Paths.get(prefix, DELTA) + VERSION_REGEX);
    this.reverseDeltaPattern = Pattern.compile(Paths.get(prefix, REVERSE_DELTA) + VERSION_REGEX);
    this.findVersionPattern = Pattern.compile(Paths.get(prefix, TYPE_REGEX) + VERSION_REGEX);
  }

  @Override
  public String getIndexName() {
    return Paths.get(prefix, "snapshot.index").toString();
  }

  @Override
  public String getAnnouncedVersionName() {
    return Paths.get(prefix, "announced.version").toString();
  }

  @Override
  public String getSnapshotName(long version) {
    return resolveName(SNAPSHOT, version);
  }

  @Override
  public String getDeltaName(long version) {
    return resolveName(DELTA, version);
  }

  @Override
  public String getReverseDeltaName(long version) {
    return resolveName(REVERSE_DELTA, version);
  }

  @Override
  public boolean isSnapshot(String name) {
    return snapshotPattern.matcher(name).matches();
  }

  @Override
  public boolean isDelta(String name) {
    return deltaPattern.matcher(name).matches();
  }

  @Override
  public boolean isReverseDelta(String name) {
    return reverseDeltaPattern.matcher(name).matches();
  }

  @Override
  public String getSnapshotPrefix() {
    return Paths.get(prefix, SNAPSHOT).toString();
  }

  @Override
  public String getDeltaPrefix() {
    return Paths.get(prefix, DELTA).toString();
  }

  @Override
  public String getReverseDeltaPrefix() {
    return Paths.get(prefix, REVERSE_DELTA).toString();
  }

  @Override
  public Optional<Long> getVersion(String name) {
    Matcher matcher = findVersionPattern.matcher(name);
    if (!matcher.find()) {
      return Optional.empty();
    }

    if (matcher.groupCount() != 3) {
      return Optional.empty();
    }

    return Optional.of(matcher.group(3))
        .map(Long::parseLong);
  }

  private String resolveName(String type, long version) {
    return Paths.get(prefix, type, toVersionString(version)).toString();
  }

  private String toVersionString(long version) {
    return String.format("%s-%s", Integer.toHexString(HashCodes.hashLong(version)), version);
  }
}
