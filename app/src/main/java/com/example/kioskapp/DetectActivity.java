package com.example.kioskapp;

import android.app.ProgressDialog;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class DetectActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://192.168.102.158:5000/face/v1.0/detect";
    private static final int PICK_IMAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    private static TextView postResponseText;
    private static ImageView imageSelected;

    private static OkHttpClient client = new OkHttpClient();

    ProgressDialog p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect);
        postResponseText = (TextView)findViewById(R.id.postResponseText);
        imageSelected = (ImageView) findViewById(R.id.imageSelected);
    }

    public void onUploadClick(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PICK_IMAGE);
        }
    }

    public void onCameraClick(View view) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePictureIntent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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

        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageSelected.setImageBitmap(imageBitmap);
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
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if(imageSelected != null) {
                p.hide();
                imageSelected.isOpaque();
                Log.i("TAG", s);
                try {
                    JSONArray parent = new JSONArray(s);
                    LinkedList<JSONObject> rectList = new LinkedList<>();

                    for(int i = 0; i < parent.length(); i++) {
                        JSONObject rect = parent.getJSONObject(i).getJSONObject("faceRectangle");
                        rectList.add(rect);
                    }

                    Bitmap imageBitmap = ((BitmapDrawable)imageSelected.getDrawable()).getBitmap();
                    imageSelected.setImageBitmap(drawRectangles(imageBitmap ,rectList));
                    imageBitmap.recycle();
                } catch (Exception e) {
                    Log.i("TAG", "errror with JSON");
                }
            } else {
                p.show();
            }
        }

        @Override
        protected String doInBackground(File... files) {
            try {
                RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("file", files[0].getName(), RequestBody.create(files[0], MediaType.get("image/jpeg")))
                        .addFormDataPart("returnFaceId", "true")
                        .addFormDataPart("returnFaceLandmarks", "false")
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
    }

    public static Bitmap drawRectangles(Bitmap original, LinkedList<JSONObject> rectList) {
        Bitmap bitmap = original.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
        paint.setStrokeWidth(10);

        for(JSONObject rect : rectList) {
            try {
                canvas.drawRect(
                        rect.getInt("left"),
                        rect.getInt("top"),
                        rect.getInt("left") + rect.getInt("width"),
                        rect.getInt("top") + rect.getInt("height"),
                        paint
                );
            } catch (Exception e) { }
        }

        return bitmap;
    }

}