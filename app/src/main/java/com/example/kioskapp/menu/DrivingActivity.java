package com.example.kioskapp.menu;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.os.Bundle;
import android.util.Log;

import com.example.kioskapp.R;
import com.example.kioskapp.camera.CameraSource;
import com.example.kioskapp.facedetector.FaceDetectorProcessor;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.mlkit.vision.face.Face;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import com.example.kioskapp.camera.CameraSourcePreview;
import com.example.kioskapp.camera.GraphicOverlay;
import com.google.mlkit.vision.face.FaceDetectorOptions;

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

    private String alarmStartTime;
    private long startTimeLong;
    private boolean inCalibrationPeriod;

    private boolean isPaused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // To remove the title bar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driving);

        // Initializes camera interface and surface texture view that shows camera feed
        cameraPreview = findViewById(R.id.preview);
        graphicOverlay = findViewById(R.id.faceOverlay);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    Log.i(TAG, "Checking eyes");
                        checkEyes();
                }
            }
        }).start();

        cameraPreview.drivingActivity = this;
        defaultOptions =
                new FaceDetectorOptions.Builder()
                        .setMinFaceSize(0.3f)
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();

        createCameraSource();

        inCalibrationPeriod = true;
        startTimeLong = System.currentTimeMillis();

        // Request camera permission if it has not already been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
        }

    }

    public boolean isInCalibrationPeriod() {
        return inCalibrationPeriod;
    }
    public void exitCalibrationPeriod() {
        Log.d(TAG, "EXITING calibration period");
        inCalibrationPeriod = false;
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

        cameraSource.setFacing(facing);
        cameraSource.setMachineLearningFrameProcessor(
                new FaceDetectorProcessor(this, defaultOptions));
    }

    /**
     * Actually starts the cameraPreview, passing in the non-null cameraSource and graphicOverlay
     */
    private void startCameraSource() {
        // Checks if Google play services are available
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dlg = GoogleApiAvailability.getInstance().getErrorDialog(this,
                    code, PLAY_SERVICES_UNAVAILABLE_CODE);
            dlg.show();
        }

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
        if (!isPaused) {
            startCameraSource();
        }
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
    }

    public void checkEyes() {
        List<Face> faces = FaceDetectorProcessor.getmFaces();
        if (faces != null){
            for (Face face : faces) {
                mediaPlayer = getMediaPlayer();
                if (mediaPlayer != null) {
                    Log.d(TAG, "Media player playing: " + mediaPlayer.isPlaying());
                } else {
                    Log.d(TAG, "Media player null");
                }
                if (face != null){
                    if (face.getRightEyeOpenProbability() < 0.3f && face.getLeftEyeOpenProbability() < 0.3f) {
                        // Starts the eyes closed "timer" because eyes need to be closed for 1 second for an alarm to go off
                        if (eyesClosedStartTime == 0 && (mediaPlayer == null || !mediaPlayer.isPlaying())) {
                            Log.d(TAG, "Starting eyes closed timer");
                            eyesClosedStartTime = System.currentTimeMillis();
                        } else if (System.currentTimeMillis() > eyesClosedStartTime + eyesClosedAlarmDelay && !setOffAlarm &&
                                (mediaPlayer == null || !mediaPlayer.isPlaying())) {

                            Log.d(TAG, "Starting alarm");

                            // setOffAlarm exists to ensure another alarm doesn't go off after the first one ends
                            setOffAlarm = true;
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
        Log.d(TAG, "Call to Trip activity start alarm");

        // Initializes the media player to play sounds and starts it
        mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
        mediaPlayer.start();

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
    }

    public MediaPlayer getMediaPlayer() {
        return mediaPlayer;
    }
    public long getStartTimeLong() {
        return startTimeLong;
    }

    public void createMediaPlayer(){
        mediaPlayer = MediaPlayer.create(this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE));
    }

}
