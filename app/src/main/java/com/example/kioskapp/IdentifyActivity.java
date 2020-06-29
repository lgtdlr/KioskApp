package com.example.kioskapp;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class IdentifyActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://192.168.102.158:5000/face/v1.0/detect";
    private static final int PICK_IMAGE = 1;

    ImageView selectedImage;
    TextView requestResult, infoText;
    ProgressDialog p;

    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify);
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);

        selectedImage = (ImageView) findViewById(R.id.selectedImage);
        infoText = (TextView) findViewById(R.id.infoText);
        requestResult = findViewById(R.id.requestResult);
    }

    public void onUpload(View view) {
        selectImage();
    }

    public void selectImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == -1){
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            return;
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, PICK_IMAGE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            final String fullPhotoPath = ImageSelect.getPath(this, fullPhotoUri);
            selectedImage.setImageURI(fullPhotoUri);

            File file = new File(fullPhotoPath);
            String fileName = file.getName();

            PostParameters params = new PostParameters(BASE_URL, file);
            AsyncTaskExample asyncTask = new AsyncTaskExample();
            if (asyncTask.execute(params).equals("Success")) {
                infoText.setText("Let's go");
            }
        }
    }

    private class PostParameters {
        String string;
        File file;

        PostParameters(String string, File file) {
            this.file = file;
            this.string = string;
        }
    }

    private class AsyncTaskExample extends AsyncTask<PostParameters, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(IdentifyActivity.this);
            p.setMessage("Please wait... Downloading");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected String doInBackground(PostParameters... params) {
            try {
                RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("file", params[0].file.getName(), RequestBody.create(params[0].file, MediaType.get("image/jpeg")))
                        .addFormDataPart("returnFaceId", "true")
                        .addFormDataPart("returnFaceLandmarks", "false")
                        .addFormDataPart("returnRecognitonModel", "false")
                        .build();
                Request request = new Request.Builder()
                        .url(params[0].string)
                        .post(requestBody)
                        .addHeader("Accept", "application/json; charset=utf-8")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    return response.body().string();
                }

            } catch (Exception ex) {
                // Handle the error
                ex.printStackTrace();
                return "Failure";
            }
        }

        @Override
        protected void onPostExecute(String string) {
            super.onPostExecute(string);
            if (selectedImage != null) {
                p.hide();
                selectedImage.isOpaque();
                infoText.setText(string);

                Log.i("TAG", string);

                //Parse JSONObject here

                try {
                    JSONObject mainObject = new JSONObject(string);
                    JSONArray array = mainObject.getJSONArray("faces");
                } catch (Exception e) {
                }

            } else {
                p.show();
            }
        }
    }
}