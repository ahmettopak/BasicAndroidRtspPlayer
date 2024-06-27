package com.ahmet.lowlatencyrtspplayer;

import static com.ahmet.lowlatencyrtspplayer.MainActivity.RTSP_URL_KEY;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ahmet.lowlatencyrtspplayer.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ahmet TOPAK
 * @version 1.0
 * @since 6/25/2024
 */
public class UIController {
    private final MainActivity activity;
    private final ActivityMainBinding binding;
    private final SharedPreferencesManager sharedPreferencesManager;
    private final RtspManager rtspManager;

    private final ArrayAdapter<String> logAdapter;
    private final List<String> logList = new ArrayList<>();

    private boolean isLoggingEnabled;

    public UIController(MainActivity activity, ActivityMainBinding binding, SharedPreferencesManager sharedPreferencesManager, RtspManager rtspManager) {
        this.activity = activity;
        this.binding = binding;
        this.sharedPreferencesManager = sharedPreferencesManager;
        this.rtspManager = rtspManager;
        this.logAdapter = new ArrayAdapter<>(activity, R.layout.simple_list_item, logList);
        binding.logListView.setAdapter(logAdapter);

        setupListeners();
        setupGestureDetection();
    }

    public void initialize(String rtspUrl, boolean isControlsVisible, boolean isLoggingEnabled) {
        this.isLoggingEnabled = isLoggingEnabled;
        binding.urlEditText.setText(rtspUrl);
        changeControlsVisibility(isControlsVisible);
        toggleLogVisibility(isLoggingEnabled);
        rtspManager.initialize(rtspUrl);
        log("RTSP stream started: " + rtspUrl);
    }

    private void setupListeners() {
        binding.saveUrlButton.setOnClickListener(v -> saveRtspUrl());
        binding.refreshStreamButton.setOnClickListener(v -> refreshStream());
        binding.startStreamButton.setOnClickListener(v -> startStream());
        binding.stopStreamButton.setOnClickListener(v -> stopStream());
        binding.toggleLogButton.setOnCheckedChangeListener((buttonView, isChecked) -> toggleLogVisibility(isChecked));
    }

    private void setupGestureDetection() {
        GestureDetector gestureDetector = new GestureDetector(activity, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                changeControlsVisibility(!binding.saveUrlButton.isShown());
                return true;
            }
        });

        binding.surfaceView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        binding.surfaceView.setOnLongClickListener(v -> {
            refreshStream();
            return true;
        });
    }

    private void saveRtspUrl() {
        rtspManager.stopStream(() -> {
            String rtspUrl = binding.urlEditText.getText().toString();
            sharedPreferencesManager.saveRtspUrl(rtspUrl);
            startStream(rtspUrl);
            log("Stream started with new URL: " + rtspUrl);
        }, () -> log("Failed to stop stream before saving new URL"));
    }

    private void startStream(String rtspUrl) {
        rtspManager.startStream(rtspUrl, () -> log("Stream started: " + rtspUrl));
    }
    private void startStream() {
        String rtspUrl = sharedPreferencesManager.loadRtspUrl();
        rtspManager.startStream(rtspUrl, () -> log("Stream started: " + rtspUrl));
    }
    private void stopStream() {
        rtspManager.stopStream(() -> log("Stream stopped"), () -> log("Failed to stop stream after maximum attempts"));
    }

    private void refreshStream() {
        String rtspUrl = binding.urlEditText.getText().toString();
        rtspManager.refreshStream(rtspUrl, () -> log("Stream stopped for refresh: " + rtspUrl), () -> log("Stream refreshed: " + rtspUrl));
    }

    private void changeControlsVisibility(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.INVISIBLE;
        binding.urlEditText.setVisibility(visibility);
        binding.saveUrlButton.setVisibility(visibility);
        binding.refreshStreamButton.setVisibility(visibility);
        binding.startStreamButton.setVisibility(visibility);
        binding.stopStreamButton.setVisibility(visibility);
        binding.toggleLogButton.setVisibility(visibility);
    }

    private void toggleLogVisibility(boolean isLogEnabled) {
        this.isLoggingEnabled = isLogEnabled;
        binding.logListView.setVisibility(isLogEnabled ? View.VISIBLE : View.GONE);
        showToast("Logging " + (isLogEnabled ? "enabled" : "disabled"));
    }

    public void log(String message) {
        if (isLoggingEnabled) {
            logList.add(message);
            activity.runOnUiThread(() -> {
                logAdapter.notifyDataSetChanged();
                binding.logListView.smoothScrollToPosition(logList.size() - 1);
            });
        }
    }

    private void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }
}
