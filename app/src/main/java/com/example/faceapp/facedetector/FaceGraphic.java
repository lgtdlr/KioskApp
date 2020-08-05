/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.faceapp.facedetector;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;

import com.example.faceapp.Emotion;
import com.example.faceapp.camera.GraphicOverlay;
import com.example.faceapp.camera.GraphicOverlay.Graphic;
import com.example.faceapp.menu.LiveDetectActivity;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;
import com.google.mlkit.vision.face.FaceLandmark.LandmarkType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Graphic instance for rendering face position, contour, and landmarks within the associated
 * graphic overlay view.
 */
public class FaceGraphic extends Graphic {
    private static int age;
    private static String gender;
    private static JSONObject json_data;
    private static JSONArray jsonFaces;

    public static Canvas getCanvas() {
        return canvas;
    }

    private static Canvas canvas;

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    private float x;
    private float y;
    private final String TAG = "FaceGraphic";
    private LiveDetectActivity liveDetectActivity;
    private static final float FACE_POSITION_RADIUS = 4.0f;
    private static final float ID_TEXT_SIZE = 30.0f;
    private static final float ID_Y_OFFSET = 40.0f;
    private static final float ID_X_OFFSET = -40.0f;
    private static final float BOX_STROKE_WIDTH = 5.0f;
    private static final int NUM_COLORS = 10;
    private static final int[][] COLORS = new int[][]{
            // {Text color, background color}
            {Color.BLACK, Color.WHITE},
            {Color.WHITE, Color.MAGENTA},
            {Color.BLACK, Color.LTGRAY},
            {Color.WHITE, Color.RED},
            {Color.WHITE, Color.BLUE},
            {Color.WHITE, Color.DKGRAY},
            {Color.BLACK, Color.CYAN},
            {Color.BLACK, Color.YELLOW},
            {Color.WHITE, Color.BLACK},
            {Color.BLACK, Color.GREEN}
    };

    private final Paint facePositionPaint;
    private final Paint[] idPaints;
    private final Paint[] boxPaints;
    private final Paint[] labelPaints;

    private volatile Face face;

    public FaceGraphic(GraphicOverlay overlay, Face face) {
        super(overlay);

        this.face = face;
        final int selectedColor = Color.WHITE;

        facePositionPaint = new Paint();
        facePositionPaint.setColor(selectedColor);

        int numColors = COLORS.length;
        idPaints = new Paint[numColors];
        boxPaints = new Paint[numColors];
        labelPaints = new Paint[numColors];
        for (int i = 0; i < numColors; i++) {
            idPaints[i] = new Paint();
            idPaints[i].setColor(COLORS[i][0] /* text color */);
            idPaints[i].setTextSize(ID_TEXT_SIZE);

            boxPaints[i] = new Paint();
            boxPaints[i].setColor(COLORS[i][1] /* background color */);
            boxPaints[i].setStyle(Paint.Style.STROKE);
            boxPaints[i].setStrokeWidth(BOX_STROKE_WIDTH);

            labelPaints[i] = new Paint();
            labelPaints[i].setColor(COLORS[i][1]  /* background color */);
            labelPaints[i].setStyle(Paint.Style.FILL);
        }
    }

