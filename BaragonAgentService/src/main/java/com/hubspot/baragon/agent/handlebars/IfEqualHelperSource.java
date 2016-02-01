package com.hubspot.baragon.agent.handlebars;

import java.io.IOException;

import com.github.jknack.handlebars.Options;

public class IfEqualHelperSource {
  public static CharSequence ifEqual(String v1, String v2, Options options) throws IOException {
    if (v1.equals(v2)) {
      return options.fn();
    } else {
      return options.inverse();
    }
  }
}
