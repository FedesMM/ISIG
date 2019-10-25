package com.isig.lab2.utils;

import com.esri.arcgisruntime.geometry.CoordinateFormatter;
import com.esri.arcgisruntime.geometry.Point;

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


}
