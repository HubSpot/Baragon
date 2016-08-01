package com.hubspot.baragon.agent.handlebars;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormatTimestampHelper implements Helper<Number> {
  private static final Logger LOG = LoggerFactory.getLogger(FormatTimestampHelper.class);
  public static final String NAME = "formatTimestamp";

  private final String defaultFormatString;

  public FormatTimestampHelper(String defaultFormatString) {
    this.defaultFormatString = defaultFormatString;
  }

  public String getDefaultFormatString() {
    return defaultFormatString;
  }

  @Override
  public CharSequence apply(Number context, Options options) throws IOException {
    String dateFormatString;
    try {
      dateFormatString = options.param(0, defaultFormatString);
    } catch (ClassCastException cce) {  // phorce.
      LOG.warn(String.format("Date format %s isn't subclass of String, using default: %s", options.param(0), defaultFormatString));
      dateFormatString = defaultFormatString;
    }

    final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormatString);

    final Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(context.longValue());

    return simpleDateFormat.format(cal.getTime());
  }
}
