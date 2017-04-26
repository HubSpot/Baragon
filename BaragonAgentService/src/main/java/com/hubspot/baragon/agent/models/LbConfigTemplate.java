package com.hubspot.baragon.agent.models;

import com.github.jknack.handlebars.Template;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

public class LbConfigTemplate {
  private final String filename;
  private final Template template;
  private final FilePathFormatType formatType;

  public LbConfigTemplate(String filename, Template template, FilePathFormatType formatType) {
    this.filename = filename;
    this.template = template;
    this.formatType = formatType;

  }

  public String getFilename() {
    return filename;
  }

  public Template getTemplate() {
    return template;
  }

  public FilePathFormatType getFormatType() {
    return formatType;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(LbConfigTemplate.class)
        .add("filename", filename)
        .add("template", template)
        .add("formatType", formatType)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(filename, template, formatType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    LbConfigTemplate that = (LbConfigTemplate) o;

    if (!filename.equals(that.filename)) {
      return false;
    }
    if (!template.equals(that.template)) {
      return false;
    }
    if (!formatType.equals(that.formatType)) {
      return false;
    }

    return true;
  }
}
