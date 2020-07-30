package com.example.kioskapp.customview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.example.kioskapp.menu.ThermalActivity;
import com.google.android.gms.vision.face.Landmark;
import com.google.mlkit.vision.face.Face;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class ThermalView extends androidx.appcompat.widget.AppCompatImageView {

    private static double temp;

    public ThermalView(Context context) {
        super(context);
    }

    public ThermalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ThermalView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public static void updateThermal(double temperature) {
         temp = temperature;
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint uPaint = new Paint();
        uPaint.setColor(Color.WHITE);
        uPaint.setStyle(Paint.Style.STROKE);
        uPaint.setStrokeWidth(1);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        List<Face> faces = ThermalActivity.getFaceList();
        Random rnd = new Random();

        if (faces != null) {
            for (Face face : faces) {

                Rect bounds = face.getBoundingBox();
                int x1 = bounds.left;
                int y1 = bounds.top;
                int x2 = bounds.right;
                int y2 = bounds.bottom;

                int id;
                if (face.getTrackingId() != null) {
                    id = face.getTrackingId();
                    switch(id) {
                        case 1:
                            paint.setColor(Color.BLUE);
                            break;
                        case 2:
                            paint.setColor(Color.GREEN);
                            break;
                        case 3:
                            paint.setColor(Color.YELLOW);
                            break;
                        case 4:
                            paint.setColor(Color.CYAN);
                            break;
                        case 5:
                            paint.setColor(Color.MAGENTA);
                            break;
                        default:
                            // code block
                            paint.setColor(Color.WHITE);
                    }
                }

                canvas.drawRoundRect(x1, y1, x2, y2, 6, 6, paint);
                paint.setTextSize(200);
                canvas.drawText(String.format("%.2f °", temp),x1, y1, paint);
                paint.setColor(Color.WHITE);
                canvas.drawPoint((x2 + x1) / 2, (y2 + y1) / 2, uPaint);
                canvas.drawPoint(canvas.getWidth()/2, canvas.getHeight()/2, uPaint);

            }
        }
    }
}