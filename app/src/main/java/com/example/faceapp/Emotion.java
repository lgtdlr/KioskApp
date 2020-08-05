package com.example.faceapp;

public class Emotion implements Comparable {

    private String type;
    private double value;

    public Emotion(String type, double value) {
        this.type = type;
        this.value = value;
    }


    public String getType() {
        return type;
    }

    public double getValue() {
        return value;
    }


    @Override
    public int compareTo(Object o) {
        o = (Emotion) o;
        if (value > ((Emotion) o).value)
            return -1;
        if (value < ((Emotion) o).value)
            return 1;
        else
            return 0;
    }
}
