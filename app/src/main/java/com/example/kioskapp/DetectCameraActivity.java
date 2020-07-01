package com.example.kioskapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
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

public class DetectCameraActivity extends CameraActivity implements CvCameraViewListener2 {

    private static final String BASE_URL = "http://192.168.102.158:5000/face/v1.0/detect?returnFaceAttributes=*";
    private static final int PICK_IMAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    private static OkHttpClient client = new OkHttpClient();

    ProgressDialog p;

    private CameraBridgeViewBase mOpenCvCameraView;
    JavaCameraView javaCameraView;
    File cascFile;
    CascadeClassifier faceDetector;
    private static Bitmap mBitmap;
    private Boolean buttonPressed = true;

    private Mat mRgba, mGray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.java_camera_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        javaCameraView = (JavaCameraView) findViewById(R.id.java_camera_view);
        if(!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, baseCallback);
        } else {
            baseCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        javaCameraView.setCvCameraViewListener(this);
    }



    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        //detect faces

        MatOfRect faceDetections = new MatOfRect();

        if (buttonPressed){
            faceDetector.detectMultiScale(mRgba, faceDetections);


            for(Rect rect: faceDetections.toArray()) {
                Imgproc.rectangle(mRgba, new Point(rect.x, rect.y),
                        new Point(rect.x + rect.width, rect.y + rect.height),
                        new Scalar(255, 0, 0));
            }
        }

//        Utils.matToBitmap(mRgba, mBitmap);
        return mRgba;
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

    public void onCameraViewStarted (int width, int height) {
        mRgba = new Mat();
        mGray = new Mat();
    }

    public void onCameraViewStopped () {
        mRgba.release();
        mGray.release();
    }

    public void onRefreshClick(View view) {
//        new PostCameraRequest().execute(mBitmap);
        if (buttonPressed == false){
            buttonPressed = true;
        } else {
            buttonPressed =false;
        }

    }

