package com.isig.lab2.models;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import com.isig.lab2.R;

public class Configuration {

    private Viewer.ChangeConfigCallback changeConfigCallback;

    public boolean shortestRoute = false;

    public void showConfigurationDialog(Context context, Configuration previousConfig, ViewGroup vg, Viewer.ChangeConfigCallback callback) {
        View viewInflated = LayoutInflater.from(context).inflate(R.layout.config, vg, false);
        this.changeConfigCallback = callback;
        final CheckBox checkBox = viewInflated.findViewById(R.id.checkbox_ruta_mas_corta);
        checkBox.setChecked(previousConfig.shortestRoute);

        final Button cancel = viewInflated.findViewById(R.id.cancel_config);
        final Button accept = viewInflated.findViewById(R.id.accept_config);
        AlertDialog.Builder AD = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setView(viewInflated);

        final AlertDialog ADClose = AD.create();

        cancel.setOnClickListener(v -> {
            changeConfigCallback.onConfigCanceled();
            ADClose.dismiss();
        });
        accept.setOnClickListener(v -> {
            this.shortestRoute = checkBox.isChecked();
            changeConfigCallback.onConfigChanged(this);
            ADClose.dismiss();
        });

        ADClose.show();
    }

    public interface Viewer {
        interface ChangeConfigCallback {
            void onConfigChanged(Configuration configuration);
            void onConfigCanceled();
        }
    }
}
