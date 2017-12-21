package com.hubspot.baragon.agent.handlebars;

import java.io.IOException;
import java.util.List;

import com.github.jknack.handlebars.Options;

public class IfContainedInHelperSource {

  public static CharSequence ifContainedIn(List<String> haystack, String needle, Options options) throws IOException {
    for (String element : haystack) {
      if (element.contains(needle)) {
        return options.fn();
      }
    }

    return options.inverse();
  }

}
