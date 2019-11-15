package com.isig.lab2.models;

import com.google.gson.Gson;

import java.io.Serializable;

public class MapOptions implements Serializable {

    public Extent extent;
    public double scale;
    public MySpatialReference spatialReference;

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
