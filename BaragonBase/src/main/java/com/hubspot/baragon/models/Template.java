package com.hubspot.baragon.models;

import com.github.mustachejava.Mustache;
import com.google.common.base.Objects;

public class Template {
  private final String filename;
  private final Mustache template;

  public Template(String filename, Mustache template) {
    this.filename = filename;
    this.template = template;
  }

  public String getFilename() {
    return filename;
  }

  public Mustache getTemplate() {
    return template;
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(Template.class)
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

    if (that instanceof Template) {
      return Objects.equal(filename, ((Template)that).getFilename())
          && Objects.equal(template, ((Template)that).getTemplate());
    }

    return false;
  }
}
