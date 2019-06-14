package com.hubspot.baragon.models;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

@JsonIgnoreProperties( ignoreUnknown = true )
public class UpstreamInfo {
  public static final String DEFAULT_GROUP = "default";

  public static boolean upstreamAndGroupMatches(UpstreamInfo a, UpstreamInfo b) {
    return a.getUpstream().equals(b.getUpstream()) && a.getGroup().equals(b.getGroup());
  }

  private final String upstream;

  private final String resolvedUpstream;

  @Size(max=250)
  @Pattern(regexp = "^$|[^\\s/|]+", message = "cannot contain whitespace, '/', or '|'")
  private final String requestId;

  @Size(max=100)
  @Pattern(regexp = "^$|[^\\s|]+", message = "cannot contain whitespace, '/', or '|'")
  private final String rackId;
  private final Optional<String> originalPath;

  @Size(max=100)
  @Pattern(regexp = "^$|[^\\s|]+", message = "cannot contain whitespace, '/', or '|'")
  private final String group;

  @JsonCreator
  public static UpstreamInfo fromString(String value) {
    String[] split = value.split("\\|", -1);
    if (split[0].contains(".") && split.length == 1) {
      return fromUnEncodedString(value);
    } else {
      String upstream = new String(BaseEncoding.base64Url().decode(split[0]), Charsets.UTF_8);
      Optional<String> requestId = split.length > 1 && !split[1].equals("") ? Optional.of(split[1]) : Optional.<String>absent();
      Optional<String> rackId = split.length > 2 && !split[2].equals("") ? Optional.of(split[2]) : Optional.<String>absent();
      Optional<String> group = split.length > 3 && !split[3].equals("") ? Optional.of(split[3]) : Optional.<String>absent();
      Optional<String> resolvedUpstream = split.length > 4 && !split[4].equals("") ? Optional.of(split[4]) : Optional.<String>absent();
      return new UpstreamInfo(upstream, requestId, rackId, Optional.of(value), group, resolvedUpstream);
    }
  }

  public static UpstreamInfo fromUnEncodedString(String upstream) {
    return new UpstreamInfo(upstream, Optional.<String>absent(), Optional.<String>absent(), Optional.of(upstream), Optional.<String>absent(), Optional.absent());
  }

  public UpstreamInfo(String upstream, Optional<String> requestId, Optional<String> rackId) {
    this(upstream, requestId, rackId, Optional.<String>absent(), Optional.<String>absent(), Optional.absent());
  }

  public UpstreamInfo(String upstream, Optional<String> requestId, Optional<String> rackId, Optional<String> group) {
    this(upstream, requestId, rackId, Optional.<String>absent(), group, Optional.absent());
  }

  public UpstreamInfo(String upstream, Optional<String> requestId, Optional<String> rackId, Optional<String> originalPath, Optional<String> group) {
    this(upstream, requestId, rackId, originalPath, group, Optional.absent());
  }

  @JsonCreator
  public UpstreamInfo(@JsonProperty("upstream") String upstream,
                      @JsonProperty("requestId") Optional<String> requestId,
                      @JsonProperty("rackId") Optional<String> rackId,
                      @JsonProperty("originalPath") Optional<String> originalPath,
                      @JsonProperty("group") Optional<String> group,
                      @JsonProperty("resolvedUpstream") Optional<String> resolvedUpstream) {
    this.upstream = upstream;
    this.requestId = requestId.or("");
    this.rackId = rackId.or("");
    this.originalPath = originalPath;
    this.group = group.or(DEFAULT_GROUP);
    this.resolvedUpstream = resolvedUpstream.or("");
  }

  public String getUpstream() {
    return upstream;
  }

  public Optional<String> getResolvedUpstream() {
    return Strings.isNullOrEmpty(resolvedUpstream) ? Optional.<String>absent() : Optional.of(resolvedUpstream);
  }

  public Optional<String> getRequestId() {
    return Strings.isNullOrEmpty(requestId) ? Optional.<String>absent() : Optional.of(requestId);
  }

  public Optional<String> getRackId() {
    return Strings.isNullOrEmpty(rackId) ? Optional.<String>absent() : Optional.of(rackId);
  }

  @JsonIgnore
  public Optional<String> getOriginalPath() {
    return originalPath;
  }

  public String getGroup() {
    return group;
  }

  @Override
  public String toString() {
    return upstream;
  }

  public String toPath() {
    return String.format("%s|%s|%s|%s|%s", sanitizeUpstream(upstream), requestId, rackId, group, resolvedUpstream);
  }

  protected String sanitizeUpstream(String name) {
    return BaseEncoding.base64Url().encode(name.getBytes(Charsets.UTF_8));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    UpstreamInfo that = (UpstreamInfo) o;

    if (!rackId.equals(that.rackId)) {
      return false;
    }
    if (!requestId.equals(that.requestId)) {
      return false;
    }
    if (!upstream.equals(that.upstream)) {
      return false;
    }
    if (!resolvedUpstream.equals(that.resolvedUpstream)) {
      return false;
    }
    if (!originalPath.equals(that.originalPath)) {
      return false;
    }
    if (!group.equals(that.group)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = upstream.hashCode();
    result = 31 * result + resolvedUpstream.hashCode();
    result = 31 * result + requestId.hashCode();
    result = 31 * result + rackId.hashCode();
    result = 31 * result + originalPath.hashCode();
    result = 31 * result + group.hashCode();
    return result;
  }
}
