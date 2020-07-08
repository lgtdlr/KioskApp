package com.example.kioskapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RealIdentifyActivity extends AppCompatActivity implements View.OnClickListener {

    private static OkHttpClient client = new OkHttpClient();

    private static final String ID_URL = "http://192.168.102.158:5000/face/v1.0/detect?recognitionModel=Recognition_02";
    private static final String IDENTIFY_URL = "http://192.168.102.158:5000/face/v1.0/identify";

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");


    ProgressDialog p;

    private static ImageView imageSelected;

    private static final int PICK_IMAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_real_identify);
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        imageSelected = (ImageView) findViewById(R.id.imageView3);
    }

    @Override
    public void onClick(View view) {
        final CharSequence[] items={"Camera","Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(RealIdentifyActivity.this);
        builder.setTitle("Add Image");

        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (items[i].equals("Camera")) {
                    if (ContextCompat.checkSelfPermission(RealIdentifyActivity.this, Manifest.permission.CAMERA) == -1){
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
                        return;
                    }

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);


                } else if (items[i].equals("Gallery")) {
                    if (ContextCompat.checkSelfPermission(RealIdentifyActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == -1){
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            final String fullPhotoPath = ImageSelect.getPath(this, fullPhotoUri);
            imageSelected.setImageURI(fullPhotoUri);

            File file = new File(fullPhotoPath);

            Bitmap bitmap = ((BitmapDrawable) imageSelected.getDrawable()).getBitmap();


            new PostImageRequest().execute(bitmap);
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            imageSelected.setImageBitmap(imageBitmap);

            Bitmap bitmap = ((BitmapDrawable) imageSelected.getDrawable()).getBitmap();

            new PostImageRequest().execute(bitmap);
        }
    }

    private class PostImageRequest extends AsyncTask<Bitmap, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(RealIdentifyActivity.this);
            p.setMessage("Getting face ids...");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            if(imageSelected != null) {
                p.hide();
                Log.i("ASHWIN", s.toString());

                imageSelected.isOpaque();
                try {
                    JSONArray parent = new JSONArray(s);
                    JSONObject person = parent.getJSONObject(0);
                    String faceId = person.getString("faceId");

                    new PersonRequest().execute(faceId);
                } catch (Exception e) {


                }

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
            p = new ProgressDialog(RealIdentifyActivity.this);
            p.setMessage("Acquiring person ids...");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.i("ASHWIN", s.toString());

            p.hide();
            try {
                JSONArray parent = new JSONArray(s);
                JSONArray candidates = parent.getJSONObject(0).getJSONArray("candidates");
                if(candidates.length() == 0) {
                    Toast.makeText(RealIdentifyActivity.this, "Unknown person detected", Toast.LENGTH_SHORT).show();
                } else {
                    String personId = candidates.getJSONObject(0).getString("personId");

                }


            } catch (Exception e) { }
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
            } catch (Exception e) { }


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
}