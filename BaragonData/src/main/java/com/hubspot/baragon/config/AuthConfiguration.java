package com.hubspot.baragon.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class AuthConfiguration {

  @JsonProperty("key")
  private String key;

  @JsonProperty("enabled")
  private boolean enabled = false;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public Optional<String> getKey() {
    return Optional.fromNullable(Strings.emptyToNull(key));
  }

  public void setKey(String key) {
    this.key = key;
  }
}
