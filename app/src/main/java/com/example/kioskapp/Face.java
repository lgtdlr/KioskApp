package com.example.kioskapp;

import android.graphics.Bitmap;

public class Face {

    private Bitmap faceBitmap;
    private String age;

    public Face(Bitmap faceBitmap, String age) {
        this.faceBitmap = faceBitmap;
        this.age = age;
    }

    public Bitmap getFaceBitmap() {
        return faceBitmap;
    }

    public String getAge() {
        return age;
    }

}
