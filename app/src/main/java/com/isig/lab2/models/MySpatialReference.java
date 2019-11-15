package com.isig.lab2.models;

import com.google.gson.Gson;

public class MySpatialReference {
    public int wkid;

    public MySpatialReference(int wkid) {
        this.wkid = wkid;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
