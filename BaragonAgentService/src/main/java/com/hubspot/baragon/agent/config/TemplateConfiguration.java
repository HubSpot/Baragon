package com.hubspot.baragon.agent.config;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;

public class TemplateConfiguration {
  @NotEmpty
  @JsonProperty("filename")
  private String filename;

  @NotNull
  @JsonProperty("template")
  private String defaultTemplate;

  @JsonProperty("extraTemplates")
  private Map<String, String> extraTemplates;

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
  }

  public String getDefaultTemplate() {
    return defaultTemplate;
  }

  public void setTemplate(String template) {
    this.defaultTemplate = template;
  }

  public Map<String, String> getExtraTemplates() {
    return extraTemplates;
  }

}
