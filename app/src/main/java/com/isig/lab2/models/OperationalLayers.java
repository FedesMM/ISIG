package com.isig.lab2.models;

import com.google.gson.Gson;


public class OperationalLayers {

    public String id;
    public String title;
    public int opacity = 1;
    public int minScale = 0;
    public int maxScale = 0;
    public String url;

    public OperationalLayers(String title, String url) {
        this.id = title;
        this.title = title;
        this.url = url;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
