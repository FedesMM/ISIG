package com.isig.lab2.utils;

import android.util.Log;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class APIUtils {

    public static void callAPI(final String fullUri, final String url, final String params, final View.APICallback callback) {
        OkHttpClient client = new OkHttpClient();

        String uri = (fullUri != null) ? fullUri : url + "?" + params;
        try {
            Request request = new Request.Builder()
                    .url(uri)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.d("APIUtils", "onFailure: " + e.getMessage());
                    callback.onError(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    try {
                        if (response.isSuccessful() && response.body() != null) {
                            String body = response.body().string();
                            Log.d("APIUtils", "onResponse, body str" + body);
                            callback.onSuccess(body);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public interface View {
        interface APICallback {
            void onSuccess(String response);
            void onError(String error);
        }
    }
}
