package com.example.kioskapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
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

        Face face = new Face(bitmap, age);

        LayoutInflater inflater = LayoutInflater.from(context);
        convertView = inflater.inflate(resource, parent, false);

        ImageView faceImage = (ImageView) convertView.findViewById(R.id.face_id);
        TextView tvAge = (TextView) convertView.findViewById(R.id.age_id);

        tvAge.setText(age);
        faceImage.setImageBitmap(bitmap);

        return convertView;
    }
}
