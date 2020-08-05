package com.example.faceapp.menu;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.faceapp.R;
import com.example.faceapp.camera.CameraSource;
import com.example.faceapp.camera.CameraSourcePreview;
import com.example.faceapp.camera.GraphicOverlay;
import com.example.faceapp.facedetector.FaceDetectorProcessor;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.IOException;
import java.util.List;

/**
 * First, added MLKit dependency, then imported needed classes for face contour detection
 * Used detected faces to get probability of open eyes and put alarm if prob. is less than value (0.3)
 */

public class DrivingActivity extends AppCompatActivity {
    private final String TAG = "DrivingActivity";

    private static final int PLAY_SERVICES_UNAVAILABLE_CODE = 9001;
    private static final int ALARM_DURATION_MILLISECONDS = 8000;
    private static final int ALARM_INTERVAL_MILLISECONDS = 1000;

    public MediaPlayer mediaPlayer;
    private CountDownTimer alarmTimer;
    private int eyesClosedAlarmDelay = 1000;
    private long eyesClosedStartTime = 0;
    private boolean setOffAlarm = false;

    private FaceDetectorOptions defaultOptions;
    private CameraSource cameraSource = null;
    private CameraSourcePreview cameraPreview;
    private GraphicOverlay graphicOverlay;
    private ImageView sleepAlert;
    private TextView alertOverlay;

    private String alarmStartTime;
    private long startTimeLong;
    private boolean threadActive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Initializes camera interface and surface texture view that shows camera feed
        cameraPreview = findViewById(R.id.preview);
        graphicOverlay = findViewById(R.id.faceOverlay);
        sleepAlert = findViewById(R.id.sleep_alert);
        alertOverlay = findViewById(R.id.alert_overlay);
        threadActive = true;


        // Runs checkEyes method every 300ms
        // Should be decreased for lower delays when alerting driver
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (threadActive) {
                    try {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                checkEyes();
                            }
                        });
                        Thread.sleep(300);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "Checking eyes");

                }
            }
        }).start();

        cameraPreview.activity = this;
        defaultOptions =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        createCameraSource();

        startTimeLong = System.currentTimeMillis();

        // Request camera permission if it has not already been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
        }

    }

    /**
     * Initializes the cameraSource instance variable with 30fps, 320x240 px resolution, facing front, and autofocus
     * enabled. Also creates the FaceDetector object and passes that into the CameraSource
     */
    private void createCameraSource() {

        int facing = CameraSource.CAMERA_FACING_FRONT;

        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(this, graphicOverlay);
        }

        CameraSource.setFacing(facing);
        cameraSource.setMachineLearningFrameProcessor(
                new FaceDetectorProcessor(this, defaultOptions));
    }

    /**
     * Actually starts the cameraPreview, passing in the non-null cameraSource and graphicOverlay
     */
    private void startCameraSource() {

        if (cameraSource != null) {
            try {
                cameraPreview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }

    }

    @Override
    /**
     * Starts the camera preview again when the app is in the foreground
     */
    protected void onResume() {
        super.onResume();
        startCameraSource();

    }

    @Override
    /**
     * Stops the cameraPreview when the app is in the background
     */
    protected void onPause() {
        super.onPause();
        if (cameraPreview != null) {
            cameraPreview.stop();
        }
        threadActive = false;
    }

    @Override
    /**
     * Releases the cameraSource right before the activity is destroyed
     */
    protected void onDestroy() {
        super.onDestroy();

        if (cameraSource != null) {
            cameraSource.release();
        }
        threadActive = false;
    }

    public void checkEyes() {
        List<Face> faces = FaceDetectorProcessor.getmFaces();
        if (faces != null) {
            for (Face face : faces) {
                mediaPlayer = getMediaPlayer();
                if (mediaPlayer != null) {
                    Log.d(TAG, "Media player playing: " + mediaPlayer.isPlaying());
                } else {
                    Log.d(TAG, "Media player null");
                }
                if (face != null && face.getRightEyeOpenProbability() != null && face.getLeftEyeOpenProbability() != null) {
                    if (face.getRightEyeOpenProbability() < 0.3f && face.getLeftEyeOpenProbability() < 0.3f ) {
                        // Starts the eyes closed "timer" because eyes need to be closed for 1 second for an alarm to go off
                        if (eyesClosedStartTime == 0 && (mediaPlayer == null || !mediaPlayer.isPlaying())) {
                            Log.d(TAG, "Starting eyes closed timer");
                            eyesClosedStartTime = System.currentTimeMillis();
                        } else if (System.currentTimeMillis() > eyesClosedStartTime + eyesClosedAlarmDelay && !setOffAlarm &&
                                (mediaPlayer == null || !mediaPlayer.isPlaying())) {

                            Log.d(TAG, "Starting alarm");

                            // setOffAlarm exists to ensure another alarm doesn't go off after the first one ends
                            setOffAlarm = true;
                            mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
                            startAlarm();
                        }
                    } else {
                        Log.d(TAG, "Stopping alarm and eyes closed timer");
                        eyesClosedStartTime = 0;
                        setOffAlarm = false;
                        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                            stopAlarm();
                        }
                    }
                }
            }
        }

    }

    /**
     * Called when an alarm goes off. Initializes the mediaPlayer to play the sound and begins playing it, starts a
     * timer on a new thread to stop the alarm after ALARM_DURATION_MILLISECONDS, and writes to the database the time
     * at which the alarm was started
     */
    public void startAlarm() {
        Log.d(TAG, "Starting alarm");

        // Initializes the media player to play sounds and starts it

        mediaPlayer.start();
        alertOverlay.setVisibility(View.VISIBLE);
        sleepAlert.setVisibility(View.VISIBLE);

        // In main thread, starts the timer to turn the alarm off after ALARM_DURATION_MILLISECONDS seconds
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                alarmTimer = new CountDownTimer(ALARM_DURATION_MILLISECONDS, ALARM_INTERVAL_MILLISECONDS) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        stopAlarm();
                    }
                }.start();
            }
        });
    }

    /**
     * Called when an alarm is supposed to be stopped. Stops the mediaPlayer and writes to the database the time at
     * which the alarm was stopped
     */
    public void stopAlarm() {

        // Stop the media player and alarm timer if they are not null
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
        }
        mediaPlayer = null;

        if (alarmTimer != null) {
            alarmTimer.cancel();
        }
        alarmTimer = null;

        alarmStartTime = null;
        alertOverlay.setVisibility(View.INVISIBLE);
        sleepAlert.setVisibility(View.INVISIBLE);
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }

    public long getStartTimeLong() {
        return startTimeLong;
    }

    public void createMediaPlayer() {
        mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
    }

}
