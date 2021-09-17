package com.hubspot.baragon.agent.handlebars;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.google.common.base.Strings;
import java.net.UnknownHostException;

// Will make a string lowercase as well as replacing any - with _ to be compatible with how nginx sets variable names
public class ToNginxVarHelper implements Helper<String> {
  public static final String NAME = "toNginxVar";

  @Override
  public CharSequence apply(String input, Options options) throws UnknownHostException {
    if (!Strings.isNullOrEmpty(input)) {
      return input.toLowerCase().replaceAll("-", "_");
    } else {
      return input;
    }
  }
}
