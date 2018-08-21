package com.mapbox.services.android.navigation.ui.v5.map;

import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.MultiLineString;
import com.mapbox.geojson.Point;
import com.mapbox.turf.TurfMeasurement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;

import static com.mapbox.turf.TurfConstants.UNIT_METRES;
import static com.mapbox.turf.TurfMeasurement.along;
import static com.mapbox.turf.TurfMisc.lineSlice;
import static com.mapbox.turf.TurfMisc.lineSliceAlong;

class WaynameFeatureFilter {

  private static final int FIRST = 0;
  private static final int ONE_FEATURE = 1;
  private static final int TWO_POINTS = 2;
  private static final double ZERO_METERS = 0d;
  private static final double TEN = 10d;
  private final List<Feature> queriedFeatures;
  private final Point currentPoint;
  private final LineString currentStepLineString;

  WaynameFeatureFilter(List<Feature> queriedFeatures, Location currentLocation, List<Point> currentStepPoints) {
    this.queriedFeatures = queriedFeatures;
    this.currentPoint = Point.fromLngLat(currentLocation.getLongitude(), currentLocation.getLatitude());
    this.currentStepLineString = LineString.fromLngLats(currentStepPoints);
  }

  @Nullable
  Feature filter() {
    // TODO If possible, it'd great if filterQueriedFeatures() returned @NonNull features
    // See https://github.com/mapbox/mapbox-navigation-android/pull/1156#issuecomment-414659621
    return filterQueriedFeatures();
  }

  private Feature filterQueriedFeatures() {
    Timber.d("NAV_DEBUG *************************************** ***************************************");
    Feature filteredFeature = queriedFeatures.get(FIRST);
    if (queriedFeatures.size() == ONE_FEATURE) {
      Timber.d("NAV_DEBUG No filter needed");
      logName(filteredFeature);
      return filteredFeature;
    }
    logNames(queriedFeatures);
    double smallestUserDistanceToFeature = Double.POSITIVE_INFINITY;
    for (Feature feature : queriedFeatures) {
      Timber.d("NAV_DEBUG *************************************** Filtering %s", feature.getStringProperty("name"));
      Geometry featureGeometry = feature.geometry();
      if (featureGeometry == null) {
        continue;
      }
      List<LineString> featureLineStrings = new ArrayList<>();
      if (featureGeometry instanceof LineString) {
        featureLineStrings.add((LineString) featureGeometry);
      } else if (featureGeometry instanceof MultiLineString) {
        featureLineStrings = ((MultiLineString) featureGeometry).lineStrings();
      }

      for (LineString featureLineString : featureLineStrings) {
        List<Point> currentStepCoordinates = currentStepLineString.coordinates();
        int stepCoordinatesSize = currentStepCoordinates.size();
        if (stepCoordinatesSize < TWO_POINTS) {
          return null;
        }
        int lastStepCoordinate = stepCoordinatesSize - 1;
        Point lastStepPoint = currentStepCoordinates.get(lastStepCoordinate);
        if (currentPoint.equals(lastStepPoint)) {
          return null;
        }
        LineString stepSliceFromCurrentPoint = lineSlice(currentPoint, lastStepPoint, currentStepLineString);
        Point pointAheadUserOnStep = along(stepSliceFromCurrentPoint, TEN, UNIT_METRES);
        Timber.d("NAV_DEBUG currentPoint: %s", currentPoint);
        Timber.d("NAV_DEBUG pointAheadUserOnStep: %s", pointAheadUserOnStep);

        List<Point> lineCoordinates = featureLineString.coordinates();
        Timber.d("NAV_DEBUG lineCoordinates");
        for (int i = 0; i < lineCoordinates.size(); i++) {
          Timber.d("NAV_DEBUG point %s: %s", i, lineCoordinates.get(i));
        }
        int lineCoordinatesSize = lineCoordinates.size();
        if (lineCoordinatesSize < TWO_POINTS) {
          return null;
        }
        int lastLineCoordinate = lineCoordinatesSize - 1;
        Point lastLinePoint = lineCoordinates.get(lastLineCoordinate);
        if (currentPoint.equals(lastLinePoint)) {
          return null;
        }
        Timber.d("NAV_DEBUG lastLinePoint: %s", lastLinePoint);

        Point firstLinePoint = lineCoordinates.get(FIRST);
        if (currentPoint.equals(firstLinePoint)) {
          return null;
        }
        Timber.d("NAV_DEBUG firstLinePoint: %s", firstLinePoint);

        LineString reversedFeatureLine = reverseFeatureLineStringCoordinates(featureLineString);
        LineString currentAheadLine = reversedFeatureLine;
        LineString currentBehindLine = featureLineString;

        Point currentDirectionAhead = firstLinePoint;
        Point currentDirectionBehind = lastLinePoint;

        double distanceCurrentFirst = calculateDistance(currentPoint, firstLinePoint);
        double distanceAheadFirst = calculateDistance(pointAheadUserOnStep, firstLinePoint);
        if (distanceAheadFirst >= distanceCurrentFirst) {
          currentAheadLine = featureLineString;
          currentBehindLine = reversedFeatureLine;
          currentDirectionAhead = lastLinePoint;
          currentDirectionBehind = firstLinePoint;
          Timber.d("NAV_DEBUG Moving along last point");
        }

        LineString sliceFromCurrentPoint = lineSlice(currentPoint, currentDirectionAhead, currentAheadLine);
        Point pointAheadFeature = along(sliceFromCurrentPoint, TEN, UNIT_METRES);
        Timber.d("NAV_DEBUG pointAheadFeature: %s", pointAheadFeature);
        LineString reverseSliceFromCurrentPoint = lineSlice(currentPoint, currentDirectionBehind, currentBehindLine);
        Point pointBehindFeature = along(reverseSliceFromCurrentPoint, TEN, UNIT_METRES);
        Timber.d("NAV_DEBUG pointBehindFeature: %s", pointBehindFeature);

        double userDistanceToAheadFeature = calculateDistance(pointAheadUserOnStep, pointAheadFeature);
        Timber.d("NAV_DEBUG userDistanceToAheadFeature: %s", userDistanceToAheadFeature);
        double userDistanceToBehindFeature = calculateDistance(pointAheadUserOnStep, pointBehindFeature);
        Timber.d("NAV_DEBUG userDistanceToBehindFeature: %s", userDistanceToBehindFeature);
        double minDistanceToFeature = Math.min(userDistanceToAheadFeature, userDistanceToBehindFeature);
        Timber.d("NAV_DEBUG minDistanceToFeature: %s", minDistanceToFeature);

        // TODO What happens in the remote case in which minDistanceToFeature == smallestUserDistanceToFeature?
        if (minDistanceToFeature < smallestUserDistanceToFeature) {
          smallestUserDistanceToFeature = minDistanceToFeature;
          filteredFeature = feature;
        }
      }
    }
    Timber.d("NAV_DEBUG smallestUserDistanceToFeature: %s", smallestUserDistanceToFeature);
    logName(filteredFeature);
    return filteredFeature;
  }

