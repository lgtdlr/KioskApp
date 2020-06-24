package com.example.kioskapp;

import android.Manifest;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Animation animFloat;
        animFloat = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.floating_animation);

        ImageView imageView = (ImageView) findViewById(R.id.imageview_id);
        Drawable myDrawable = getResources().getDrawable(R.drawable.azure);
        imageView.setImageDrawable(myDrawable);
        imageView.startAnimation(animFloat);

        requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        requestPermissions(new String[] {Manifest.permission.CAMERA}, 2);
    }

    public void onDetectClick(View view) {
        //start new activity
        Intent teachIntent = new Intent(this, DetectActivity.class);
        startActivity(teachIntent);
    }

    public void onIdentifyClick(View view){
        Intent identifyIntent = new Intent(this, IdentifyActivity.class);
        startActivity(identifyIntent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}