package com.ahmet.lowlatencyrtspplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.ahmet.lowlatencyrtspplayer.databinding.ActivityMainBinding;
import com.ahmet.lowlatencyrtspplayer.rtsp.widget.RtspSurfaceView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements RtspSurfaceView.RtspStatusListener {
    ActivityMainBinding binding;
    private RtspSurfaceView surfaceView;
    private final Object streamLock = new Object();
    private static final int MAX_STOP_ATTEMPTS = 5;
    private static final long STOP_RETRY_DELAY_MS = 500;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private Button saveUrlButton, startStreamButton, stopStreamButton, refreshStreamButton;
    private EditText urlEditText;
    private SharedPreferences sharedPreferences;

    private static final String SHARED_PREFS_NAME = "RTSPPrefs";
    private static final String RTSP_URL_KEY = "RTSP_URL";
    private static final String DEFAULT_RTSP_URL = "rtsp://192.168.3.10:554/user=elektroland&password=EXLXEXKX&channel=2&stream=0.sdp?";

    private String rtspUrl;
    private boolean isControlsVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initView();
        setupSharedPreferences();
        loadRtspUrl();
        setupSurfaceView();
        setupListeners();
        changeControlsVisibility(isControlsVisible);
    }

    private void initView() {
        surfaceView = binding.surfaceView;
        saveUrlButton = binding.saveUrlButton;
        startStreamButton = binding.startStreamButton;
        stopStreamButton = binding.stopStreamButton;
        refreshStreamButton = binding.refreshStreamButton;
        urlEditText = binding.urlEditText;
    }

    private void setupSharedPreferences() {
        sharedPreferences = getSharedPreferences(SHARED_PREFS_NAME, MODE_PRIVATE);
    }

    private void loadRtspUrl() {
        rtspUrl = sharedPreferences.getString(RTSP_URL_KEY, DEFAULT_RTSP_URL);
        urlEditText.setText(rtspUrl);
    }

    private void setupSurfaceView() {
        Uri rtspUri = Uri.parse(rtspUrl);
        surfaceView.init(rtspUri, "", "");
        surfaceView.start(true, true);
        showToast("RTSP stream started: " + rtspUrl);
    }

    private void setupListeners() {
        saveUrlButton.setOnClickListener(v -> saveRtspUrl());
        refreshStreamButton.setOnClickListener(v -> refreshStream());
        startStreamButton.setOnClickListener(v -> startStream());
        stopStreamButton.setOnClickListener(v -> stopStream());
        setupGestureDetection();

        surfaceView.setStatusListener(this);
    }

    private void saveRtspUrl() {
        surfaceView.stop();
        rtspUrl = urlEditText.getText().toString();

        saveRtspUrlToPreferences(rtspUrl);
        startStream();
        showToast("Stream started with new URL: " + rtspUrl);
    }

    private void saveRtspUrlToPreferences(String url) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(RTSP_URL_KEY, url);
        editor.apply();
    }

    private void startStream() {
        executorService.submit(() -> {
            synchronized (streamLock) {
                if (surfaceView.isStarted()) {
                    runOnUiThread(() -> showToast("Stream is already started: " + rtspUrl));
                    return;
                }

                Uri rtspUri = Uri.parse(rtspUrl);
                surfaceView.init(rtspUri, "", "");
                surfaceView.start(true, true);
                runOnUiThread(() -> showToast("Stream started: " + rtspUrl));
            }
        });
    }

    private void stopStream() {
        executorService.submit(() -> {
            synchronized (streamLock) {
                if (!surfaceView.isStarted()) {
                    runOnUiThread(() -> showToast("Stream is already stopped: " + rtspUrl));
                    return;
                }

                int attempts = 0;
                boolean stopped = false;

                while (attempts < MAX_STOP_ATTEMPTS && !stopped) {
                    surfaceView.stop();
                    try {
                        Thread.sleep(STOP_RETRY_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }

                    stopped = !surfaceView.isStarted();
                    attempts++;
                }

                boolean finalStopped = stopped;
                runOnUiThread(() -> {
                    if (finalStopped) {
                        showToast("Stream stopped: " + rtspUrl);
                    } else {
                        showToast("Failed to stop stream after " + MAX_STOP_ATTEMPTS + " attempts: " + rtspUrl);
                    }
                });
            }
        });
    }

    private void refreshStream() {
        executorService.submit(() -> {
            synchronized (streamLock) {
                if (surfaceView.isStarted()) {
                    surfaceView.stop();
                    try {
                        Thread.sleep(STOP_RETRY_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    runOnUiThread(() -> showToast("Stream stopped for refresh: " + rtspUrl));
                }

                surfaceView.start(true, false);
                runOnUiThread(() -> showToast("Stream refreshed: " + rtspUrl));
            }
        });
    }

    private void setupGestureDetection() {
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                isControlsVisible = !isControlsVisible;
                changeControlsVisibility(isControlsVisible);
                return true;
            }
        });

        surfaceView.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        surfaceView.setOnLongClickListener(v -> {
            refreshStream();
            return true;
        });
    }

    private void changeControlsVisibility(boolean isVisible) {
        int visibility = isVisible ? View.VISIBLE : View.INVISIBLE;
        urlEditText.setVisibility(visibility);
        saveUrlButton.setVisibility(visibility);
        refreshStreamButton.setVisibility(visibility);
        startStreamButton.setVisibility(visibility);
        stopStreamButton.setVisibility(visibility);
    }

    private void showToast(String message) {
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRtspStatusConnecting() {
    }

    @Override
    public void onRtspStatusConnected() {
    }

    @Override
    public void onRtspStatusDisconnecting() {
    }

    @Override
    public void onRtspStatusDisconnected() {
    }

    @Override
    public void onRtspStatusFailedUnauthorized() {
    }

    @Override
    public void onRtspStatusFailed(@Nullable String message) {
    }

    @Override
    public void onRtspFirstFrameRendered() {
    }
}