  private void logNames(List<Feature> queriedFeatures) {
    List<String> names = new ArrayList<>();
    for (Feature feature : queriedFeatures) {
      boolean hasValidNameProperty = feature.hasNonNullValueForProperty("name");
      if (hasValidNameProperty) {
        names.add(feature.getStringProperty("name"));
      }
    }
    Timber.d("NAV_DEBUG Queried names: %s", names);
  }

  private void logName(Feature feature) {
    boolean hasValidNameProperty = feature.hasNonNullValueForProperty("name");
    if (hasValidNameProperty) {
      String name = feature.getStringProperty("name");
      Timber.d("NAV_DEBUG *************************************** filteredFeature found: %s", name);
    }
  }

  private double calculateDistance(Point lhs, Point rhs) {
    if (lhs == null || rhs == null) {
      return Double.POSITIVE_INFINITY;
    }
    return TurfMeasurement.distance(lhs, rhs);
  }

  @Nullable
  Point findPointFromCurrentPoint(Point currentPoint, double metersFromCurrentPoint, LineString lineString) {
    List<Point> lineStringCoordinates = lineString.coordinates();
    int coordinateSize = lineStringCoordinates.size();
    if (coordinateSize < TWO_POINTS) {
      return null;
    }
    Point lastLinePoint = lineStringCoordinates.get(coordinateSize - 1);
    if (currentPoint == null || currentPoint.equals(lastLinePoint)) {
      return null;
    }
    // TODO find nearestPointOnLine instead of currentPoint?
    LineString sliceFromCurrentPoint = lineSlice(currentPoint, lastLinePoint, lineString);
    LineString meterSlice = lineSliceAlong(sliceFromCurrentPoint, ZERO_METERS, metersFromCurrentPoint, UNIT_METRES);
    List<Point> slicePoints = meterSlice.coordinates();
    if (slicePoints.isEmpty()) {
      return null;
    }
    // TODO use 0 here?
    return slicePoints.get(0);
  }

  @NonNull
  private LineString reverseFeatureLineStringCoordinates(LineString featureLineString) {
    List<Point> reversedFeatureCoordinates = new ArrayList<>(featureLineString.coordinates());
    Collections.reverse(reversedFeatureCoordinates);
    return LineString.fromLngLats(reversedFeatureCoordinates);
  }
}
