package com.hubspot.baragon.agent.handlebars;

import java.io.IOException;

import com.github.jknack.handlebars.Options;
import com.google.common.base.Optional;

public class IfEqualHelperSource {
  public static CharSequence ifEqual(String v1, String v2, Options options) throws IOException {
    if (v1 == null ? v2 == null : v1.equals(v2)) {
      return options.fn();
    } else {
      return options.inverse();
    }
  }

  public static CharSequence ifOptionalEqual(Optional<String> v1, Optional<String> v2, Options options) throws IOException {
    if (v1.equals(v2)) {
      return options.fn();
    } else {
      return options.inverse();
    }
  }
}
