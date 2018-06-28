package com.mapbox.services.android.navigation.v5.utils.time;

public class TwelveHourFormatter extends TimeFormatter {
  static final String TWELVE_HOUR_FORMAT = "%tl:%tM %tp";

  @Override
  protected String getFormattingString() {
    return TWELVE_HOUR_FORMAT;
  }
}
