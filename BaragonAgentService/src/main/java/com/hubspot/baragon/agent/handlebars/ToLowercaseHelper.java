package com.hubspot.baragon.agent.handlebars;

import java.net.UnknownHostException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.google.common.base.Strings;

public class ToLowercaseHelper implements Helper<String> {

  public static final String NAME = "toLowercase";

  @Override
  public CharSequence apply(String input, Options options) throws UnknownHostException {
    if (!Strings.isNullOrEmpty(input)) {
      return input.toLowerCase();
    } else {
      return input;
    }
  }
}
