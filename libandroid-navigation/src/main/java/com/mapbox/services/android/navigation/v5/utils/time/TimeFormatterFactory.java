package com.mapbox.services.android.navigation.v5.utils.time;

import android.content.Context;
import android.text.format.DateFormat;

import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;

public class TimeFormatterFactory {
  public TimeFormatter getTimeFormatter(Context context, @NavigationTimeFormat.Type int type) {
    boolean isTwentyFourHourFormat = DateFormat.is24HourFormat(context);
    if (type == NavigationTimeFormat.NONE_SPECIFIED) {
      type = isTwentyFourHourFormat ? NavigationTimeFormat.TWENTY_FOUR_HOURS : NavigationTimeFormat.TWELVE_HOURS;
    }

    if (type == NavigationTimeFormat.TWENTY_FOUR_HOURS) {
      return new TwentyFourHourFormatter();
    } else {
      return new TwelveHourFormatter();
    }
  }
}
