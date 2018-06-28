package com.mapbox.services.android.navigation.ui.v5.summary;

import android.content.Context;
import android.text.SpannableStringBuilder;

import com.mapbox.api.directions.v5.DirectionsCriteria;
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceUtils;
import com.mapbox.services.android.navigation.v5.utils.time.TimeUtils;

public class SummaryModel {

  private final String distanceRemaining;
  private final SpannableStringBuilder timeRemaining;
  private final String arrivalTime;

  public SummaryModel(Context context, RouteProgress progress, String language,
                      @DirectionsCriteria.VoiceUnitCriteria String unitType,
                      @NavigationTimeFormat.Type int timeFormatType) {
    TimeUtils timeUtils = new TimeUtils(context, timeFormatType);

    distanceRemaining = new DistanceUtils(context, language, unitType)
      .formatDistance(progress.distanceRemaining()).toString();
    timeRemaining = timeUtils.formatTimeRemaining(progress.durationRemaining());
    arrivalTime = timeUtils.formatTime(progress.durationRemaining());
  }

  String getDistanceRemaining() {
    return distanceRemaining;
  }

  SpannableStringBuilder getTimeRemaining() {
    return timeRemaining;
  }

  String getArrivalTime() {
    return arrivalTime;
  }
}
