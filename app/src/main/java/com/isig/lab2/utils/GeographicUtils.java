package com.isig.lab2.utils;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;

import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.geometry.CoordinateFormatter;
import com.esri.arcgisruntime.geometry.Point;

import java.util.List;

public class GeographicUtils {

    private static String[] Point2LatLong(Point location) {
        String coords = CoordinateFormatter.toLatitudeLongitude(location, CoordinateFormatter.LatitudeLongitudeFormat.DECIMAL_DEGREES, 4);
        return coords.split(" ");
    }

    public static String Point2Lat(Point location) {
        String[] coords = Point2LatLong(location);
        if (coords.length == 2) {
            return StringUtils.format(coords[0]);
        }
        return "";
    }

    public static String Point2Long(Point location) {
        String[] coords = Point2LatLong(location);
        if (coords.length == 2) {
            return StringUtils.format(coords[1]);
        }
        return "";
    }

    public static boolean isSelected(List<Feature> list, Feature feature) {
        if (feature.getGeometry() instanceof Point) {
            Point pointFeature = (Point) feature.getGeometry();
            if (!list.isEmpty()) {
                for (Feature f : list) {
                    if (f.getGeometry() instanceof Point) {
                        Point p = (Point) f.getGeometry();
                        if (p.getX() == pointFeature.getX() && p.getY() == pointFeature.getY()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void removeFeatureFromList(List<Feature> list, Feature feature) {
        if (feature.getGeometry() instanceof Point) {
            Point pointFeature = (Point) feature.getGeometry();
            if (!list.isEmpty()) {
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i).getGeometry() instanceof Point) {
                        Point p = (Point) list.get(i).getGeometry();
                        if (p.getX() == pointFeature.getX() && p.getY() == pointFeature.getY()) {
                            list.remove(i);
                        }
                    }
                }
            }
        }
    }
}