    /**
     * Draws the face annotations for position on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        FaceGraphic.canvas = canvas;
        Face face = this.face;
        if (face == null) {
            return;
        }

        // Draws a circle at the position of the detected face, with the face's track id below.
        x = translateX(face.getBoundingBox().centerX());
        y = translateY(face.getBoundingBox().centerY());
        canvas.drawCircle(x, y, FACE_POSITION_RADIUS, facePositionPaint);

        // Calculate positions.
        float left = x - scale(face.getBoundingBox().width() / 2.0f);
        float top = y - scale(face.getBoundingBox().height() / 2.0f);
        float right = x + scale(face.getBoundingBox().width() / 2.0f);
        float bottom = y + scale(face.getBoundingBox().height() / 2.0f);
        float lineHeight = ID_TEXT_SIZE + BOX_STROKE_WIDTH;
        float yLabelOffset = -lineHeight;

        // Decide color based on face ID
        int colorID = (face.getTrackingId() == null)
                ? 0 : Math.abs(face.getTrackingId() % NUM_COLORS);

        // Calculate width and height of label box
        float textWidth = idPaints[colorID].measureText("ID: " + face.getTrackingId());
        if (face.getSmilingProbability() != null) {
            yLabelOffset -= lineHeight;
            textWidth = Math.max(textWidth, idPaints[colorID].measureText(
                    String.format(Locale.US, "Happiness: %.2f", face.getSmilingProbability())));
        }
        if (face.getLeftEyeOpenProbability() != null) {
            yLabelOffset -= lineHeight;
            textWidth = Math.max(textWidth, idPaints[colorID].measureText(
                    String.format(Locale.US, "Left eye: %.2f", face.getLeftEyeOpenProbability())));
        }
        if (face.getRightEyeOpenProbability() != null) {
            yLabelOffset -= lineHeight;
            textWidth = Math.max(textWidth, idPaints[colorID].measureText(
                    String.format(Locale.US, "Right eye: %.2f", face.getLeftEyeOpenProbability())));
        }

        // Draw labels
        canvas.drawRect(left - BOX_STROKE_WIDTH,
                top + yLabelOffset,
                left + textWidth + (2 * BOX_STROKE_WIDTH),
                top,
                labelPaints[colorID]);
        yLabelOffset += ID_TEXT_SIZE;
        canvas.drawRect(left, top, right, bottom, boxPaints[colorID]);
        canvas.drawText("ID: " + face.getTrackingId(), left, top + yLabelOffset,
                idPaints[colorID]);
        yLabelOffset += lineHeight;

        // Draws all face contours.
//        for (FaceContour contour : face.getAllContours()) {
//            for (PointF point : contour.getPoints()) {
//                canvas.drawCircle(
//                        translateX(point.x), translateY(point.y), FACE_POSITION_RADIUS, facePositionPaint);
//            }
//        }

        //Draw eye contours.
        if (face.getContour(FaceContour.LEFT_EYE) != null) {
            List<PointF> leftEyeContour =
                    face.getContour(FaceContour.LEFT_EYE).getPoints();
            for (PointF point : leftEyeContour) {
                canvas.drawCircle(
                        translateX(point.x), translateY(point.y), FACE_POSITION_RADIUS, facePositionPaint);
            }
        }
        if (face.getContour(FaceContour.RIGHT_EYE) != null) {
            List<PointF> rightEyeContour =
                    face.getContour(FaceContour.RIGHT_EYE).getPoints();
            for (PointF point : rightEyeContour) {
                canvas.drawCircle(
                        translateX(point.x), translateY(point.y), FACE_POSITION_RADIUS, facePositionPaint);
            }
        }

        // Draws smiling and left/right eye open probabilities.
        if (face.getSmilingProbability() != null) {
            canvas.drawText(
                    "Smiling: " + String.format(Locale.US, "%.2f", face.getSmilingProbability()),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        }

        FaceLandmark leftEye = face.getLandmark(FaceLandmark.LEFT_EYE);
        if (leftEye != null && face.getLeftEyeOpenProbability() != null) {
            canvas.drawText(
                    "Left eye open: " + String.format(Locale.US, "%.2f", face.getLeftEyeOpenProbability()),
                    translateX(leftEye.getPosition().x) + ID_X_OFFSET,
                    translateY(leftEye.getPosition().y) + ID_Y_OFFSET,
                    idPaints[colorID]);
        } else if (leftEye != null && face.getLeftEyeOpenProbability() == null) {
            canvas.drawText(
                    "Left eye",
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        } else if (leftEye == null && face.getLeftEyeOpenProbability() != null) {
            canvas.drawText(
                    "Left eye open: " + String.format(Locale.US, "%.2f", face.getLeftEyeOpenProbability()),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        }

        FaceLandmark rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE);
        if (rightEye != null && face.getRightEyeOpenProbability() != null) {
            canvas.drawText(
                    "Right eye open: " + String.format(Locale.US, "%.2f", face.getRightEyeOpenProbability()),
                    translateX(rightEye.getPosition().x) + ID_X_OFFSET,
                    translateY(rightEye.getPosition().y) + ID_Y_OFFSET,
                    idPaints[colorID]);
        } else if (rightEye != null && face.getRightEyeOpenProbability() == null) {
            canvas.drawText(
                    "Right eye",
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
            yLabelOffset += lineHeight;
        } else if (rightEye == null && face.getRightEyeOpenProbability() != null) {
            canvas.drawText(
                    "Right eye open: " + String.format(Locale.US, "%.2f", face.getRightEyeOpenProbability()),
                    left,
                    top + yLabelOffset,
                    idPaints[colorID]);
        }

        // Draw facial landmarks
        drawFaceLandmark(canvas, FaceLandmark.LEFT_EYE);
        drawFaceLandmark(canvas, FaceLandmark.RIGHT_EYE);
        drawFaceLandmark(canvas, FaceLandmark.LEFT_CHEEK);
        drawFaceLandmark(canvas, FaceLandmark.RIGHT_CHEEK);

        // Draw overlay for face results from LiveDetectActivity
        // If uncommented be sure to uncomment relevant code in LiveDetectActivity
//        if (jsonFaces != null) {
//            float textBoundary = 70;
//            int scale = 1;
//            Paint paint = new Paint();
//            paint.setColor(Color.WHITE);
//            paint.setTextSize(textBoundary);
//            LinkedList<JSONObject> rectList = new LinkedList<>();
//            for (int i = 0; i < jsonFaces.length(); i++) {
//                try {
//                    json_data = jsonFaces.getJSONObject(i);
//                    JSONObject faceAttributes = json_data.getJSONObject("faceAttributes");
//                    JSONObject emotions = faceAttributes.getJSONObject("emotion");
//                    JSONObject rectangle = json_data.getJSONObject("faceRectangle");
//                    rectList.add(rectangle);
//                    age = faceAttributes.getInt("age");
//                    gender = faceAttributes.getString("gender");
//                    float rectLeft = x - scale(rectangle.getInt("width") / 2.0f);
//                    float rectTop = y - scale(rectangle.getInt("top") / 2.0f);
//
//                    ArrayList<Emotion>  emotionsList = getEmotions(emotions);
//                    canvas.drawText("Age: " + age, rectLeft*scale, rectTop*scale+(int)(textBoundary*1), paint);
//                    canvas.drawText("Gender: " + gender, rectLeft*scale, rectTop*scale+(int)(textBoundary*2), paint);
//                    canvas.drawText("Emotions: ", rectLeft*scale, rectTop*scale+(int)(textBoundary*3), paint);
//
//                    canvas.drawText(emotionsList.get(0).getType(), rectLeft*scale, rectTop*scale+(int)(textBoundary*4), paint);
//                    canvas.drawRoundRect(rectLeft+350, rectTop*scale+(int)(textBoundary*4)-40, rectLeft+350+((float)emotionsList.get(0).getValue()*400), rectTop+(int)(textBoundary*4), 20, 20, paint);
//                    canvas.drawText(emotionsList.get(1).getType(), rectLeft*scale, rectTop*scale+(int)(textBoundary*5), paint);
//                    canvas.drawRoundRect(rectLeft+350, rectTop*scale+(int)(textBoundary*5)-40, rectLeft+350+((float)emotionsList.get(1).getValue()*400), rectTop+(int)(textBoundary*5), 20, 20, paint);
//                    canvas.drawText(emotionsList.get(2).getType(), rectLeft*scale, rectTop*scale+(int)(textBoundary*6), paint);
//                    canvas.drawRoundRect(rectLeft+350, rectTop*scale+(int)(textBoundary*6)-40, rectLeft+350+((float)emotionsList.get(2).getValue()*400), rectTop+(int)(textBoundary*6), 20, 20, paint)   ;
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//
//            }
//
//        }
    }

    private ArrayList<Emotion> getEmotions(JSONObject attributes) throws JSONException {
        ArrayList<Emotion> emotionsList = new ArrayList<>();

        emotionsList.add(new Emotion("anger", attributes.getDouble("anger")));
        emotionsList.add(new Emotion("contempt", attributes.getDouble("contempt")));
        emotionsList.add(new Emotion("disgust", attributes.getDouble("anger")));
        emotionsList.add(new Emotion("fear", attributes.getDouble("fear")));
        emotionsList.add(new Emotion("happiness", attributes.getDouble("happiness")));
        emotionsList.add(new Emotion("neutral", attributes.getDouble("neutral")));
        emotionsList.add(new Emotion("sadness", attributes.getDouble("sadness")));
        emotionsList.add(new Emotion("surprise", attributes.getDouble("surprise")));

        Collections.sort(emotionsList);

        return emotionsList;
    }


    public static void displayDetectData(int detectAge, String detectGender, JSONArray jsonArray) {
        age = detectAge;
        gender = detectGender;
        jsonFaces = jsonArray;
        
    }

    private void drawFaceLandmark(Canvas canvas, @LandmarkType int landmarkType) {
        FaceLandmark faceLandmark = face.getLandmark(landmarkType);
        if (faceLandmark != null) {
            canvas.drawCircle(
                    translateX(faceLandmark.getPosition().x),
                    translateY(faceLandmark.getPosition().y),
                    FACE_POSITION_RADIUS,
                    facePositionPaint);
        }
    }
}
