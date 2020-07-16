package com.example.kioskapp.menu;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kioskapp.R;

import org.json.JSONArray;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
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

public class LiveIdentifyActivity extends CameraActivity implements CvCameraViewListener2 {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String ID_URL = "http://192.168.102.158:5000/face/v1.0/detect?recognitionModel=Recognition_02";
    private static final String IDENTIFY_URL = "http://192.168.102.158:5000/face/v1.0/identify";
    private static final String NAME_URL = "http://192.168.102.158:5000/face/v1.0/persongroups/5000/persons/";
    private static OkHttpClient client = new OkHttpClient();
    private static Bitmap mBitmap;
    ProgressDialog p;
    JavaCameraView javaCameraView;
    File cascFile;
    CascadeClassifier faceDetector;
    TextView fpsTextView;
    int fps;
    long startTime = 0;
    long currentTime = 1000;
    int cameraIndex = CAMERA_ID_FRONT;
    private CameraBridgeViewBase mOpenCvCameraView;
    private Boolean buttonPressed = false;
    private Mat mRgba, mGray;
    private BaseLoaderCallback baseCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
                    File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                    cascFile = new File(cascadeDir, "haarcascade_frontalface_alt2.xml");

                    try {
                        FileOutputStream fos = new FileOutputStream(cascFile);
                        byte[] buffer = new byte[4096];
                        int bytesRead;

                        while ((bytesRead = is.read(buffer)) != -1) {
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

                    if (faceDetector.empty()) {
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

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_identify);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mOpenCvCameraView = findViewById(R.id.identify_java_camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        javaCameraView = findViewById(R.id.identify_java_camera_view);
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseCallback);
        } else {
            baseCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        //Draw FPS for portrait activity
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fpsTextView = findViewById(R.id.fps_id);
            }
        });

        javaCameraView.setCvCameraViewListener(this);
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        int orientation = getResources().getConfiguration().orientation;

        if (cameraIndex == CAMERA_ID_FRONT && orientation == Configuration.ORIENTATION_PORTRAIT) {
            Core.flip(mRgba, mRgba, 1);
        }

        //detect faces
        MatOfRect faceDetections = new MatOfRect();
        if (buttonPressed) {
            faceDetector.detectMultiScale(mRgba, faceDetections);


            for (Rect rect : faceDetections.toArray()) {
                Imgproc.rectangle(mRgba, new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(255, 0, 0));
            }
        }

        mBitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, mBitmap);

        //Draw FPS counter
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (currentTime - startTime >= 1000) {
                    fpsTextView.setText("FPS: " + fps);
                    fps = 0;
                    startTime = System.currentTimeMillis();
                }
                currentTime = System.currentTimeMillis();
                fps += 1;

            }
        });

        return mRgba;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(mOpenCvCameraView);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat();
        mGray = new Mat();
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
    }

    public void onRefreshClick(View view) {
        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            // In portrait
            //Rotate bitmap to proper orientation before sending
            new PostCameraRequest().execute(RotateBitmap(mBitmap, 90));

        } else {
            // In lanscape
            new PostCameraRequest().execute(mBitmap);
        }

    }

    //Toggle face detection
    public void onRectToggle(View view) {
        buttonPressed = buttonPressed == false;
    }

    //Switch between front and back camera
    public void onCameraSwitch(View view) {
        Log.i("CameraIndex", "is " + cameraIndex);
        if (cameraIndex == CAMERA_ID_FRONT) {
            cameraIndex = CAMERA_ID_BACK;
        } else {
            cameraIndex = CAMERA_ID_FRONT;
        }
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(cameraIndex);
        mOpenCvCameraView.enableView();
    }

    public void onCameraDetectButtonClick(View view) {
        //Switch to live detect activity
        Intent intent = new Intent(this, LiveDetectActivity.class);
        startActivity(intent);
    }

    public void onCameraTrainButtonClick(View view) {
        //Switch to live train activity
        Intent intent = new Intent(this, LiveTrainActivity.class);
        startActivity(intent);
    }

    public void onBackClick(View view) {
        //Go directly to main menu
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private class PostCameraRequest extends AsyncTask<Bitmap, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(LiveIdentifyActivity.this);
            p.setMessage("Getting face ids...");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            p.hide();
            Log.i("ASHWIN", s);

            try {
                JSONArray parent = new JSONArray(s);
                JSONObject person = parent.getJSONObject(0);
                String faceId = person.getString("faceId");

                new PersonRequest().execute(faceId);
            } catch (Exception e) {


            }
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
                        .url(ID_URL)
                        .post(requestBody)
                        .addHeader("Accept", "application/json; charset=utf-8")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    return response.body().string();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "Error";
            }

        }
    }

    private class PersonRequest extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(LiveIdentifyActivity.this);
            p.setMessage("Acquiring person ids...");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.i("ASHWIN", s);

            p.hide();
            try {
                JSONArray parent = new JSONArray(s);
                JSONArray candidates = parent.getJSONObject(0).getJSONArray("candidates");
                if (candidates.length() == 0) {
                    Toast.makeText(LiveIdentifyActivity.this, "Unknown person detected", Toast.LENGTH_SHORT).show();
                } else {
                    String personId = candidates.getJSONObject(0).getString("personId");
                    new NameRequest().execute(personId);
                }


            } catch (Exception e) {
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            JSONObject json = new JSONObject();
            try {
                json.put("confidenceThreshold", 0.5);
                JSONArray jsonArray = new JSONArray();
                jsonArray.put(strings[0]);
                json.put("faceIds", jsonArray);
                json.put("PersonGroupId", "5000");
                json.put("maxNumOfCandidatesReturned", 1);
            } catch (Exception e) {
            }


            RequestBody requestBody = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url(IDENTIFY_URL)
                    .post(requestBody)
                    .addHeader("Accept", "application/json; charset=utf-8")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
                return "Error";
            }
        }
    }

    private class NameRequest extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(LiveIdentifyActivity.this);
            p.setMessage("Getting identity...");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            p.hide();

            try {
                JSONObject parent = new JSONObject(s);
                String name = parent.getString("name");
                Toast.makeText(LiveIdentifyActivity.this, "Hello " + name, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
            }
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                String url = NAME_URL + strings[0];
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestProperty("Accept", "application/json; charset=utf-8");
                connection.setRequestMethod("GET");

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

                String line = "";
                String str = "";

                while ((line = reader.readLine()) != null) {
                    str += line;
                }

                return str;
            } catch (Exception e) {
                return "Error";
            }
        }
    }

}