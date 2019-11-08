package com.isig.lab2.models;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Path {
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

    public List<Marker> getPoints() {
        List<Marker> list = new ArrayList<>();
        list.add(new Marker(getPaths()[0][0][0], getPaths()[0][0][1], Marker.REPRESENTATION_WGS84));

        for (int i = 0; i < getPaths().length; i++) {
            for (int j = 0; j < getPaths()[i].length; j++) {
                if (j > 0) {
                    list.add(new Marker(getPaths()[i][j][0], getPaths()[i][j][1], Marker.REPRESENTATION_WGS84));
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

    public static List<Marker> pointsInPath(List<Marker> list, double speed, double refreshRate) {
        double distRecorrida = (speed / 3600) * refreshRate;
        return Path.nextPoint(list, distRecorrida, distRecorrida);
    }

    private static List<Marker> nextPoint(List<Marker> list, double distAcumulada, double distPorSalto) {
        if (list == null || list.size() == 0) {
            return null;
        } else if (list.size() == 1) {
            return list;
        } else {
            Marker org = list.get(0);
            Marker dst = list.get(1);
            double distPuntos = Marker.LotLong2Km(org, dst);
            if (distPuntos < distAcumulada) {
                list.remove(0);
                return nextPoint(list,distAcumulada - distPuntos, distPorSalto);
            } else if (distPuntos > distAcumulada) {
                double ratio = distAcumulada / distPuntos;
                double newLon = org.lon - (org.lon - dst.lon) * ratio;
                double newLat = org.lat - (org.lat - dst.lat) * ratio;
                Marker newMarker = new Marker(newLon, newLat, Marker.REPRESENTATION_WGS84);
                List<Marker> result = new ArrayList<>();
                result.add(newMarker);
                list.remove(0);
                list.add(0, newMarker);
                List<Marker> res = nextPoint(list, distPorSalto, distPorSalto);
                if (res != null) {
                    result.addAll(res);
                }
                return result;
            } else {
                List<Marker> result = new ArrayList<>();
                result.add(list.get(1));
                list.remove(0);
                List<Marker> res = nextPoint(list, distPorSalto, distPorSalto);
                if (res != null) {
                    result.addAll(res);
                }
                return result;
            }
        }
    }
}
