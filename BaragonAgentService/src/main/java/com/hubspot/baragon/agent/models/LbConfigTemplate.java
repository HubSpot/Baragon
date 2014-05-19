package com.hubspot.baragon.agent.models;

import com.github.jknack.handlebars.Template;
import com.google.common.base.Objects;

public class LbConfigTemplate {
  private final String filename;
  private final Template template;

  public LbConfigTemplate(String filename, Template template) {
    this.filename = filename;
    this.template = template;
  }

  public String getFilename() {
    return filename;
  }

  public Template getTemplate() {
    return template;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(LbConfigTemplate.class)
        .add("filename", filename)
        .add("template", template)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(filename, template);
  }

  @Override
  public boolean equals(Object that) {
    if (this == that) {
      return true;
    }

    if (that == null) {
      return false;
    }

    if (that instanceof LbConfigTemplate) {
      return Objects.equal(filename, ((LbConfigTemplate)that).getFilename())
          && Objects.equal(template, ((LbConfigTemplate)that).getTemplate());
    }

    return false;
  }
}
