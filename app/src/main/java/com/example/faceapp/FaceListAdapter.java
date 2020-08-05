package com.example.faceapp;

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
        String emotion2 = getItem(position).getEmotion2();
        double emotion2Score = getItem(position).getEmotion2Score();
        String emotion3 = getItem(position).getEmotion3();
        double emotion3Score = getItem(position).getEmotion3Score();

        String face_description = String.format("%s\nGender: %s",
                age,
                gender
        );

        Face face = new Face(bitmap, age, gender, emotion, emotionScore, emotion2, emotion2Score, emotion3, emotion3Score);

        LayoutInflater inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(resource, parent, false);

        ImageView faceImage = (ImageView) convertView.findViewById(R.id.face_id);
        TextView tvAge = (TextView) convertView.findViewById(R.id.age_id);
        TextView tvEmotion = (TextView) convertView.findViewById(R.id.emotion_id);
        ProgressBar emotionBar = (ProgressBar) convertView.findViewById(R.id.emotion_bar_id);
        TextView tv2Emotion = (TextView) convertView.findViewById(R.id.emotion_id_2);
        ProgressBar emotion2Bar = (ProgressBar) convertView.findViewById(R.id.emotion_bar_id_2);
        TextView tv3Emotion = (TextView) convertView.findViewById(R.id.emotion_id_3);
        ProgressBar emotion3Bar = (ProgressBar) convertView.findViewById(R.id.emotion_bar_id_3);

        emotion2Bar.setVisibility(View.VISIBLE);
        emotion3Bar.setVisibility(View.VISIBLE);
        tv2Emotion.setVisibility(View.VISIBLE);
        tv3Emotion.setVisibility(View.VISIBLE);


        tvAge.setText(face_description);
        tvEmotion.setText("Emotion: " + emotion);
        emotionBar.setProgress((int) (emotionScore * 1000));
        tv2Emotion.setText("Emotion: " + emotion2);
        emotion2Bar.setProgress((int) (emotion2Score * 1000));
        if (emotion2Bar.getProgress() == 0) {
            emotion2Bar.setVisibility(View.GONE);
            tv2Emotion.setVisibility(View.GONE);
        }
        tv3Emotion.setText("Emotion: " + emotion3);
        emotion3Bar.setProgress((int) (emotion3Score * 1000));
        if (emotion3Bar.getProgress() == 0) {
            emotion3Bar.setVisibility(View.GONE);
            tv3Emotion.setVisibility(View.GONE);
        }
        faceImage.setImageBitmap(bitmap);

        return convertView;
    }
}
