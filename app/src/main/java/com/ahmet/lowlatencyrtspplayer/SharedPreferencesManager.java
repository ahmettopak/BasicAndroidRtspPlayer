package com.ahmet.lowlatencyrtspplayer;

import static com.ahmet.lowlatencyrtspplayer.MainActivity.DEFAULT_RTSP_URL;
import static com.ahmet.lowlatencyrtspplayer.MainActivity.RTSP_URL_KEY;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author Ahmet TOPAK
 * @version 1.0
 * @since 6/25/2024
 */

public class SharedPreferencesManager {
    private final SharedPreferences sharedPreferences;

    public SharedPreferencesManager(Context context, String prefsName) {
        sharedPreferences = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);
    }

    public String loadRtspUrl(String defaultValue) {
        return sharedPreferences.getString(RTSP_URL_KEY, defaultValue);
    }
    public String loadRtspUrl() {
        return sharedPreferences.getString(RTSP_URL_KEY, DEFAULT_RTSP_URL);
    }
    public void saveRtspUrl(String url) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(RTSP_URL_KEY, url);
        editor.apply();
    }
}

