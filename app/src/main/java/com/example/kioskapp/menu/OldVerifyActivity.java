package com.example.kioskapp.menu;

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
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.kioskapp.R;
import com.example.kioskapp.utils.SelectImageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class OldVerifyActivity extends AppCompatActivity {

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final String DETECT_URL = "http://192.168.102.158:5000/face/v1.0/detect";
    private static final String VERIFY_URL = "http://192.168.102.158:5000/face/v1.0/verify";
    private static final int REQUEST_CAMERA_ONE = 1, SELECT_FILE_ONE = 0, REQUEST_CAMERA_TWO = 3, SELECT_FILE_TWO = 2;
    private static OkHttpClient client = new OkHttpClient();
    ArrayList<String> faceIds = new ArrayList<>();
    ProgressDialog p;
    private TextView verifyResults;
    private ImageView face1Selected, face2Selected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_old_verify);
        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        verifyResults = findViewById(R.id.verifyResults);
        face1Selected = findViewById(R.id.face1Selected);
        face2Selected = findViewById(R.id.face2Selected);
    }

    public void onFace1UploadClick(View view) {
        selectImage(1);
    }

    public void onFace2UploadClick(View view) {
        selectImage(2);
    }

    private void selectImage(final int faceSelected) {
        final CharSequence[] items = {"Camera", "Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(OldVerifyActivity.this);
        builder.setTitle("Add Image");

        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (items[i].equals("Camera")) {
                    if (ContextCompat.checkSelfPermission(OldVerifyActivity.this, Manifest.permission.CAMERA) == -1) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
                        return;
                    }

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (faceSelected == 1) {
                        startActivityForResult(intent, REQUEST_CAMERA_ONE);
                    } else {
                        startActivityForResult(intent, REQUEST_CAMERA_TWO);
                    }


                } else if (items[i].equals("Gallery")) {
                    if (ContextCompat.checkSelfPermission(OldVerifyActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == -1) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        return;
                    }
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    //Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    if (faceSelected == 1) {
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

    private void setPostResponseText(String s) {
        verifyResults.setText(s);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_FILE_ONE && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            final String fullPhotoPath = SelectImageUtils.getPath(this, fullPhotoUri);
            face1Selected.setImageURI(fullPhotoUri);

            File file = new File(fullPhotoPath);

            new PostImageRequest().execute(file);

        } else if (requestCode == SELECT_FILE_TWO && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            final String fullPhotoPath = SelectImageUtils.getPath(this, fullPhotoUri);
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

    private void setUiAfterUpdate(JSONArray parent) throws JSONException {
        // ArrayList<String> items = new ArrayList<String>();
        faceIds.add(parent.getJSONObject(0).getString("faceId"));
        for (int i = 0; i < faceIds.size(); i++) {
            Log.d("Faces", faceIds.get(i));
        }
    }

    private class VerifyRequest extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(OldVerifyActivity.this);
            p.setMessage("Please wait... Uploading");
            p.setIndeterminate(false);
            p.setCancelable(false);
            p.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {

                JSONObject json = new JSONObject();
                json.put("faceId1", strings[0]);
                json.put("faceId2", strings[1]);

                RequestBody requestBody = RequestBody.create(json.toString(), JSON);

                Log.i("faceId1", strings[0]);
                Log.i("faceId2", strings[1]);


                Request request = new Request.Builder()
                        .url(VERIFY_URL)
                        .post(requestBody)
                        .addHeader("Accept", "application/json; charset=utf-8")
                        .build();

                Log.i("requestBody", requestBody.toString());

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
            Log.i("ASHWIN", s);
            try {
                JSONObject parent = new JSONObject(s);
                setPostResponseText(s);
            } catch (Exception e) {
                Log.i("TAG", "error with JSON");
                Log.e("Error", e.getMessage());
                Log.e("Error", e.getLocalizedMessage());
            }
        }
    }

    private class PostCameraRequest extends AsyncTask<ImageView, String, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            p = new ProgressDialog(OldVerifyActivity.this);
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
            p = new ProgressDialog(OldVerifyActivity.this);
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
                    setUiAfterUpdate(parent);
                } catch (Exception e) {
                    Log.i("TAG", "error with JSON");
                }
            } else {
                p.show();
            }
        }
    }

}