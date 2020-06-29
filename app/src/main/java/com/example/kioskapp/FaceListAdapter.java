package com.example.kioskapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class FaceListAdapter extends ArrayAdapter<Face> {

    private Context context;
    private int resource;

    public FaceListAdapter(Context context, int resource, ArrayList<Face> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Bitmap bitmap = getItem(position).getFaceBitmap();
        String age = getItem(position).getAge();
        String gender = getItem(position).getGender();
        String emotion = getItem(position).getEmotion();
        double emotionScore = getItem(position).getEmotionScore();
        String face_description = String.format("%s\nGender: %s",
                age,
                gender
        );

        Face face = new Face(bitmap, age, gender, emotion, emotionScore);

        LayoutInflater inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(resource, parent, false);

        ImageView faceImage = (ImageView) convertView.findViewById(R.id.face_id);
        TextView tvAge = (TextView) convertView.findViewById(R.id.age_id);
        TextView tvEmotion = (TextView) convertView.findViewById(R.id.emotion_id);
        ProgressBar emotionBar = (ProgressBar) convertView.findViewById(R.id.emotion_bar_id);

        tvAge.setText(face_description);
        tvEmotion.setText("Emotion: " + emotion);
        emotionBar.setProgress((int) (emotionScore*100));
        faceImage.setImageBitmap(bitmap);

        return convertView;
    }
}
