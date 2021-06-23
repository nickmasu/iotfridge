package manrique.nicolas.iotfridge;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class TemperatureView extends View {

    private int SIZE = 720;
    private float TEMPERATURE_REFERENCE = 20;


    private Bitmap bitmapBackground;
    private Bitmap bitmapArrow;
    private Bitmap bitmapMinimum;
    private Bitmap bitmapMaximum;

    private float currentTemperature;

    private boolean minimumTemperatureEnable;
    private boolean maximumTemperatureEnable;

    private Matrix currentTemperaturePosition = new Matrix();
    private Matrix minimumTemperaturePosition;
    private Matrix maximumTemperaturePosition;

    public TemperatureView(Context context) {
        super(context);
        initBitmaps();
    }

    public TemperatureView(Context context, @Nullable AttributeSet attrs) {
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
        // draw text
        drawLowLimit(canvas);
        drawHighLimit(canvas);
        drawTemperature(canvas);

        // draw widget
        canvas.drawBitmap(bitmapArrow, currentTemperaturePosition, null);

        if (maximumTemperatureEnable)
            canvas.drawBitmap(bitmapMaximum, maximumTemperaturePosition, null);
        if (minimumTemperatureEnable)
            canvas.drawBitmap(bitmapMinimum, minimumTemperaturePosition, null);

        canvas.drawBitmap(bitmapBackground, new Matrix(), null);


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
        maximumTemperatureEnable = true;
        maximumTemperaturePosition = calculatePosition(temperature);
        invalidate();
    }

    public void removeMaximumTemperature() {
        maximumTemperatureEnable = false;
        invalidate();
    }

    public void setMinimumTemperature(float temperature) {
        minimumTemperatureEnable = true;
        minimumTemperaturePosition = calculatePosition(temperature);
        invalidate();
    }

    public void removeMinimumTemperature() {
        minimumTemperatureEnable = false;
        invalidate();
    }


    private void drawLowLimit(Canvas canvas) {
        int margin = 70;
        String text = "0 Cº";
        int positionY = (SIZE / 2) + margin;
        int positionX = 0;

        Paint paint = new Paint();
        paint.setColor(Color.BLUE);
        paint.setTextSize(42);
        canvas.drawText(text, positionX, positionY, paint);
    }

    private void drawHighLimit(Canvas canvas) {
        int margin = 70;
        String text = String.format("%d Cº", (int) TEMPERATURE_REFERENCE * 2);
        int positionY = (SIZE / 2) + margin;
        int positionX = SIZE - margin - 50;

        Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setTextSize(42);
        canvas.drawText(text, positionX, positionY, paint);
    }

    private void drawTemperature(Canvas canvas) {
        String text = String.format("%.1f Cº", currentTemperature);
        int positionY = (SIZE / 2) + 120;
        int positionX = (SIZE / 2) - 120;

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(68);
        paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));

        canvas.drawText(text, positionX, positionY, paint);
    }
}