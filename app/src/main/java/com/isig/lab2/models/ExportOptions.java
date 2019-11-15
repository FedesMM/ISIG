package com.isig.lab2.models;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

public class ExportOptions {
    public int dpi;
    public List<Integer> outputSize = new ArrayList<>();

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
