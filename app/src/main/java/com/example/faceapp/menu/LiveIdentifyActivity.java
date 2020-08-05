package com.example.faceapp.menu;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.faceapp.FrameMetadata;
import com.example.faceapp.R;
import com.example.faceapp.camera.CameraSource;
import com.example.faceapp.camera.CameraSourcePreview;
import com.example.faceapp.camera.GraphicOverlay;
import com.example.faceapp.facedetector.FaceDetectorProcessor;
import com.example.faceapp.utils.BitmapUtils;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LiveIdentifyActivity extends AppCompatActivity {

    private final String TAG = "LiveIdentifyActivity";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String ID_URL = "http://192.168.102.158:5000/face/v1.0/detect?recognitionModel=Recognition_02";
    private static final String IDENTIFY_URL = "http://192.168.102.158:5000/face/v1.0/identify";
    private static final String NAME_URL = "http://192.168.102.158:5000/face/v1.0/persongroups/5000/persons/";
    private static OkHttpClient client = new OkHttpClient();
    private Bitmap mBitmap;
    private FaceDetectorOptions defaultOptions;
    private CameraSource cameraSource = null;
    private CameraSourcePreview cameraPreview;
    private GraphicOverlay graphicOverlay;
    private ProgressDialog p;
    private int facing;

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

        // Initializes camera interface and surface texture view that shows camera feed
        cameraPreview = findViewById(R.id.preview);
        graphicOverlay = findViewById(R.id.faceOverlay);
        facing = CameraSource.CAMERA_FACING_FRONT;

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

    public void onRefreshClick(View view) {
        mBitmap = BitmapUtils.getBitmap(CameraSource.getData(), new FrameMetadata.Builder()
                .setWidth(CameraSource.getPreviewSize().getWidth())
                .setHeight(CameraSource.getPreviewSize().getHeight())
                .setRotation(CameraSource.getRotationDegrees())
                .build());
        new PostCameraRequest().execute(mBitmap);
    }

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