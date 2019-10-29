package com.isig.lab2.models;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.arcgisruntime.geometry.Point;
import com.google.gson.Gson;
import com.isig.lab2.R;
import com.isig.lab2.utils.GeographicUtils;

import java.util.List;

public class Marker {

    private static final int FROM_MAP = 0;
    private static final int FROM_LAT_LONG = 1;

    public static final int REPRESENTATION_WGS84 = 0;
    public static final int REPRESENTATION_UTM = 1;

    private String id;
    private String nombre;
    private double lat;
    private double lon;
    private int color;
    private int representation;

    private Viewer.AddMarkerCallback addMarkerCallback;

    public Marker(String id, String nombre, double lat, double lon, int representation) {
        this.id = id;
        this.nombre = nombre;
        this.lat = lat;
        this.lon = lon;
        this.color = Color.RED;
        this.representation = representation;
    }

    public void showAddFromMapDialog(Context context, ViewGroup vg, Point point, List<Marker> markers, Viewer.AddMarkerCallback callback) {
        View viewInflated = LayoutInflater.from(context).inflate(R.layout.view_add_point_on_map, vg, false);
        final TextView latMarker = viewInflated.findViewById(R.id.lat_marcador);
        final TextView longMarker = viewInflated.findViewById(R.id.long_marcador);
        latMarker.setText(GeographicUtils.Point2Lat(point));
        longMarker.setText(GeographicUtils.Point2Long(point));
        showDialog(context, point, markers, callback, viewInflated, FROM_MAP);
    }

    public void showAddFromLatLongDialog(Context context, ViewGroup vg, Point point, List<Marker> markers, Viewer.AddMarkerCallback callback) {
        View viewInflated = LayoutInflater.from(context).inflate(R.layout.view_add_point_from_lat_long, vg, false);
        showDialog(context, point, markers, callback, viewInflated, FROM_LAT_LONG);
    }

    private void showDialog(Context context, Point point, List<Marker> markers, Viewer.AddMarkerCallback callback, View viewInflated, final int type) {
        this.addMarkerCallback = callback;
        final EditText name = viewInflated.findViewById(R.id.nombre_marcador);
        final Button cancel = viewInflated.findViewById(R.id.cancel_edit_marker);
        final Button accept = viewInflated.findViewById(R.id.accept_edit_marker);
        AlertDialog.Builder AD = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setView(viewInflated);

        final AlertDialog ADClose = AD.create();

        cancel.setOnClickListener(v -> {
            addMarkerCallback.onMarkerAddingCanceled();
            ADClose.dismiss();
        });
        accept.setOnClickListener(v -> {
            this.nombre = name.getText().toString();
            if (type == FROM_MAP) {
                this.lat = point.getY();
                this.lon = point.getX();
                markers.add(this);
                ADClose.dismiss();
                addMarkerCallback.onMarkerAdded();
            } else {
                final EditText latMarker = viewInflated.findViewById(R.id.lat_marcador);
                final EditText longMarker = viewInflated.findViewById(R.id.long_marcador);
                this.nombre = name.getText().toString();
                try {
                    this.lat = Float.parseFloat(latMarker.getText().toString());
                    try {
                        this.lon = Float.parseFloat(longMarker.getText().toString());
                        markers.add(this);
                        ADClose.dismiss();
                        addMarkerCallback.onMarkerAdded();
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(context, context.getString(R.string.error_parsing_lat), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(context, context.getString(R.string.error_parsing_long), Toast.LENGTH_LONG).show();
                }
            }
        });

        ADClose.show();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getRepresentation() {
        return representation;
    }

    public void setRepresentation(int representation) {
        this.representation = representation;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public interface Viewer {
        interface AddMarkerCallback {
            void onMarkerAdded();
            void onMarkerAddingCanceled();
        }
    }
}