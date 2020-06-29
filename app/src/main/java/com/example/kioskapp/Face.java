package com.example.kioskapp;

import android.graphics.Bitmap;

public class Face {

    private Bitmap faceBitmap;
    private String age;
    private String gender;
    private String emotion;
    private double emotionScore;

    public Face(Bitmap faceBitmap, String age, String gender, String emotion, double emotionScore) {
        this.faceBitmap = faceBitmap;
        this.age = age;
        this.gender = gender;
        this.emotion = emotion;
        this.emotionScore = emotionScore;
    }

    public Bitmap getFaceBitmap() {
        return faceBitmap;
    }

    public String getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public String getEmotion() {
        return emotion;
    }

    public double getEmotionScore() {
        return emotionScore;
    }
}
