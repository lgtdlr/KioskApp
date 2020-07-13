package com.example.kioskapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.opencv.android.CameraBridgeViewBase.CAMERA_ID_BACK;
import static org.opencv.android.CameraBridgeViewBase.CAMERA_ID_FRONT;

public class LiveTrainActivity extends CameraActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static OkHttpClient client = new OkHttpClient();

    private String URL = "";
    private String TRAIN_URL = "http://192.168.102.158:5000/face/v1.0/persongroups/5000/train";


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
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view2);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view2);

        Bundle extras = getIntent().getExtras();

        if(extras != null) {
            URL = "http://192.168.102.158:5000/face/v1.0/persongroups/5000/persons/"
                    + extras.getString("personId")
                    + "/persistedFaces";

            Toast.makeText(this, "Add faces for " + extras.getString("myName"), Toast.LENGTH_LONG).show();
        }

        if(!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseCallback);
        } else {
            baseCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
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
                    super.onManagerConnected(status);
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
        Mat mRgbaT = mRgba.t();
//        if (cameraIndex == CAMERA_ID_FRONT){
//            Core.flip(mRgba, mRgba, 1);
//        }
        Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());


        mBitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, mBitmap);

        return mRgba;
    }

    public void onCameraSwitch(View view) {
        if(view.getId() == R.id.camera_button_id6) {
            if (cameraIndex == CAMERA_ID_FRONT){
                cameraIndex = CAMERA_ID_BACK;
            } else {
                cameraIndex = CAMERA_ID_FRONT;
            }
            mOpenCvCameraView.disableView();
            mOpenCvCameraView.setCameraIndex(cameraIndex);
            mOpenCvCameraView.enableView();
        }

        if(view.getId() == R.id.camera_button_id5) {
            if (cameraIndex == CAMERA_ID_FRONT){
                new PostImageRequest().execute(mBitmap);
            } else {
                new PostImageRequest().execute(mBitmap);
            }
        }

        if(view.getId() == R.id.train_id) {
            new TrainRequest().execute("");
        }
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

    public void onCameraIdentifyButtonClick(View view) {
        Intent intent = new Intent(this, RealCameraIdentifyActivity.class);
        startActivity(intent);
    }

    public void onCameraDetectButtonClick(View view) {
        Intent intent = new Intent(this, DetectCameraActivity.class);
        startActivity(intent);
    }

    public void onBackClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private class PostImageRequest extends AsyncTask<Bitmap, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(LiveTrainActivity.this);
            p.setMessage("Please wait... uploading");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            p.hide();

            Toast.makeText(LiveTrainActivity.this, "Face added!", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            Bitmap bitmap = bitmaps[0];
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            try {
                RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("file", "img.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
                        .addFormDataPart("returnFaceId", "true")
                        .addFormDataPart("returnFaceAttributes", "*")
                        .addFormDataPart("returnFaceLandmarks", "true")
                        .addFormDataPart("returnRecognitionModel", "true")
                        .build();

                Request request = new Request.Builder()
                        .url(URL)
                        .post(requestBody)
                        .addHeader("Accept", "application/json; charset=utf-8")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    return response.body().string();
                }
            } catch (Exception e) {
                return "Error";
            }
        }
    }

    private class TrainRequest extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(LiveTrainActivity.this);
            p.setMessage("Please wait... uploading");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            p.hide();

            Toast.makeText(LiveTrainActivity.this, "Training is being performed on server..", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                HttpURLConnection connection = (HttpURLConnection) new URL(TRAIN_URL).openConnection();
                connection.setRequestProperty("accept", "application/json; charset=utf-8");
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String line = "";
                String str = "";

                while((line = reader.readLine()) != null) {
                    str += line;
                }

                return str;
            } catch (IOException e) {
                e.printStackTrace();
                return "Error";
            }
        }
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
}