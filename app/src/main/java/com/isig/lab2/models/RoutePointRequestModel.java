package com.isig.lab2.models;

import java.util.List;

public class RoutePointRequestModel {
    public List<Marker> list;
    public double distAcumulada;
    public double speed;
    public double refreshRate;
    public Marker resultMarker;

    public RoutePointRequestModel(List<Marker> list, double distAcumulada, double speed, double refreshRate) {
        this.list = list;
        this.distAcumulada = distAcumulada;
        this.speed = speed;
        this.refreshRate = refreshRate;
    }

    public long getRefreshRate() {
        return Math.round(1000 / refreshRate);
    }

    public double getDistPorSalto() {
        return (speed / 3600) * refreshRate;
    }
}
