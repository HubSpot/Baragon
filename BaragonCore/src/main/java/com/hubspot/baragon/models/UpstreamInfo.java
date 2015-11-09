package com.hubspot.baragon.models;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;

@JsonIgnoreProperties( ignoreUnknown = true )
public class UpstreamInfo {
  private final String upstream;

  @Size(max=250)
  @Pattern(regexp = "[^\\s/|]+", message = "cannot contain whitespace, '/', or '|'")
  private final String requestId;

  @Size(max=100)
  @Pattern(regexp = "[^\\s/|]+", message = "cannot contain whitespace, '/', or '|'")
  private final String rackId;
  private final Optional<String> originalPath;

  @JsonCreator
  public static UpstreamInfo fromString(String value) {
    String[] split = value.split("\\|", -1);
    if (split[0].contains(".") && split.length == 1) {
      return fromUnEncodedString(value);
    } else {
      String upstream = new String(BaseEncoding.base64Url().decode(split[0]), Charsets.UTF_8);
      Optional<String> requestId = split.length > 1 && !split[1].equals("") ? Optional.of(split[1]) : Optional.<String>absent();
      Optional<String> rackId = split.length > 2 && !split[2].equals("") ? Optional.of(split[2]) : Optional.<String>absent();
      return new UpstreamInfo(upstream, requestId, rackId, Optional.of(value));
    }
  }

  public static UpstreamInfo fromUnEncodedString(String upstream) {
    return new UpstreamInfo(upstream, Optional.<String>absent(), Optional.<String>absent(), Optional.of(upstream));
  }

  public UpstreamInfo (String upstream, Optional<String> requestId, Optional<String> rackId) {
    this(upstream, requestId, rackId, Optional.<String>absent());
  }

  @JsonCreator
  public UpstreamInfo(@JsonProperty("upstream") String upstream,
                      @JsonProperty("requestId") Optional<String> requestId,
                      @JsonProperty("rackId") Optional<String> rackId,
                      @JsonProperty("originalPath") Optional<String> originalPath) {
    this.upstream = upstream;
    this.requestId = requestId.or("");
    this.rackId = rackId.or("");
    this.originalPath = originalPath;
  }

  public String getUpstream() {
    return upstream;
  }

  public Optional<String> getRequestId() {
    return Strings.isNullOrEmpty(requestId) ? Optional.<String>absent() : Optional.of(requestId);
  }

  public Optional<String> getRackId() {
    return Strings.isNullOrEmpty(rackId) ? Optional.<String>absent() : Optional.of(rackId);
  }

  public Optional<String> getOriginalPath() {
    return originalPath;
  }

  @Override
  public String toString() {
    return upstream;
  }

  public String toPath() {
    return String.format("%s|%s|%s", sanitizeUpstream(upstream), requestId, rackId);
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
    if (!originalPath.equals(that.originalPath)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = upstream.hashCode();
    result = 31 * result + requestId.hashCode();
    result = 31 * result + rackId.hashCode();
    result = 31 *result + originalPath.hashCode();
    return result;
  }
}
