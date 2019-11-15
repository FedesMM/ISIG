package com.isig.lab2.models;

import com.google.gson.Gson;

import java.io.Serializable;

public class Extent implements Serializable {
    public double xmin;
    public double ymin;
    public double xmax;
    public double ymax;

    public Extent(double xmin, double ymin, double xmax, double ymax) {
        this.xmin = xmin;
        this.ymin = ymin;
        this.xmax = xmax;
        this.ymax = ymax;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}