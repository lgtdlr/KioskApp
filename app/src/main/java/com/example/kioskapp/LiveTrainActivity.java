package com.example.kioskapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;

import static org.opencv.android.CameraBridgeViewBase.CAMERA_ID_BACK;
import static org.opencv.android.CameraBridgeViewBase.CAMERA_ID_FRONT;

public class LiveTrainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static OkHttpClient client = new OkHttpClient();

    private static final String ID_URL = "http://192.168.102.158:5000/face/v1.0/detect?recognitionModel=Recognition_02";
    private static final String IDENTIFY_URL = "http://192.168.102.158:5000/face/v1.0/identify";
    private static final String NAME_URL = "http://192.168.102.158:5000/face/v1.0/persongroups/5000/persons/";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    ProgressDialog p;

    private static ImageView imageSelected;

    private static final int PICK_IMAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    private CameraBridgeViewBase mOpenCvCameraView;
    JavaCameraView javaCameraView;
    File cascFile;
    CascadeClassifier faceDetector;
    private static Bitmap mBitmap;
    private Boolean buttonPressed = false;
    ImageView imageView;
    int cameraIndex = CAMERA_ID_FRONT;

    private Mat mRgba, mGray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_train);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view2);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view2);

        if(!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseCallback);
        } else {
            try {
                baseCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        javaCameraView.setCvCameraViewListener(this);
    }

    private BaseLoaderCallback baseCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    cascFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");

                    try{
                        FileOutputStream fos = new FileOutputStream(cascFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        while((bytesRead = is.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        fos.close();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    faceDetector = new CascadeClassifier(cascFile.getAbsolutePath());

                    if(faceDetector.empty()) {
                        faceDetector = null;
                    } else
                        cascadeDir.delete();

                    javaCameraView.enableView();
                }
                break;

                default: {
                    try {
                        super.onManagerConnected(status);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
        }
    };

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mGray = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        return mRgba;
    }

    public void onCameraSwitch(View view) {
        Log.i("CameraIndex", "is " + cameraIndex);
        if (cameraIndex == CAMERA_ID_FRONT){
            cameraIndex = CAMERA_ID_BACK;
        } else {
            cameraIndex = CAMERA_ID_FRONT;
        }
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(cameraIndex);
        mOpenCvCameraView.enableView();
    }

    @Override
    public void onResume () {
        super.onResume();
    }

    @Override
    public List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    public void onPause ()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy () {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
}