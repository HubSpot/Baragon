package com.hubspot.baragon.config;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

public class TemplateConfiguration {
  @NotNull
  @JsonProperty("filename")
  private String filename;

  @NotNull
  @JsonProperty("template")
  private String template;

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getTemplate() {
    return template;
  }

  public void setTemplate(String template) {
    this.template = template;
  }
}
