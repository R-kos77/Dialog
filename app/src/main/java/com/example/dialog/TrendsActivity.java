package com.example.dialog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;

import com.example.dialog.utils.LocaleHelper;

import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrendsActivity extends BaseActivity {
    private SimpleLineChartView chart;
    private TextView averageText, highestText, lowestText;
    private TextView userInfoText;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd", Locale.getDefault());
    private static final String TAG = "TrendsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trends);
        setupToolbar();
        initializeViews();
        loadUserInfo();
        loadData();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.trends));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeViews() {
        chart = findViewById(R.id.chart);
        userInfoText = findViewById(R.id.userInfoText);
        averageText = findViewById(R.id.averageText);
        highestText = findViewById(R.id.highestText);
        lowestText = findViewById(R.id.lowestText);
    }

    private void loadUserInfo() {
        try {
            SharedPreferences prefs = getSharedPreferences("CurrentUser", MODE_PRIVATE);
            String userJson = prefs.getString("userData", "");
            if (!userJson.isEmpty()) {
                JSONObject user = new JSONObject(userJson);
                String gender = user.getString("gender");
                
                // Translate gender if in Amharic
                if (LocaleHelper.getLanguage(this).equals("am")) {
                    gender = gender.equals("Male") ? "ወንድ" : "ሴት";
                }

                String userInfo = String.format(Locale.getDefault(), 
                    "%s %s    %s",
                    user.getString("firstName"),
                    user.getString("lastName"),
                    gender);
                userInfoText.setText(userInfo);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user info: " + e.getMessage());
        }
    }

    private void loadData() {
        new Thread(() -> {
            List<LogEntry> logs = AppDatabase.getInstance(this)
                .logEntryDao()
                .getAllForUser(((DialogApp) getApplication()).getCurrentUserId());

            if (logs != null && !logs.isEmpty()) {
                final double[] stats = calculateStats(logs);
                final double average = stats[0];
                final double highest = stats[1];
                final double lowest = stats[2];
                final long highestTime = (long)stats[3];
                final long lowestTime = (long)stats[4];

                runOnUiThread(() -> {
                    try {
                        if (!isFinishing() && chart != null) {
                            chart.setData(logs);
                            
                            averageText.setText(String.format(Locale.getDefault(), 
                                "Average: %.0f mg/dL", average));
                            
                            highestText.setText(String.format(Locale.getDefault(),
                                "Highest: %.0f mg/dL (%s)", 
                                highest,
                                dateFormat.format(new Date(highestTime))));
                            
                            lowestText.setText(String.format(Locale.getDefault(),
                                "Lowest: %.0f mg/dL (%s)", 
                                lowest,
                                dateFormat.format(new Date(lowestTime))));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error updating UI: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    private double[] calculateStats(List<LogEntry> logs) {
        double sum = 0;
        double highest = Double.MIN_VALUE;
        double lowest = Double.MAX_VALUE;
        long highestTime = 0;
        long lowestTime = 0;

        for (LogEntry log : logs) {
            sum += log.bloodSugar;
            if (log.bloodSugar > highest) {
                highest = log.bloodSugar;
                highestTime = log.timestamp;
            }
            if (log.bloodSugar < lowest) {
                lowest = log.bloodSugar;
                lowestTime = log.timestamp;
            }
        }

        double average = sum / logs.size();
        return new double[]{average, highest, lowest, highestTime, lowestTime};
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trends_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_export_pdf) {
            exportToPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportToPdf() {
        try {
            File pdfFile = new File(getExternalFilesDir(null), "blood_sugar_trends.pdf");
            PdfDocument document = new PdfDocument();
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size
            PdfDocument.Page page = document.startPage(pageInfo);
            Canvas canvas = page.getCanvas();

            Paint paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setTextSize(12);

            // Add title
            paint.setTextSize(16);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            canvas.drawText("Blood Sugar Trends", 50, 50, paint);

            // Add user info
            paint.setTextSize(12);
            paint.setTypeface(Typeface.DEFAULT);
            canvas.drawText(userInfoText.getText().toString(), 50, 80, paint);

            // Add statistics
            paint.setTextSize(12);
            canvas.drawText(averageText.getText().toString(), 50, 120, paint);
            canvas.drawText(highestText.getText().toString(), 50, 140, paint);
            canvas.drawText(lowestText.getText().toString(), 50, 160, paint);

            // Capture and draw the chart
            Bitmap chartBitmap = getChartBitmap();
            if (chartBitmap != null) {
                try {
                    // Calculate scaling to fit PDF width while maintaining aspect ratio
                    float pdfWidth = pageInfo.getPageWidth() - 100; // 50px padding on each side
                    float scale = pdfWidth / chartBitmap.getWidth();
                    float scaledHeight = chartBitmap.getHeight() * scale;

                    // Create matrix for scaling
                    Matrix matrix = new Matrix();
                    matrix.setRectToRect(
                        new RectF(0, 0, chartBitmap.getWidth(), chartBitmap.getHeight()),
                        new RectF(50, 200, 50 + pdfWidth, 200 + scaledHeight),
                        Matrix.ScaleToFit.START
                    );

                    // Draw scaled bitmap
                    canvas.drawBitmap(chartBitmap, matrix, null);
                } finally {
                    chartBitmap.recycle();
                }
            }

            document.finishPage(page);
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            document.close();
            fos.close();

            // Share the PDF
            Uri pdfUri = FileProvider.getUriForFile(this, 
                "com.rkos.dialog.fileprovider", pdfFile);
            
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, pdfUri);
            startActivity(Intent.createChooser(intent, "Export PDF"));
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error exporting PDF: " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getChartBitmap() {
        try {
            // Make sure chart has a size
            chart.measure(
                View.MeasureSpec.makeMeasureSpec(chart.getWidth(), View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(chart.getHeight(), View.MeasureSpec.EXACTLY)
            );
            chart.layout(0, 0, chart.getMeasuredWidth(), chart.getMeasuredHeight());

            // Create bitmap of the chart's size
            Bitmap bitmap = Bitmap.createBitmap(
                chart.getMeasuredWidth(),
                chart.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
            );

            // Draw chart onto canvas
            Canvas canvas = new Canvas(bitmap);
            chart.draw(canvas);

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Reload data when configuration changes
        loadData();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save any necessary state
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore any necessary state
        loadData();
    }
} 