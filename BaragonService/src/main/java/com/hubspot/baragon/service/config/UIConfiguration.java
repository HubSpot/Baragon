package com.hubspot.baragon.service.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.hibernate.validator.constraints.NotEmpty;

@JsonIgnoreProperties( ignoreUnknown = true )
public class UIConfiguration {

  @NotEmpty
  @JsonProperty
  private String title = "Baragon";

  @JsonProperty
  private String baseUrl;

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public Optional<String> getBaseUrl() {
    return Optional.fromNullable(Strings.emptyToNull(baseUrl));
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

}
