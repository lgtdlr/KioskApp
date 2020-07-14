package com.example.kioskapp;

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
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.opencv.android.CameraBridgeViewBase.CAMERA_ID_BACK;
import static org.opencv.android.CameraBridgeViewBase.CAMERA_ID_FRONT;

public class LiveDetectActivity extends CameraActivity implements CvCameraViewListener2 {

    private static final String BASE_URL = "http://192.168.102.158:5000/face/v1.0/detect?returnFaceAttributes=*";

    private static OkHttpClient client = new OkHttpClient();
    private static Bitmap mBitmap;
    ProgressDialog p;
    JavaCameraView javaCameraView;
    File cascFile;
    CascadeClassifier faceDetector;
    int cameraIndex = CAMERA_ID_FRONT;
    TextView fpsTextView;
    int fps;
    long startTime = 0;
    long currentTime = 1000;
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

    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_detect);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);

        //Load OpenCV
        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseCallback);
        } else {
            try {
                baseCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Draw FPS for portrait activity
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fpsTextView = (TextView) findViewById(R.id.fps_id);
            }
        });


        javaCameraView.setCvCameraViewListener(this);
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT && cameraIndex == CAMERA_ID_FRONT) {
            // In portrait
            //Without this feed is upside down when using front portrait camera
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
            //Bitmap is rotated before being so it has the right orientation
            new PostCameraRequest().execute(RotateBitmap(mBitmap, 90));

        } else {
            // In landscape
            //No need for rotation
            new PostCameraRequest().execute(mBitmap);
        }
    }

    //Toggle face rectangles
    public void onRectToggle(View view) {
        buttonPressed = buttonPressed == false;
    }

    //Switch between front and back camera
    public void onCameraSwitch(View view) {
        if (cameraIndex == CAMERA_ID_FRONT) {
            cameraIndex = CAMERA_ID_BACK;
        } else {
            cameraIndex = CAMERA_ID_FRONT;
        }
        mOpenCvCameraView.disableView();
        mOpenCvCameraView.setCameraIndex(cameraIndex);
        mOpenCvCameraView.enableView();
    }

    public void onCameraTrainButtonClick(View view) {
        //Switch to live train activity
        Intent intent = new Intent(this, LiveTrainActivity.class);
        startActivity(intent);
    }

    public void onCameraIdentifyButtonClick(View view) {
        //Switch to live identify activity
        Intent intent = new Intent(this, LiveIdentifyActivity.class);
        startActivity(intent);
    }

    public void onBackClick(View view) {
        //Go back to main menu directly
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    private void setUiAfterUpdate(JSONArray parent) throws JSONException {
        ArrayList<Face> faces = new ArrayList<>();
        LinkedList<JSONObject> rectList = new LinkedList<>();

        int orientation = getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mBitmap = (RotateBitmap(mBitmap, 90));
        }
        for (int i = 0; i < parent.length(); i++) {
            JSONObject json_data = parent.getJSONObject(i);
            JSONObject faceAttributes = json_data.getJSONObject("faceAttributes");
            JSONObject emotions = faceAttributes.getJSONObject("emotion");
            JSONObject rectangle = json_data.getJSONObject("faceRectangle");
            rectList.add(rectangle);

            Bitmap faceBitmap;
            faceBitmap = Bitmap.createBitmap(mBitmap, rectangle.getInt("left"),
                    rectangle.getInt("top"),
                    rectangle.getInt("width"),
                    rectangle.getInt("height"));

            int age = faceAttributes.getInt("age");
            String gender = faceAttributes.getString("gender");
            Log.i("Adding faces", "Wait...");
            ArrayList<Emotion> emotionsList = getEmotions(emotions);
            Log.i("EMOTIONS", emotionsList.get(0).getType() + " " + emotionsList.get(0).getValue());
            Log.i("EMOTIONS", emotionsList.get(1).getType() + " " + emotionsList.get(1).getValue());
            Log.i("EMOTIONS", emotionsList.get(2).getType() + " " + emotionsList.get(2).getValue());


            faces.add(new Face(faceBitmap, "Age: " + age, gender, emotionsList.get(0).getType(),
                    emotionsList.get(0).getValue(), emotionsList.get(1).getType(), emotionsList.get(1).getValue(),
                    emotionsList.get(2).getType(), emotionsList.get(2).getValue()));

            Log.i("Adding faces", "Success");
        }
        ListView listView = (ListView) findViewById(R.id.results_list);
        FaceListAdapter adapter = new FaceListAdapter(LiveDetectActivity.this, R.layout.detect_adapter_view_layout, faces);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();


    }

    private ArrayList<Emotion> getEmotions(JSONObject attributes) throws JSONException {
        ArrayList<Emotion> emotionsList = new ArrayList<>();

        emotionsList.add(new Emotion("anger", attributes.getDouble("anger")));
        emotionsList.add(new Emotion("contempt", attributes.getDouble("contempt")));
        emotionsList.add(new Emotion("disgust", attributes.getDouble("anger")));
        emotionsList.add(new Emotion("fear", attributes.getDouble("fear")));
        emotionsList.add(new Emotion("happiness", attributes.getDouble("happiness")));
        emotionsList.add(new Emotion("neutral", attributes.getDouble("neutral")));
        emotionsList.add(new Emotion("sadness", attributes.getDouble("sadness")));
        emotionsList.add(new Emotion("surprise", attributes.getDouble("surprise")));

        Collections.sort(emotionsList);

        return emotionsList;
    }

    private class PostCameraRequest extends AsyncTask<Bitmap, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.i("TAG", s);
            try {
                JSONArray parent = new JSONArray(s);

                //Populate ListView with received JSON info
                setUiAfterUpdate(parent);
            } catch (Exception e) {
                Log.i("Failed to update UI", e.getLocalizedMessage());
            }
        }

        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmaps[0].compress(Bitmap.CompressFormat.JPEG, 100, stream);
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
                        .url(BASE_URL)
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
}