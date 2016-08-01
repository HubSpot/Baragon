package com.hubspot.baragon.agent.handlebars;

import java.io.IOException;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

public class FirstOfHelper implements Helper<Object> {
  public static final String NAME = "firstOf";

  private final Object fallback;

  public FirstOfHelper(Object fallback) {
    this.fallback = fallback;
  }

  public Object getFallback() {
    return fallback;
  }

  @Override
  public CharSequence apply(Object context, Options options) throws IOException {
    // handle null
    if (context == null) {
      return options.param(0, fallback).toString();
    }

    // handle optional
    if (context instanceof Optional) {
      final Optional<Object> contextOptional = (Optional<Object>) context;
      return contextOptional.or(options.param(0, fallback)).toString();
    }

    // handle empty string
    if (context instanceof String) {
      final String contextString = (String) context;

      return !Strings.isNullOrEmpty(contextString) ? contextString : options.param(0, fallback).toString();
    }

    // otherwise just return context
    return context.toString();
  }
}
