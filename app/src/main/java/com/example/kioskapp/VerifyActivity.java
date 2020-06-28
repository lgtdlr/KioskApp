package com.example.kioskapp;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class VerifyActivity extends AppCompatActivity {

    private static final String DETECT_URL = "http://192.168.102.158:5000/face/v1.0/detect";
    private static final String VERIFY_URL = "http://192.168.102.158:5000/face/v1.0/verify";
    private static int FACE_ONE_SELECTED = 0, FACE_TWO_SELECTED = 1;
    private static final int REQUEST_CAMERA_ONE=1, SELECT_FILE_ONE=0, REQUEST_CAMERA_TWO=3, SELECT_FILE_TWO=2;
    ArrayList<String> faceIds = new ArrayList<>();
    Map<String, Object> Ids = new HashMap();

    private static TextView postVerifyText, verifyResults;
    private static ImageView face1Selected, face2Selected;
    private String faceId1, faceId2;

    private static OkHttpClient client = new OkHttpClient();

    ProgressDialog p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);
        postVerifyText = (TextView) findViewById(R.id.postVerifyText);
        verifyResults = (TextView) findViewById(R.id.verifyResults);
        face1Selected = (ImageView) findViewById(R.id.face1Selected);
        face2Selected = (ImageView) findViewById(R.id.face2Selected);
    }

    public void onFace1UploadClick(View view) {
        selectImage(1);
    }

    public void onFace2UploadClick(View view) {
        selectImage(2);
    }

    private void selectImage(final int faceSelected) {
        final CharSequence[] items={"Camera","Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(VerifyActivity.this);
        builder.setTitle("Add Image");

        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (items[i].equals("Camera")) {

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if(faceSelected == 1){
                        startActivityForResult(intent, REQUEST_CAMERA_ONE);
                    } else {
                        startActivityForResult(intent, REQUEST_CAMERA_TWO);
                    }


                } else if (items[i].equals("Gallery")) {

                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    if (faceSelected == 1){
                        startActivityForResult(intent, SELECT_FILE_ONE);
                    } else {
                        startActivityForResult(intent, SELECT_FILE_TWO);
                    }

                } else if (items[i].equals("Cancel")) {
                    dialogInterface.dismiss();
                }
            }
        });
        builder.show();
    }

    public void onVerifyClickButton(View view) {
        new VerifyRequest().execute(faceIds.get(0), faceIds.get(1));
    }

    private class VerifyRequest extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(VerifyActivity.this);
            p.setMessage("Please wait... Uploading");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("faceId1", strings[0])
                        .addFormDataPart("faceId2", strings[1])
                        .build();

                Log.i("faceId1", strings[0]);
                Log.i("faceId2", strings[1]);


                Request request = new Request.Builder()
                        .url(VERIFY_URL)
                        .post(requestBody)
                        .addHeader("Accept", "application/json; charset=utf-8")
                        .build();

                Log.i("requestBody", request.toString());

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
                p.hide();
                face1Selected.isOpaque();
                Log.i("TAG", s);
                try {
                    //JSONArray parent = new JSONArray(s);
                    postVerifyText.setText(s);
                    //Boolean isIdentical = parent.getJSONObject(0).getBoolean("isIdentical");
                    //Double confidence = parent.getJSONObject(0).getDouble("confidence");
//                    if (isIdentical){
//                        postVerifyText.setText("Success");
//                    } else if {
//                    postVerifyText.setText("Failure")
//                }
                    //;

                    //setUiAfterUpdate(parent);
                    setPostResponseText(s);
                } catch (Exception e) {
                    Log.i("TAG", "error with JSON");
                    Log.e("Error", e.getMessage());
                    Log.e("Error", e.getLocalizedMessage());
                }
        }
    }

    private void setPostResponseText(String s) {
        verifyResults.setText(s);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_FILE_ONE && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            final String fullPhotoPath = ImageSelect.getPath(this, fullPhotoUri);
            face1Selected.setImageURI(fullPhotoUri);

            File file = new File(fullPhotoPath);

            new PostImageRequest().execute(file);

        } else if (requestCode == SELECT_FILE_TWO && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            final String fullPhotoPath = ImageSelect.getPath(this, fullPhotoUri);
            face2Selected.setImageURI(fullPhotoUri);

            File file = new File(fullPhotoPath);

            new PostImageRequest().execute(file);

        }

        if (requestCode == REQUEST_CAMERA_ONE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            face1Selected.setImageBitmap(imageBitmap);

            new PostCameraRequest().execute(face1Selected);

        } else if (requestCode == REQUEST_CAMERA_TWO && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            face2Selected.setImageBitmap(imageBitmap);

            new PostCameraRequest().execute(face2Selected);
        }
    }

    private class PostCameraRequest extends AsyncTask<ImageView, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(VerifyActivity.this);
            p.setMessage("Please wait... uploading");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (face1Selected != null) {
                p.hide();
                //imageSelected.isOpaque();
                Log.i("TAG", s);
                try {
                    JSONArray parent = new JSONArray(s);
                    LinkedList<JSONObject> rectList = new LinkedList<>();

                    for (int i = 0; i < parent.length(); i++) {
                        JSONObject rect = parent.getJSONObject(i).getJSONObject("faceRectangle");
                        rectList.add(rect);
                    }

                    Bitmap imageBitmap = ((BitmapDrawable) face1Selected.getDrawable()).getBitmap();
                    //imageSelected.setImageBitmap(drawRectangles(imageBitmap, rectList));
                    imageBitmap.recycle();
                    setUiAfterUpdate(parent);
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
                        .addFormDataPart("returnFaceLandmarks", "true")
                        .addFormDataPart("returnRecognitionModel", "true")
                        .build();

                Request request = new Request.Builder()
                        .url(DETECT_URL)
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
            p = new ProgressDialog(VerifyActivity.this);
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
                        .addFormDataPart("returnRecognitionModel", "false")
                        .build();
                Request request = new Request.Builder()
                        .url(DETECT_URL)
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
            if (face1Selected != null) {
                p.hide();
                face1Selected.isOpaque();
                Log.i("TAG", s);
                try {
                    JSONArray parent = new JSONArray(s);
//                    LinkedList<JSONObject> rectList = new LinkedList<>();





                    //Bitmap face1ImageBitmap = ((BitmapDrawable) face1Selected.getDrawable()).getBitmap();
                    //imageSelected.setImageBitmap(drawRectangles(imageBitmap, rectList));
                    //face1ImageBitmap.recycle();
                    setUiAfterUpdate(parent);
                } catch (Exception e) {
                    Log.i("TAG", "error with JSON");
                }
            } else {
                p.show();
            }
        }
    }

    private void setUiAfterUpdate(JSONArray parent) throws JSONException {
       // ArrayList<String> items = new ArrayList<String>();
        faceIds.add(parent.getJSONObject(0).getString("faceId"));
        for (int i = 0 ; i < faceIds.size() ; i++){
            Log.d("Faces", faceIds.get(i).toString());
        }


//        for(int i=0; i < parent.length() ; i++) {
//            JSONObject faceAttributes = parent.getJSONObject(i).getJSONObject("faceAttributes");
//            int age = faceAttributes.getInt("age");
//            items.add("Age: " + age);
//        }
//        ListView listView = (ListView)findViewById(R.id.results_list);
//        ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);
//        listView.setAdapter(mArrayAdapter);
//        mArrayAdapter.notifyDataSetChanged();
    }

}