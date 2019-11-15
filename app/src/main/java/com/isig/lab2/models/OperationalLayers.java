package com.isig.lab2.models;

import com.google.gson.Gson;


public class OperationalLayers {

    public String id;
    public String url;
    public String title;
    public boolean visibility = true;

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
