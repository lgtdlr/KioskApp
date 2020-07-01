package com.example.kioskapp;

import android.graphics.Bitmap;

public class Face {

    private Bitmap faceBitmap;
    private String age;
    private String gender;
    private String emotion;
    private double emotionScore;
    private String emotion2;
    private double emotion2Score;
    private String emotion3;
    private double emotion3Score;

    public Face(Bitmap faceBitmap, String age, String gender, String emotion, double emotionScore, String emotion2, double emotion2Score, String emotion3, double emotion3Score) {
        this.faceBitmap = faceBitmap;
        this.age = age;
        this.gender = gender;
        this.emotion = emotion;
        this.emotionScore = emotionScore;
        this.emotion2 = emotion2;
        this.emotion2Score = emotion2Score;
        this.emotion3 = emotion3;
        this.emotion3Score = emotion3Score;
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

    public String getEmotion2() {
        return emotion2;
    }

    public double getEmotion2Score() {
        return emotion2Score;
    }

    public String getEmotion3() {
        return emotion3;
    }

    public double getEmotion3Score() {
        return emotion3Score;
    }
}
