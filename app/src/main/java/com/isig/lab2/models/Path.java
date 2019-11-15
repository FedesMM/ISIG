package com.isig.lab2.models;

import android.graphics.Color;
import android.util.Log;

import com.esri.arcgisruntime.geometry.Geometry;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReference;
import com.esri.arcgisruntime.geometry.SpatialReferences;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Path implements Serializable {
    public static double MIN_TOUR_TRAVEL = 20;
    public static double MED_TOUR_TRAVEL = 60;
    public static double MAX_TOUR_TRAVEL = 200;
    public static int TOUR_TRAVEL_INTERVALS = 7; // siempre IMPAR

    public static int GREEN_R = 45;
    public static int GREEN_G = 201;
    public static int GREEN_B = 55;
    public static int RED_R = 204;
    public static int RED_G = 50;
    public static int RED_B = 50;
    public static int YELLOW_R = 254;
    public static int YELLOW_G = 221;
    public static int YELLOW_B = 44;

    public static int[] speeds = null;

    private boolean hasM;
    private double[][][] paths;
    private Map<String, Integer> spatialReference;

    public Path(boolean hasM, double[][][] paths, Map<String, Integer> spatialReference) {
        this.hasM = hasM;
        this.paths = paths;
        this.spatialReference = spatialReference;
    }

    public boolean isHasM() {
        return hasM;
    }

    public void setHasM(boolean hasM) {
        this.hasM = hasM;
    }

    public double[][][] getPaths() {
        return paths;
    }

    public void setPaths(double[][][] paths) {
        this.paths = paths;
    }

    public Map<String, Integer> getSpatialReference() {
        return spatialReference;
    }

    public void setSpatialReference(Map<String, Integer> spatialReference) {
        this.spatialReference = spatialReference;
    }

    public static int getMediumSpeedIndex() {
        return (TOUR_TRAVEL_INTERVALS-1)/2;
    }

    public List<Marker> getPoints(int representation) {
        List<Marker> list = new ArrayList<>();
        if (representation == Marker.REPRESENTATION_UTM) {
            Geometry g = GeometryEngine.project(new Point(getPaths()[0][0][0], getPaths()[0][0][1], SpatialReference.create(102100)), SpatialReference.create(4326));
            //Log.d("getPoints", g.toJson());
            if (g instanceof Point) {
                list.add(new Marker(((Point) g).getX(), ((Point) g).getY(), representation));
            }
        } else {
            list.add(new Marker(getPaths()[0][0][0], getPaths()[0][0][1], Marker.REPRESENTATION_WGS84));
        }

        for (int i = 0; i < getPaths().length; i++) {
            for (int j = 0; j < getPaths()[i].length; j++) {
                if (j > 0) {
                    if (representation == Marker.REPRESENTATION_UTM) {
                        Geometry g = GeometryEngine.project(new Point(getPaths()[i][j][0], getPaths()[i][j][1], SpatialReference.create(102100)), SpatialReference.create(4326));
                        //Log.d("getPoints", g.toJson());
                        if (g instanceof Point) {
                            list.add(new Marker(((Point) g).getX(), ((Point) g).getY(), representation));
                        }
                    } else {
                        list.add(new Marker(getPaths()[i][j][0], getPaths()[i][j][1], Marker.REPRESENTATION_WGS84));
                    }
                }
            }
        }
        return list;
    }

    public static Marker largo(List<Marker> list) {
        if (list != null && list.size() > 1) {
            double lon = Math.abs(list.get(0).lon) - Math.abs(list.get(1).lon);
            double lat = Math.abs(list.get(0).lat) - Math.abs(list.get(1).lat);
            for (int i = 1; i < list.size(); i++) {
                if (list.size() > i+1) {
                    lon += Math.abs(list.get(i).lon) - Math.abs(list.get(i + 1).lon);
                    lat += Math.abs(list.get(i).lat) - Math.abs(list.get(i + 1).lat);
                }
            }
            return new Marker(lon, lat, Marker.REPRESENTATION_WGS84);
        } else
            return new Marker(0,0, Marker.REPRESENTATION_WGS84);
    }

    public static double largoCaminoEnKm(List<Marker> markers) {
        double dist = 0;
        if (!markers.isEmpty() && markers.size() > 1) {
            for (int i = 0; i < markers.size() - 1; i++) {
                dist = dist + Marker.LatLong2Km(markers.get(i), markers.get(i+1));
            }
        }
        return dist;
    }

    public static RoutePointRequestModel nextPoint(RoutePointRequestModel request) {
        if (request.list == null || request.list.size() == 0) {
            request.resultMarker = null;
            return request;
        } else if (request.list.size() == 1) {
            request.resultMarker = request.list.get(0);
            request.list.remove(0);
            return request;
        } else {
            Marker org = request.list.get(0);
            Marker dst = request.list.get(1);
            double distPuntos = Marker.LatLong2Km(org, dst);
            if (distPuntos < request.distAcumulada) {
                request.list.remove(0);
                request.distAcumulada = request.distAcumulada - distPuntos;
                return nextPoint(request);
            } else if (distPuntos > request.distAcumulada) {
                double ratio = request.distAcumulada / distPuntos;
                double newLon = org.lon - (org.lon - dst.lon) * ratio;
                double newLat = org.lat - (org.lat - dst.lat) * ratio;
                Marker newMarker = new Marker("", "", newLon, newLat, Marker.REPRESENTATION_WGS84, Color.BLACK);
                request.resultMarker = newMarker;
                request.list.remove(0);
                request.list.add(0, newMarker);
                request.distAcumulada = request.getDistPorSalto();
                return request;
            } else {
                request.resultMarker = request.list.get(1);
                request.resultMarker.setColor(Color.BLACK);
                request.list.remove(0);
                request.distAcumulada = request.getDistPorSalto();
                return request;
            }
        }
    }

    public static int[] getSpeeds(double distance) {
        if (speeds == null || speeds.length != TOUR_TRAVEL_INTERVALS) {
            int[] res = new int[TOUR_TRAVEL_INTERVALS];
            int med = getMediumSpeedIndex();
            double speed = 3600 * distance;
            res[0] = (int) (speed / MIN_TOUR_TRAVEL);
            res[TOUR_TRAVEL_INTERVALS - 1] = (int) (speed / MAX_TOUR_TRAVEL);
            for (int i = 1; i < TOUR_TRAVEL_INTERVALS - 1; i++) {
                if (i < med) {
                    double ratio = (double) i / (double) med;
                    res[i] = (int) (speed / MIN_TOUR_TRAVEL) + (int) Math.round(((speed / MED_TOUR_TRAVEL) - (speed / MIN_TOUR_TRAVEL)) * ratio);
                } else if (i > med) {
                    double ratio = (double) (i - med) / (double) med;
                    res[i] = (int) (speed / MED_TOUR_TRAVEL) + (int) Math.round(((speed / MAX_TOUR_TRAVEL) - (speed / MED_TOUR_TRAVEL)) * ratio);
                } else {
                    res[i] = (int) (speed / MED_TOUR_TRAVEL);
                }
            }
            speeds = res;
        }
        return speeds;
    }

    public static int getSpeedByIndex(double distTotal, int index) {
        return Path.getSpeeds(distTotal)[Path.TOUR_TRAVEL_INTERVALS-index-1];
    }

    public static int getColorBySpeedIndex(int index) {
        if (index == 0)
            return Color.rgb(RED_R, RED_G, RED_B);
        if (index == MAX_TOUR_TRAVEL-1)
            return Color.rgb(GREEN_R, GREEN_G, GREEN_B);
        int med = getMediumSpeedIndex();
        if (index < med) {
            double ratio = (double) index / (double) med;
            String hexString = "#" + minPlusRatioInHex(RED_R,YELLOW_R,ratio)+minPlusRatioInHex(RED_G,YELLOW_G,ratio)+minPlusRatioInHex(RED_B,YELLOW_B,ratio);
            return Color.parseColor(hexString);
        } else if (index > med) {
            double ratio = (double) (index - med) / (double) med;
            String hexString = "#" + minPlusRatioInHex(YELLOW_R,GREEN_R,ratio)+minPlusRatioInHex(YELLOW_G,GREEN_G,ratio)+minPlusRatioInHex(YELLOW_B,GREEN_B,ratio);
            return Color.parseColor(hexString);
        } else {
            return Color.rgb(YELLOW_R, YELLOW_G, YELLOW_B);
        }
    }

    private static String minPlusRatioInHex(int a, int b, double ratio) {
        String res = Long.toHexString(Math.round(a + ((b - a) * ratio)));
        if (a > b) {
            res = Long.toHexString(Math.round(a - ((a - b) * ratio)));
        }
        if (res.length() == 1) {
            res = "0" + res;
        }
        return res;
    }
}
