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
import android.widget.Button;
import android.widget.EditText;
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

    EditText editText;

    ProgressDialog p;

    private static final String BASE_URL = "http://192.168.102.158:5000/face/v1.0/persongroups/5000/persons";
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private static OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identify);
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);

        Button button = findViewById(R.id.button2);
        editText = findViewById(R.id.editText);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*Intent intent = new Intent(IdentifyActivity.this, TrainActivity.class);
                intent.putExtra("mytext", editText.getText().toString());
                startActivity(intent);*/
                new NewPersonRequest().execute(editText.getText().toString());
            }
        });
    }

    private class NewPersonRequest extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(IdentifyActivity.this);
            p.setMessage("Please wait...");
            p.setInverseBackgroundForced(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            p.hide();
            try {
                JSONObject parent = new JSONObject(s);
                String personId = parent.getString("personId");
                Intent intent = new Intent(IdentifyActivity.this, TrainActivity.class);
                intent.putExtra("myName", editText.getText().toString());
                intent.putExtra("personId", personId);
                startActivity(intent);
            } catch (Exception e) { }

        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                JSONObject json = new JSONObject();
                json.put("name", strings[0]);
                RequestBody requestBody = RequestBody.create(json.toString(), JSON);

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