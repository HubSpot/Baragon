package com.hubspot.baragon.service.config;

import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import org.hibernate.validator.constraints.NotEmpty;

public class UIConfiguration {

  @NotEmpty
  @JsonProperty
  private String title = "Baragon";

  @JsonProperty
  @Pattern( regexp = "^|#[0-9a-fA-F]{6}$" )
  private String navColor = "";

  @JsonProperty
  private String baseUrl;

  private boolean allowEdit = false;

  @JsonProperty
  private Optional<String> allowEditKey = Optional.absent();

  public boolean allowEdit() {
    return allowEdit;
  }

  public void setAllowEdit(boolean allowEdit) {
    this.allowEdit = allowEdit;
  }

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

  public String getNavColor() {
    return navColor;
  }

  public void setNavColor(String navColor) {
    this.navColor = navColor;
  }

  public Optional<String> getAllowEditKey() {
    return allowEditKey;
  }

  public void setAllowEditKey(Optional<String> allowEditKey) {
    this.allowEditKey = allowEditKey;
  }
}
