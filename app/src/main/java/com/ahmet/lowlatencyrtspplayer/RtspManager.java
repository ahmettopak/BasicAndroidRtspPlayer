package com.ahmet.lowlatencyrtspplayer;

import android.content.Context;
import android.net.Uri;

import com.ahmet.lowlatencyrtspplayer.rtsp.widget.RtspSurfaceView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Ahmet TOPAK
 * @version 1.0
 * @since 6/25/2024
 */

public class RtspManager {
    private final RtspSurfaceView surfaceView;
    private final Object streamLock = new Object();
    private static final int MAX_STOP_ATTEMPTS = 5;
    private static final long STOP_RETRY_DELAY_MS = 500;

    public RtspManager(RtspSurfaceView surfaceView, RtspSurfaceView.RtspStatusListener listener) {
        this.surfaceView = surfaceView;
        this.surfaceView.setStatusListener(listener);
    }

    public void initialize(String rtspUrl) {
        Uri rtspUri = Uri.parse(rtspUrl);
        surfaceView.init(rtspUri, "", "");
        surfaceView.start(true, true);
    }

    public void startStream(String rtspUrl, Runnable onStreamStarted) {
            synchronized (streamLock) {
                if (surfaceView.isStarted()) {
                    return;
                }

                Uri rtspUri = Uri.parse(rtspUrl);
                surfaceView.init(rtspUri, "", "");
                surfaceView.start(true, true);
                onStreamStarted.run();
            }
    }

    public void stopStream(Runnable onStreamStopped, Runnable onStopFailed) {
            synchronized (streamLock) {
                if (!surfaceView.isStarted()) {
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

                if (stopped) {
                    onStreamStopped.run();
                } else {
                    onStopFailed.run();
                }
            }
    }

    public void refreshStream(String rtspUrl, Runnable onStreamStoppedForRefresh, Runnable onStreamRefreshed) {
            synchronized (streamLock) {
                if (surfaceView.isStarted()) {
                    surfaceView.stop();
                    try {
                        Thread.sleep(STOP_RETRY_DELAY_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    onStreamStoppedForRefresh.run();
                }

                Uri rtspUri = Uri.parse(rtspUrl);
                surfaceView.init(rtspUri, "", "");
                surfaceView.start(true, false);
                onStreamRefreshed.run();
            }
    }
}
