package com.ahmet.lowlatencyrtspplayer;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.Toast;

import com.ahmet.lowlatencyrtspplayer.databinding.ActivityMainBinding;
import com.ahmet.lowlatencyrtspplayer.rtsp.widget.RtspSurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class MainActivity extends AppCompatActivity implements RtspSurfaceView.RtspStatusListener {
    private ActivityMainBinding binding;
    private RtspManager rtspManager;
    private SharedPreferencesManager sharedPreferencesManager;
    private UIController uiController;

    private static final String SHARED_PREFS_NAME = "RTSPPrefs";
    public static final String RTSP_URL_KEY = "RTSP_URL";
    public static final String DEFAULT_RTSP_URL = "rtsp://192.168.3.10:554/user=elektroland&password=EXLXEXKX&channel=2&stream=0.sdp?";

    private boolean isControlsVisible = false;
    private boolean isLoggingEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        sharedPreferencesManager = new SharedPreferencesManager(this, SHARED_PREFS_NAME);
        rtspManager = new RtspManager(binding.surfaceView, this);
        uiController = new UIController(this, binding, sharedPreferencesManager, rtspManager);

        String rtspUrl = sharedPreferencesManager.loadRtspUrl(DEFAULT_RTSP_URL);
        uiController.initialize(rtspUrl, isControlsVisible, isLoggingEnabled);
    }

    @Override
    public void onRtspStatusConnecting() {
        uiController.log("RTSP status: Connecting");
    }

    @Override
    public void onRtspStatusConnected() {
        uiController.log("RTSP status: Connected");
    }

    @Override
    public void onRtspStatusDisconnecting() {
        uiController.log("RTSP status: Disconnecting");
    }

    @Override
    public void onRtspStatusDisconnected() {
        uiController.log("RTSP status: Disconnected");
    }

    @Override
    public void onRtspStatusFailedUnauthorized() {
        uiController.log("RTSP status: Failed - Unauthorized");
    }

    @Override
    public void onRtspStatusFailed(@Nullable String message) {
        uiController.log("RTSP status: Failed - " + message);
    }

    @Override
    public void onRtspFirstFrameRendered() {
        uiController.log("RTSP first frame rendered");
    }
}
