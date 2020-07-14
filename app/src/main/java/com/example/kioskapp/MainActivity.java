package com.example.kioskapp;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toast.makeText(MainActivity.this, "OpenCV Load Status: " + OpenCVLoader.initDebug(), Toast.LENGTH_LONG).show();

        Animation animFloat;
        animFloat = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.floating_animation);

        ImageView imageView = (ImageView) findViewById(R.id.main_imageview_placeholder);
        //Drawable myDrawable = getResources().getDrawable(R.drawable.cognitive);
        //imageView.setImageDrawable(myDrawable);
        imageView.startAnimation(animFloat);

        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    public void onDetectClick(View view) {
        //start new activity
        Intent intent = new Intent(this, DetectActivity.class);
        startActivity(intent);
    }

    public void onIdentifyClick(View view) {
        Intent identifyIntent = new Intent(this, IdentifyActivity.class);
        startActivity(identifyIntent);
    }

    public void onCameraClick(View view) {
        //Get camera permissions before opening camera activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == -1) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            return;
        }
        Intent cameraIntent = new Intent(this, LiveDetectActivity.class);
        startActivity(cameraIntent);
    }

    public void onObjectClick(View view) {
        Intent identifyIntent = new Intent(this, ObjectDetectActivity.class);
        startActivity(identifyIntent);
    }

    public void onTrainClick(View view) {
        Intent trainIntent = new Intent(this, TrainActivity.class);
        startActivity(trainIntent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    public void onPlaceholderClick(View view) {
        Toast.makeText(MainActivity.this, "New services coming soon", Toast.LENGTH_LONG).show();
    }

    public void onMenuClick(View view) {
        //Open navigation menu
        DrawerLayout mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.openDrawer(GravityCompat.START);
    }
}