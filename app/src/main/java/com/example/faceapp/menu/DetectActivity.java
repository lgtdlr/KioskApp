package com.example.faceapp.menu;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.faceapp.Emotion;
import com.example.faceapp.Face;
import com.example.faceapp.FaceListAdapter;
import com.example.faceapp.R;
import com.example.faceapp.utils.SelectImageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;




public class DetectActivity extends AppCompatActivity {

    //MEC address
    private static final String BASE_URL = "http://192.168.102.158:5000/face/v1.0/detect?returnFaceAttributes=*";
    private static final int PICK_IMAGE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static OkHttpClient client = new OkHttpClient();
    ProgressDialog p;
    //    Animation animPushUp;
    private ImageView imageSelected;
//    private ImageView dropDownImage;

    //draw rectangles on image
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
        imageSelected = findViewById(R.id.imageSelected);
        //Tentative continue animation option
//        dropDownImage = (ImageView) findViewById(R.id.imageviewdropdown);
//        animPushUp = new ScaleAnimation(
//                1f, 1f, // Start and end values for the X axis scaling
//                15f, 0f, // Start and end values for the Y axis scaling
//                Animation.RELATIVE_TO_SELF, 0f, // Pivot point of X scaling
//                Animation.RELATIVE_TO_SELF, 0f); // Pivot point of Y scaling
//        animPushUp.setDuration(3000);
//        animPushUp.setAnimationListener(new Animation.AnimationListener(){
//
//            @Override
//            public void onAnimationStart(Animation animation){}
//
//            @Override
//            public void onAnimationRepeat(Animation animation){}
//
//            @Override
//            public void onAnimationEnd(Animation animation){
//                dropDownImage.setVisibility(View.GONE);
//            }
//        });
//        dropDownImage.startAnimation(animPushUp);

    }

    //selecting image from camera roll
    private void selectImage() {
        final CharSequence[] items = {"Camera", "Gallery", "Cancel"};

        AlertDialog.Builder builder = new AlertDialog.Builder(DetectActivity.this);
        builder.setTitle("Add Image");

        builder.setItems(items, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (items[i].equals("Camera")) {
                    if (ContextCompat.checkSelfPermission(DetectActivity.this, Manifest.permission.CAMERA) == -1) {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
                        return;
                    }

                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);


                } else if (items[i].equals("Gallery")) {
                    if (ContextCompat.checkSelfPermission(DetectActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == -1) {
                        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                        return;
                    }
                    Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
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

    //this method does commands based on if camera roll was selected or camera
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {
            Uri fullPhotoUri = data.getData();
            final String fullPhotoPath = SelectImageUtils.getPath(this, fullPhotoUri);
            imageSelected.setImageURI(fullPhotoUri);

            File file = new File(fullPhotoPath);


            new PostImageRequest().execute(file);
        }

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            Matrix matrix = new Matrix();
            matrix.postRotate(270);
            imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), matrix, true);
            imageSelected.setImageBitmap(imageBitmap);

            new PostCameraRequest().execute(imageSelected);
        }
    }


    //this code updates the UI after request is sent
    private void setUiAfterUpdate(JSONArray parent) throws JSONException {
        ArrayList<Face> faces = new ArrayList<>();
        LinkedList<JSONObject> rectList = new LinkedList<>();

        for (int i = 0; i < parent.length(); i++) {
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

            ArrayList<Emotion> emotionsList = getEmotions(emotions);

            faces.add(new Face(faceBitmap, "Age: " + age, gender, emotionsList.get(0).getType(),
                    emotionsList.get(0).getValue(), emotionsList.get(1).getType(), emotionsList.get(1).getValue(),
                    emotionsList.get(2).getType(), emotionsList.get(2).getValue()));

            Log.i("Adding faces", "Success");
        }
        ListView listView = findViewById(R.id.results_list);
        FaceListAdapter adapter = new FaceListAdapter(DetectActivity.this, R.layout.detect_adapter_view_layout, faces);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();


    }

    //this method parses through JSON object to retrieve emotions
    private ArrayList<Emotion> getEmotions(JSONObject attributes) throws JSONException {
        ArrayList<Emotion> emotionsList = new ArrayList<>();

        emotionsList.add(new Emotion("anger", attributes.getDouble("anger")));
        emotionsList.add(new Emotion("contempt", attributes.getDouble("contempt")));
        emotionsList.add(new Emotion("disgust", attributes.getDouble("anger")));
        emotionsList.add(new Emotion("fear", attributes.getDouble("fear")));
        emotionsList.add(new Emotion("happiness", attributes.getDouble("happiness")));
        emotionsList.add(new Emotion("neutral", attributes.getDouble("neutral")));
        emotionsList.add(new Emotion("sadness", attributes.getDouble("sadness")));
        emotionsList.add(new Emotion("surprise", attributes.getDouble("surprise")));

        Collections.sort(emotionsList);

        return emotionsList;
    }

    //this method executes an api request
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
                    setUiAfterUpdate(parent);
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

}