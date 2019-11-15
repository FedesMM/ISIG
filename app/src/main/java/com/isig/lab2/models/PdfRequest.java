package com.isig.lab2.models;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.List;

public class PdfRequest implements Serializable {

    public MapOptions mapOptions;
    public List<OperationalLayers> operationalLayers;
    public ExportOptions exportOptions;

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
