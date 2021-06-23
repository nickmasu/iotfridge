package manrique.nicolas.iotfridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class ArrowView extends View {

    private int SIZE = 512;
    private float TEMPERATURE_REFERENCE = 25;


    private Bitmap bitmapBackground;
    private Bitmap bitmapArrow;
    private Bitmap bitmapMinimum;
    private Bitmap bitmapMaximum;

    private float currentTemperature;
    private float minimumTemperature;
    private float maximumTemperature;

    private boolean minimumTemperatureEnable;
    private boolean maximumTemperatureEnable;

    private Matrix currentTemperaturePosition = new Matrix();
    private Matrix minimumTemperaturePosition;
    private Matrix maximumTemperaturePosition;

    public ArrowView(Context context) {
        super(context);
        initBitmaps();
    }

    public ArrowView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initBitmaps();
    }

    private void initBitmaps() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        bitmapBackground = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, false);

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
        bitmapArrow = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, false);

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.minimum);
        bitmapMinimum = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, false);

        bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.maximum);
        bitmapMaximum = Bitmap.createScaledBitmap(bitmap, SIZE, SIZE, false);
    }

    @Override
    public void onDraw(Canvas canvas) {
        //Paint Background
        Paint background = new Paint();
        background.setColor(Color.RED);
        canvas.drawRect(0, 0, getWidth(), getHeight(), background);

        canvas.drawBitmap(bitmapBackground, new Matrix(), null);
        canvas.drawBitmap(bitmapArrow, currentTemperaturePosition, null);

        if (maximumTemperatureEnable)
            canvas.drawBitmap(bitmapMaximum, maximumTemperaturePosition, null);
        if (minimumTemperatureEnable)
            canvas.drawBitmap(bitmapMaximum, minimumTemperaturePosition, null);
    }


    private Matrix calculatePosition(float temperature) {
        float degrees = 0;
        if (temperature > TEMPERATURE_REFERENCE)
            degrees = (90 * (temperature - TEMPERATURE_REFERENCE)) / TEMPERATURE_REFERENCE;
        else if (temperature < TEMPERATURE_REFERENCE)
            degrees = (90 * (temperature) / TEMPERATURE_REFERENCE) - 90;

        Matrix position = new Matrix();
        position.postRotate(degrees, SIZE / 2, SIZE / 2);
        return position;
    }


    public void setCurrentTemperature(float temperature) {
        currentTemperature = temperature;
        currentTemperaturePosition = calculatePosition(temperature);
        invalidate();
    }

    public void setMaximumTemperature(float temperature) {
        maximumTemperature = temperature;
        maximumTemperatureEnable = true;
        maximumTemperaturePosition = calculatePosition(temperature);
        invalidate();
    }

    public void removeMaximumTemperature() {
        maximumTemperatureEnable = false;
        invalidate();
    }

    public void setMinimumTemperature(float temperature) {
        minimumTemperature = temperature;
        minimumTemperatureEnable = true;
        minimumTemperaturePosition = calculatePosition(temperature);
        invalidate();
    }

    public void removeMinimumTemperature() {
        minimumTemperatureEnable = false;
        invalidate();
    }
}