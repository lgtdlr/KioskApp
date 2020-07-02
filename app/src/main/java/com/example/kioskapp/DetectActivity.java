package com.example.kioskapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DetectActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://192.168.102.158:5000/face/v1.0/detect?returnFaceAttributes=*";
    private static final int PICK_IMAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    private static TextView postResponseText;
    private static ImageView imageSelected;

    private static OkHttpClient client = new OkHttpClient();

    ProgressDialog p;

    public static Bitmap drawRectangles(Bitmap original, LinkedList<JSONObject> rectList) {
        Bitmap bitmap = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);

        for (JSONObject rect : rectList) {
            try {
                canvas.drawRect(
                        rect.getInt("left"),
                        rect.getInt("top"),
                        rect.getInt("left") + rect.getInt("width"),
                        rect.getInt("top") + rect.getInt("height"),
                        paint
                );
            } catch (Exception e) {
            }
        }

        return bitmap;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        imageSelected = (ImageView) findViewById(R.id.imageSelected);
    }

    private void selectImage() {
        final CharSequence[] items={"Camera","Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(DetectActivity.this);
        builder.setTitle("Add Image");

        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (items[i].equals("Camera")) {
                    if (ContextCompat.checkSelfPermission(DetectActivity.this, Manifest.permission.CAMERA) == -1){
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
                        return;
                    }

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);


                } else if (items[i].equals("Gallery")) {
                    if (ContextCompat.checkSelfPermission(DetectActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == -1){
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        return;
                    }
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    startActivityForResult(intent, PICK_IMAGE);

                } else if (items[i].equals("Cancel")) {
                    dialogInterface.dismiss();
                }
            }
        });
        builder.show();
    }

    public void onUploadClick(View view) {
        selectImage();
    }

    public void onCameraClick(View view) {
        selectImage();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            final String fullPhotoPath = ImageSelect.getPath(this, fullPhotoUri);
            imageSelected.setImageURI(fullPhotoUri);

            File file = new File(fullPhotoPath);


            new PostImageRequest().execute(file);
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageSelected.setImageBitmap(imageBitmap);

            new PostCameraRequest().execute(imageSelected);
        }
    }

    private class PostCameraRequest extends AsyncTask<ImageView, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(DetectActivity.this);
            p.setMessage("Please wait... uploading");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (imageSelected != null) {
                p.hide();
                imageSelected.isOpaque();
                Log.i("TAG", s);
                try {
                    JSONArray parent = new JSONArray(s);
                    LinkedList<JSONObject> rectList = new LinkedList<>();

                    for (int i = 0; i < parent.length(); i++) {
                        JSONObject rect = parent.getJSONObject(i).getJSONObject("faceRectangle");
                        rectList.add(rect);
                    }

                    Bitmap imageBitmap = ((BitmapDrawable) imageSelected.getDrawable()).getBitmap();
                    imageSelected.setImageBitmap(drawRectangles(imageBitmap, rectList));
                    imageBitmap.recycle();
                    setUiAfterUpdate(s, parent);
                } catch (Exception e) {
                    Log.i("TAG", "errror with JSON");
                }
            } else {
                p.show();
            }
        }

        @Override
        protected String doInBackground(ImageView... imageViews) {
            Bitmap bitmap = ((BitmapDrawable) imageViews[0].getDrawable()).getBitmap();
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

    private class PostImageRequest extends AsyncTask<File, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(DetectActivity.this);
            p.setMessage("Please wait... Uploading");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected String doInBackground(File... files) {
            try {
                RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("file", files[0].getName(), RequestBody.create(files[0], MediaType.get("image/jpeg")))
                        .addFormDataPart("returnFaceId", "true")
                        .addFormDataPart("returnFaceLandmarks", "false")
                        .addFormDataPart("returnFaceAttributes", "*")
                        .addFormDataPart("returnRecognitionModel", "false")
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
                return "Failure";
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (imageSelected != null) {
                p.hide();
                imageSelected.isOpaque();
                Log.i("TAG", s);
                try {
                    JSONArray parent = new JSONArray(s);
                    LinkedList<JSONObject> rectList = new LinkedList<>();

                    for (int i = 0; i < parent.length(); i++) {
                        JSONObject rect = parent.getJSONObject(i).getJSONObject("faceRectangle");
                        rectList.add(rect);
                    }

                    Log.i("Updating screen", "...");
                    setUiAfterUpdate(s, parent);
                    Log.i("Updating screen", "Success");
                    Bitmap imageBitmap = ((BitmapDrawable) imageSelected.getDrawable()).getBitmap();
                    imageSelected.setImageBitmap(drawRectangles(imageBitmap, rectList));
                    imageBitmap.recycle();
                } catch (Exception e) {
                    Log.i("TAG", "errror with JSON");
                }
            } else {
                p.show();
            }
        }
    }

    private void setUiAfterUpdate(String s, JSONArray parent) throws JSONException {
        ArrayList<Face> faces = new ArrayList<>();
        LinkedList<JSONObject> rectList = new LinkedList<>();

        for(int i=0; i < parent.length() ; i++) {
            JSONObject json_data = parent.getJSONObject(i);
            JSONObject faceAttributes = json_data.getJSONObject("faceAttributes");
            JSONObject emotions = faceAttributes.getJSONObject("emotion");
            JSONObject rectangle = json_data.getJSONObject("faceRectangle");
            rectList.add(rectangle);
            Bitmap currentBitmap = ((BitmapDrawable) imageSelected.getDrawable()).getBitmap();
            Bitmap faceBitmap = Bitmap.createBitmap(currentBitmap, rectangle.getInt("left"),
                                                                        rectangle.getInt("top"),
                                                                        rectangle.getInt("width"),
                                                                        rectangle.getInt("height"));
            int age = faceAttributes.getInt("age");
            String gender = faceAttributes.getString("gender");
            Log.i("Adding faces", "Wait...");
            //faces.add(new Face(faceBitmap, "Age: " + age, gender, getEmotion(emotions), getEmotionScore(emotions)));
            Log.i("Adding faces", "Success");
        }
        ListView listView = (ListView)findViewById(R.id.results_list);
        FaceListAdapter adapter = new FaceListAdapter(DetectActivity.this, R.layout.detect_adapter_view_layout, faces);
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