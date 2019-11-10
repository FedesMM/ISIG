package com.isig.lab2.models;

import android.graphics.Color;
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
            double distPuntos = Marker.LotLong2Km(org, dst);
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
}
