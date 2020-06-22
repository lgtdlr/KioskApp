package com.example.kioskapp;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    public void onTeachClick(View view) {
        //start new activity
        Intent teachIntent = new Intent(this, DetectActivity.class);
        startActivity(teachIntent);
    }

    public void onIdentifyClick(View view){
        Intent identifyIntent = new Intent(this, IdentifyActivity.class);
        startActivity(identifyIntent);
    }
}