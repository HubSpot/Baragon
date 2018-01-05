package com.hubspot.baragon.agent.handlebars;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;

import com.github.jknack.handlebars.Options;

public class IfContainedInHelperSource {

  public static CharSequence ifContainedIn(Collection<String> haystack, String needle, Options options) throws IOException {
    if (Objects.isNull(haystack)) {
      return options.inverse();
    }

    for (String element : haystack) {
      if (element.contains(needle)) {
        return options.fn();
      }
    }

    return options.inverse();
  }

}
