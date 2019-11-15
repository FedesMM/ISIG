package com.isig.lab2.models;
import com.google.gson.Gson;

import java.io.Serializable;

public class CondadosID implements Serializable {
    public String objectIdFieldName;
    public double [] objectIds;

    @Override
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }


}

