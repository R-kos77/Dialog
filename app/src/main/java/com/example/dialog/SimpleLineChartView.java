package com.example.dialog;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SimpleLineChartView extends View {
    private List<LogEntry> dataPoints = new ArrayList<>();
    private Paint linePaint;
    private Paint fillPaint;
    private Paint pointPaint;
    private Paint gridPaint;
    private Paint textPaint;
    private Paint thresholdPaint;
    private float padding = 50;
    private float maxY = 300;
    private float minY = 0;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());

    public SimpleLineChartView(Context context) {
        super(context);
        init();
    }

    public SimpleLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint = new Paint();
        linePaint.setColor(getResources().getColor(R.color.colorPrimary));
        linePaint.setStrokeWidth(4f);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        pointPaint = new Paint();
        pointPaint.setColor(getResources().getColor(R.color.colorPrimary));
        pointPaint.setStyle(Paint.Style.FILL);
        pointPaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.LTGRAY);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAlpha(100);

        textPaint = new Paint();
        textPaint.setColor(Color.DKGRAY);
        textPaint.setTextSize(12);
        textPaint.setAntiAlias(true);

        thresholdPaint = new Paint();
        thresholdPaint.setStyle(Paint.Style.STROKE);
        thresholdPaint.setStrokeWidth(2f);
        thresholdPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 5}, 0));
        thresholdPaint.setAntiAlias(true);
    }

    public void setData(List<LogEntry> data) {
        this.dataPoints = data;
        calculateYBounds();
        invalidate();
    }

    private void calculateYBounds() {
        if (dataPoints.isEmpty()) return;
        
        maxY = 0;
        minY = Float.MAX_VALUE;
        for (LogEntry entry : dataPoints) {
            maxY = Math.max(maxY, (float) entry.bloodSugar);
            minY = Math.min(minY, (float) entry.bloodSugar);
        }
        // Add padding to bounds
        float range = maxY - minY;
        maxY += range * 0.1f;
        minY = Math.max(0, minY - range * 0.1f);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (dataPoints.isEmpty()) return;

        float width = getWidth() - 2 * padding;
        float height = getHeight() - 2 * padding;

        // Sort data points by timestamp (oldest to newest)
        List<LogEntry> sortedData = new ArrayList<>(dataPoints);
        Collections.sort(sortedData, (a, b) -> Long.compare(a.timestamp, b.timestamp));

        float xStep = width / (sortedData.size() - 1);

        // Draw grid lines
        float gridStep = height / 4;
        for (int i = 0; i <= 4; i++) {
            float y = padding + i * gridStep;
            canvas.drawLine(padding, y, getWidth() - padding, y, gridPaint);
            float value = maxY - (maxY - minY) * (i / 4f);
            canvas.drawText(String.format("%.0f", value), 10, y + 5, textPaint);
        }

        // Draw date labels
        for (int i = 0; i < sortedData.size(); i += Math.max(1, sortedData.size() / 5)) {
            float x = padding + i * xStep;
            String date = dateFormat.format(new Date(sortedData.get(i).timestamp));
            float textWidth = textPaint.measureText(date);
            canvas.drawText(date, x - textWidth/2, getHeight() - padding/2, textPaint);
        }

        // Draw threshold lines
        float highY = getYCoordinate(180);
        float lowY = getYCoordinate(70);
        Paint thresholdPaint = new Paint(gridPaint);
        thresholdPaint.setColor(getResources().getColor(R.color.colorError));
        thresholdPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10, 5}, 0));
        canvas.drawLine(padding, highY, getWidth() - padding, highY, thresholdPaint);
        canvas.drawLine(padding, lowY, getWidth() - padding, lowY, thresholdPaint);

        // Draw the line chart
        Path path = new Path();
        boolean first = true;

        for (int i = 0; i < sortedData.size(); i++) {
            float x = padding + i * xStep;
            float y = getYCoordinate((float) sortedData.get(i).bloodSugar);
            
            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
            
            // Draw points
            canvas.drawCircle(x, y, 8, pointPaint);
        }
        canvas.drawPath(path, linePaint);
    }

    private float getYCoordinate(float value) {
        float height = getHeight() - 2 * padding;
        return getHeight() - padding - (value - minY) / (maxY - minY) * height;
    }
} 