package com.hubspot.baragon.agent.config;

import java.util.Map;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.hibernate.validator.constraints.NotEmpty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TemplateConfiguration {
  @NotEmpty
  @JsonProperty("filename")
  private String filename;

  @NotNull
  @JsonProperty("template")
  private String defaultTemplate;

  @JsonProperty("namedTemplates")
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

  public Map<String, String> getNamedTemplates() {
    return extraTemplates;
  }

}
