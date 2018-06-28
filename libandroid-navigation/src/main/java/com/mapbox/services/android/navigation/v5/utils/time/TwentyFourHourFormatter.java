package com.mapbox.services.android.navigation.v5.utils.time;

public class TwentyFourHourFormatter extends TimeFormatter {
  static final String TWENTY_FOUR_HOUR_FORMAT = "%tk:%tM";

  @Override
  protected String getFormattingString() {
    return TWENTY_FOUR_HOUR_FORMAT;
  }
}
