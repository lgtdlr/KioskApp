package com.example.kioskapp.menu;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.kioskapp.R;
import com.google.android.material.navigation.NavigationView;

import org.opencv.android.OpenCVLoader;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    Animation animFloat, animDropDown;
    ImageView wave, fullWave, redOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toast.makeText(MainActivity.this, "OpenCV Load Status: " + OpenCVLoader.initDebug(), Toast.LENGTH_LONG).show();

        animDropDown = new ScaleAnimation(
                1f, 1f, // Start and end values for the X axis scaling
                1f, 15f, // Start and end values for the Y axis scaling
                Animation.RELATIVE_TO_SELF, 0f, // Pivot point of X scaling
                Animation.RELATIVE_TO_SELF, 0f); // Pivot point of Y scaling
        animDropDown.setDuration(1000);

        animFloat = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.floating_animation);

        wave = findViewById(R.id.main_imageview_placeholder);
        fullWave = findViewById(R.id.main_imageview_dropdown);
        redOverlay = findViewById(R.id.red_overlay);

        redOverlay.setVisibility(View.GONE);
        fullWave.setVisibility(View.GONE);
        wave.startAnimation(animFloat);

        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        redOverlay.setVisibility(View.GONE);
        fullWave.setVisibility(View.GONE);
    }

    public void onDetectClick(View view) {
        //start new activity
        fullWave.setVisibility(View.VISIBLE);
        fullWave.startAnimation(animDropDown);
        Intent intent = new Intent(this, DetectActivity.class);
        animDropDown.setAnimationListener(new Animation.AnimationListener(){
            @Override
            public void onAnimationStart(Animation animation){}

            @Override
            public void onAnimationRepeat(Animation animation){}

            @Override
            public void onAnimationEnd(Animation animation){
                redOverlay.setVisibility(View.VISIBLE);
                startActivity(intent);
            }
        });
    }

    public void onIdentifyClick(View view) {
        Intent intent = new Intent(this, IdentifyActivity.class);
        fullWave.setVisibility(View.VISIBLE);
        fullWave.startAnimation(animDropDown);
        animDropDown.setAnimationListener(new Animation.AnimationListener(){

            @Override
            public void onAnimationStart(Animation animation){}

            @Override
            public void onAnimationRepeat(Animation animation){}

            @Override
            public void onAnimationEnd(Animation animation){
                redOverlay.setVisibility(View.VISIBLE);
                startActivity(intent);
            }
        });
    }

    public void onCameraClick(View view) {
        //Get camera permissions before opening camera activity
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == -1) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            return;
        }
        Intent intent = new Intent(this, LiveDetectActivity.class);
        fullWave.setVisibility(View.VISIBLE);
        fullWave.startAnimation(animDropDown);
        animDropDown.setAnimationListener(new Animation.AnimationListener(){

            @Override
            public void onAnimationStart(Animation animation){}

            @Override
            public void onAnimationRepeat(Animation animation){}

            @Override
            public void onAnimationEnd(Animation animation){
                redOverlay.setVisibility(View.VISIBLE);
                startActivity(intent);
            }
        });
    }

    public void onObjectClick(View view) {
        Intent intent = new Intent(this, ObjectDetectActivity.class);
        fullWave.setVisibility(View.VISIBLE);
        fullWave.startAnimation(animDropDown);
        animDropDown.setAnimationListener(new Animation.AnimationListener(){

            @Override
            public void onAnimationStart(Animation animation){}

            @Override
            public void onAnimationRepeat(Animation animation){}

            @Override
            public void onAnimationEnd(Animation animation){
                redOverlay.setVisibility(View.VISIBLE);
                startActivity(intent);
            }
        });
    }

    public void onTrainClick(View view) {
        Intent intent = new Intent(this, TrainActivity.class);
        fullWave.setVisibility(View.VISIBLE);
        fullWave.startAnimation(animDropDown);
        animDropDown.setAnimationListener(new Animation.AnimationListener(){

            @Override
            public void onAnimationStart(Animation animation){}

            @Override
            public void onAnimationRepeat(Animation animation){}

            @Override
            public void onAnimationEnd(Animation animation){
                redOverlay.setVisibility(View.VISIBLE);
                startActivity(intent);
            }
        });
    }

    public void onDrivingClick(View view) {
        Intent intent = new Intent(this, DrivingActivity.class);
        fullWave.setVisibility(View.VISIBLE);
        fullWave.startAnimation(animDropDown);
        animDropDown.setAnimationListener(new Animation.AnimationListener(){

            @Override
            public void onAnimationStart(Animation animation){}

            @Override
            public void onAnimationRepeat(Animation animation){}

            @Override
            public void onAnimationEnd(Animation animation){
                redOverlay.setVisibility(View.VISIBLE);
                startActivity(intent);
            }
        });
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
        DrawerLayout mDrawerLayout = findViewById(R.id.drawer_layout);
        mDrawerLayout.openDrawer(GravityCompat.START);
    }
}