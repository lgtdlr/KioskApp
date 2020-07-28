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
import java.util.Set;

public class ThermalView extends androidx.appcompat.widget.AppCompatImageView {

    public ThermalView(Context context) {
        super(context);
    }

    public ThermalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ThermalView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        List<Face> faces = ThermalActivity.getFaceList();

        if (faces != null) {
            for (Face face : faces) {

                Rect bounds = face.getBoundingBox();
                int x1 = bounds.left;
                int y1 = bounds.top;
                int x2 = bounds.right;
                int y2 = bounds.bottom;

                if (face.getTrackingId() == 1) {
                    paint.setColor(Color.RED);
                }
                if (face.getTrackingId() == 2) {
                    paint.setColor(Color.BLUE);
                }
                if (face.getTrackingId() == 0) {
                    paint.setColor(Color.WHITE);

                    canvas.drawRoundRect(x1, y1, x2, y2, 5, 5, paint);
                    canvas.drawPoint((x2 + x1) / 2, (y2 + y1) / 2, paint);


                }

//            for (int i : setString){
//                faces.get(i).getLandmark(Landmark.NOSE_BASE).getPosition();
//            }

            }
        }
    }
}