package com.isig.lab2.models;

import com.google.gson.Gson;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Intersecciones implements Serializable {
    public String displayFieldName;
    public String geometryType;
    public Map<String, String> fieldAliases;
    public Map<String, Integer> spatialReference;
    public List<FeatureParseo> features;


    public class FeatureParseo implements Serializable {
        public Map<String, String>  attributes;
        public GeometryParseo geometry;
        @Override
        public String toString(){
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    @Override
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public class GeometryParseo implements Serializable {
        public double [][][] rings;

        @Override
        public String toString(){
            Gson gson = new Gson();
            return gson.toJson(this);
        }

    }

}


