package com.mapbox.services.android.navigation.v5.utils.time;

import java.util.Calendar;
import java.util.Locale;

public abstract class TimeFormatter {

  public String formatTime(double duration) {
    Calendar time = Calendar.getInstance();
    time.add(Calendar.SECOND, (int) duration);
    return formatTime(time, duration);
  }

  public String formatTime(Calendar time, double duration) {
    return String.format(Locale.getDefault(), getFormattingString(), time, time);
  }

  protected abstract String getFormattingString();
}
