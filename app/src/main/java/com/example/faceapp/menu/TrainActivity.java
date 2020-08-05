package com.example.faceapp.menu;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.faceapp.R;

import org.json.JSONObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TrainActivity extends AppCompatActivity {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String BASE_URL = "http://192.168.102.158:5000/face/v1.0/persongroups/5000/persons";
    private static OkHttpClient client = new OkHttpClient();
    EditText editText;
    ProgressDialog p;
    boolean trainButtonClicked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);

        Button button = findViewById(R.id.button2);

        editText = findViewById(R.id.editText);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new NewPersonRequest().execute(editText.getText().toString());
            }
        });


    }

    private class NewPersonRequest extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(TrainActivity.this);
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
                Intent intent;
                if (trainButtonClicked) {
                    intent = new Intent(TrainActivity.this, LiveTrainActivity.class);
                } else
                    intent = new Intent(TrainActivity.this, OldTrainActivity.class);

                trainButtonClicked = false;
                intent.putExtra("myName", editText.getText().toString());
                intent.putExtra("personId", personId);
                startActivity(intent);
            } catch (Exception e) {
            }

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