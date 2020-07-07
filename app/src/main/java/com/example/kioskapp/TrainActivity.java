package com.example.kioskapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.TextView;

public class TrainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_train);

        TextView textView = findViewById(R.id.textView6);
        Bundle extras = getIntent().getExtras();
        if(extras != null) {
            textView.setText(extras.getString("personId"));
        }
    }
}