//    private class PostCameraRequest extends AsyncTask<Bitmap, String, String> {
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            p = new ProgressDialog(DetectCameraActivity.this);
//            p.setMessage("Please wait... uploading");
//            p.setIndeterminate(false);
//            p.setCancelable(false);
//            p.show();
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
//            super.onPostExecute(s);
//                Log.i("TAG", s);
//                try {
//                    JSONArray parent = new JSONArray(s);
//                    LinkedList<JSONObject> rectList = new LinkedList<>();
//
//                    imageBitmap.recycle();
//                    setUiAfterUpdate(s, parent);
//                } catch (Exception e) {
//                    Log.i("TAG", "errror with JSON");
//                }
//        }
//
//        @Override
//        protected String doInBackground(ImageView... imageViews) {
//            Bitmap bitmap = ((BitmapDrawable) imageViews[0].getDrawable()).getBitmap();
//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
//            byte[] byteArray = stream.toByteArray();
//
//            try {
//                RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
//                        .addFormDataPart("file", "img.jpg", RequestBody.create(MediaType.parse("image/*jpg"), byteArray))
//                        .addFormDataPart("returnFaceId", "true")
//                        .addFormDataPart("returnFaceAttributes", "*")
//                        .addFormDataPart("returnFaceLandmarks", "true")
//                        .addFormDataPart("returnRecognitionModel", "true")
//                        .build();
//
//                Request request = new Request.Builder()
//                        .url(BASE_URL)
//                        .post(requestBody)
//                        .addHeader("Accept", "application/json; charset=utf-8")
//                        .build();
//
//                try (Response response = client.newCall(request).execute()) {
//                    return response.body().string();
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                return "Error";
//            }
//        }
//    }
    private void setUiAfterUpdate(String s, JSONArray parent) throws JSONException {
        ArrayList<Face> faces = new ArrayList<>();
        LinkedList<JSONObject> rectList = new LinkedList<>();

        for(int i=0; i < parent.length() ; i++) {
            JSONObject json_data = parent.getJSONObject(i);
            JSONObject faceAttributes = json_data.getJSONObject("faceAttributes");
            JSONObject emotions = faceAttributes.getJSONObject("emotion");
            JSONObject rectangle = json_data.getJSONObject("faceRectangle");
            rectList.add(rectangle);
            Bitmap faceBitmap = Bitmap.createBitmap(mBitmap, rectangle.getInt("left"),
                    rectangle.getInt("top"),
                    rectangle.getInt("width"),
                    rectangle.getInt("height"));
            int age = faceAttributes.getInt("age");
            String gender = faceAttributes.getString("gender");
            Log.i("Adding faces", "Wait...");
            faces.add(new Face(faceBitmap, "Age: " + age, gender, getEmotion(emotions), getEmotionScore(emotions)));
            Log.i("Adding faces", "Success");
        }
        ListView listView = (ListView)findViewById(R.id.results_list);
        FaceListAdapter adapter = new FaceListAdapter(DetectCameraActivity.this, R.layout.detect_adapter_view_layout, faces);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();


    }

    private String getEmotion(JSONObject attributes) throws JSONException {
        Log.i("Parsing emotions", "...");
        String emotionType = "";
        double emotionValue = 0.0;

        if (attributes.getDouble("anger") > emotionValue)
        {
            emotionValue = attributes.getDouble("anger");
            emotionType = "Anger";
        }
        if (attributes.getDouble("contempt") > emotionValue)
        {
            emotionValue = attributes.getDouble("contempt");
            emotionType = "Contempt";
        }
        if (attributes.getDouble("disgust") > emotionValue)
        {
            emotionValue = attributes.getDouble("disgust");
            emotionType = "Disgust";
        }
        if (attributes.getDouble("fear") > emotionValue)
        {
            emotionValue = attributes.getDouble("fear");
            emotionType = "Fear";
        }
        if (attributes.getDouble("happiness") > emotionValue)
        {
            emotionValue = attributes.getDouble("happiness");
            emotionType = "Happiness";
        }
        if (attributes.getDouble("neutral") > emotionValue)
        {
            emotionValue = attributes.getDouble("neutral");
            emotionType = "Neutral";
        }
        if (attributes.getDouble("sadness") > emotionValue)
        {
            emotionValue = attributes.getDouble("sadness");
            emotionType = "Sadness";
        }
        if (attributes.getDouble("surprise") > emotionValue)
        {
            emotionValue = attributes.getDouble("surprise");
            emotionType = "Surprise";
        }
        Log.i("Parsing emotions", "Success");
        return emotionType;
    }

    private double getEmotionScore(JSONObject attributes) throws JSONException {
        Log.i("Parsing emotions for score", "...");
        double emotionValue = 0.0;

        if (attributes.getDouble("anger") > emotionValue)
        {
            emotionValue = attributes.getDouble("anger");
        }
        if (attributes.getDouble("contempt") > emotionValue)
        {
            emotionValue = attributes.getDouble("contempt");
        }
        if (attributes.getDouble("disgust") > emotionValue)
        {
            emotionValue = attributes.getDouble("disgust");
        }
        if (attributes.getDouble("fear") > emotionValue)
        {
            emotionValue = attributes.getDouble("fear");
        }
        if (attributes.getDouble("happiness") > emotionValue)
        {
            emotionValue = attributes.getDouble("happiness");
        }
        if (attributes.getDouble("neutral") > emotionValue)
        {
            emotionValue = attributes.getDouble("neutral");
        }
        if (attributes.getDouble("sadness") > emotionValue)
        {
            emotionValue = attributes.getDouble("sadness");
        }
        if (attributes.getDouble("surprise") > emotionValue)
        {
            emotionValue = attributes.getDouble("surprise");
        }
        Log.i("Parsing emotions for score", "Success");

        Log.i("Emotion score", "is " + emotionValue);
        return emotionValue;
    }
}