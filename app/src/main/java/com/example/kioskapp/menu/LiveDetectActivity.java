package com.example.kioskapp.menu;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.kioskapp.Emotion;
import com.example.kioskapp.Face;
import com.example.kioskapp.FaceListAdapter;
import com.example.kioskapp.FrameMetadata;
import com.example.kioskapp.R;
import com.example.kioskapp.VisionProcessorBase;
import com.example.kioskapp.camera.CameraSource;
import com.example.kioskapp.camera.CameraSourcePreview;
import com.example.kioskapp.camera.GraphicOverlay;
import com.example.kioskapp.facedetector.FaceDetectorProcessor;
import com.example.kioskapp.utils.BitmapUtils;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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


public class LiveDetectActivity extends AppCompatActivity {

    private final String TAG = "LiveDetectActivity";
    private static final String BASE_URL = "http://192.168.102.158:5000/face/v1.0/detect?returnFaceAttributes=*";
    private FaceDetectorOptions defaultOptions;
    private CameraSource cameraSource = null;
    private CameraSourcePreview cameraPreview;
    private GraphicOverlay graphicOverlay;
    private Bitmap mBitmap;
    int facing;
    int facingBack;
    private FrameMetadata frameMetadata;

    private static OkHttpClient client = new OkHttpClient();
    ProgressDialog p;

    TextView fpsTextView;
    int fps;
    long startTime = 0;
    long currentTime = 1000;
    private Boolean buttonPressed = false;


    public static Bitmap RotateBitmap(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    //----------------------------------------------------------------------------------------
    //----------------------------------------------------------------------------------------


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_detect);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        // Initializes camera interface and surface texture view that shows camera feed
        cameraPreview = findViewById(R.id.preview);
        graphicOverlay = findViewById(R.id.faceOverlay);
        facing = CameraSource.CAMERA_FACING_FRONT;

//        mOpenCvCameraView = findViewById(R.id.java_camera_view);
//        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
//        mOpenCvCameraView.setCvCameraViewListener(this);

//        javaCameraView = findViewById(R.id.java_camera_view);

        //Load OpenCV
//        if (!OpenCVLoader.initDebug()) {
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseCallback);
//        } else {
//            baseCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }

        cameraPreview.activity = this;
        defaultOptions =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .enableTracking()
                        .build();

        createCameraSource();

        // Request camera permission if it has not already been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
        }

        //Draw FPS for portrait activity
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                fpsTextView = findViewById(R.id.fps_id);
            }
        });


        //javaCameraView.setCvCameraViewListener(this);
    }

    /**
     * Initializes the cameraSource instance variable with 30fps, 320x240 px resolution, facing front, and autofocus
     * enabled. Also creates the FaceDetector object and passes that into the CameraSource
     */
    private void createCameraSource() {

        facing = CameraSource.CAMERA_FACING_FRONT;

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


    public void onRefreshClick(View view) {
        mBitmap = BitmapUtils.getBitmap(CameraSource.getData(), new FrameMetadata.Builder()
                .setWidth(CameraSource.getPreviewSize().getWidth())
                .setHeight(CameraSource.getPreviewSize().getHeight())
                .setRotation(CameraSource.getRotationDegrees())
                .build());
        new PostCameraRequest().execute(mBitmap);
    }
//
    //Switch between front and back camera
    public void onCameraSwitch(View view) {

        cameraPreview.stop();

        if (facing == CameraSource.CAMERA_FACING_FRONT) {
            facing = CameraSource.CAMERA_FACING_BACK;
            CameraSource.setFacing(facing);
        } else {
            facing = CameraSource.CAMERA_FACING_FRONT;
            CameraSource.setFacing(facing);
        }

        startCameraSource();

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

//        int orientation = getResources().getConfiguration().orientation;
//        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
//            mBitmap = (RotateBitmap(mBitmap, 90));
//        }
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
        ListView listView = findViewById(R.id.results_list);
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