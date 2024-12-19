package com.example.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.appcompat.widget.Toolbar;

import com.opencsv.CSVWriter;

import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.widget.ImageView;
import android.content.res.Resources;
import android.content.res.Configuration;

import com.example.dialog.utils.LocaleHelper;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;

public class LogViewActivity extends BaseActivity {
    private TableLayout logTable;
    private TextView userInfoText;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    private List<LogEntry> logs = new ArrayList<>();
    private static final String TAG = "LogViewActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.view_logs));
        }
        setupToolbar();
        initializeViews();
        loadUserInfo();
        loadLogs();
        setupListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.logs_header));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeViews() {
        logTable = findViewById(R.id.logTable);
        userInfoText = findViewById(R.id.userInfoText);
    }

    private void setupListeners() {
        Button viewTrendsButton = findViewById(R.id.viewTrendsButton);
        viewTrendsButton.setOnClickListener(v -> startActivity(new Intent(this, TrendsActivity.class)));
    }

    private void loadUserInfo() {
        try {
            SharedPreferences prefs = getSharedPreferences("CurrentUser", MODE_PRIVATE);
            String userJson = prefs.getString("userData", "");
            if (!userJson.isEmpty()) {
                JSONObject user = new JSONObject(userJson);
                String info = getString(R.string.user_info_format,
                    user.getString("firstName"),
                    user.getString("lastName"),
                    user.getInt("age"),
                    user.getString("gender").equals("Male") ? 
                        getString(R.string.male) : getString(R.string.female)
                );
                userInfoText.setText(info);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user info: " + e.getMessage());
        }
    }

    private void loadLogs() {
        new Thread(() -> {
            try {
                String userId = ((DialogApp) getApplication()).getCurrentUserId();
                Log.d(TAG, "Loading logs for user: " + userId);
                
                SharedPreferences prefs = getSharedPreferences("CurrentUser", MODE_PRIVATE);
                String userJson = prefs.getString("userData", "");
                if (!userJson.isEmpty()) {
                    JSONObject user = new JSONObject(userJson);
                    String verifiedUserId = user.getString("firstName") + "_" + user.getString("lastName");
                    if (!verifiedUserId.equals(userId)) {
                        Log.e(TAG, "User ID mismatch! App: " + userId + ", Prefs: " + verifiedUserId);
                        userId = verifiedUserId;
                    }
                }
                
                logs = AppDatabase.getInstance(this)
                    .logEntryDao()
                    .getAllForUser(userId);
                
                Log.d(TAG, "Found " + (logs != null ? logs.size() : 0) + " logs");
                
                runOnUiThread(() -> {
                    if (logs != null && !logs.isEmpty()) {
                        updateLogDisplay(logs);
                    } else {
                        showEmptyState();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading logs: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error loading logs: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void updateLogDisplay(List<LogEntry> logs) {
        logTable.removeAllViews();
        addHeaderRow();
        
        for (LogEntry log : logs) {
            addLogRow(log);
        }
    }

    private void addHeaderRow() {
        TableRow header = new TableRow(this);
        header.setLayoutParams(new TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT
        ));
        
        GradientDrawable gradient = new GradientDrawable();
        gradient.setColor(getResources().getColor(R.color.colorPrimary));
        gradient.setCornerRadius(8);
        header.setBackground(gradient);
        
        String[] headers = {
            getString(R.string.date),
            getString(R.string.time),
            "mg/dL",
            getString(R.string.status),
            ""  // For edit button
        };
        
        for (String headerText : headers) {
            TextView cell = createHeaderCell(headerText);
            header.addView(cell);
        }
        
        logTable.addView(header);
    }

    private TextView createHeaderCell(String text) {
        TextView cell = new TextView(this);
        cell.setText(text);
        int padding = getResources().getDimensionPixelSize(R.dimen.cell_padding);
        cell.setPadding(padding, padding, padding, padding);
        cell.setGravity(Gravity.CENTER);
        cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        cell.setTypeface(null, Typeface.BOLD);
        cell.setTextColor(Color.WHITE);
        
        if (text.equals(getString(R.string.date))) {
            cell.setMinWidth(getResources().getDimensionPixelSize(R.dimen.date_cell_width));
        } else if (text.equals(getString(R.string.time))) {
            cell.setMinWidth(getResources().getDimensionPixelSize(R.dimen.time_cell_width));
        } else if (text.equals("mg/dL")) {
            cell.setMinWidth(getResources().getDimensionPixelSize(R.dimen.mgdl_cell_width));
        } else if (text.equals(getString(R.string.status))) {
            cell.setMinWidth(getResources().getDimensionPixelSize(R.dimen.status_cell_width));
        }
        
        return cell;
    }

    private void addLogRow(LogEntry log) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new TableRow.LayoutParams(
            TableRow.LayoutParams.MATCH_PARENT,
            TableRow.LayoutParams.WRAP_CONTENT
        ));
        
        Date logDate = new Date(log.timestamp);
        
        TextView dateCell = createTableCell(dateFormat.format(logDate));
        TextView timeCell = createTableCell(timeFormat.format(logDate));
        TextView mgdlCell = createTableCell(String.format("%.0f", log.bloodSugar));
        TextView statusCell = createStatusCell(getStatusForBloodSugar(log.bloodSugar));
        ImageButton editButton = createEditButton(log);
        
        row.addView(dateCell);
        row.addView(timeCell);
        row.addView(mgdlCell);
        row.addView(statusCell);
        row.addView(editButton);
        
        logTable.addView(row);
        
        View divider = new View(this);
        divider.setBackgroundColor(getResources().getColor(R.color.dividerColor));
        divider.setLayoutParams(new TableLayout.LayoutParams(
            TableLayout.LayoutParams.MATCH_PARENT,
            1
        ));
        logTable.addView(divider);
    }

    private TextView createTableCell(String text) {
        TextView cell = new TextView(this);
        cell.setText(text);
        int padding = getResources().getDimensionPixelSize(R.dimen.cell_padding);
        cell.setPadding(padding, padding, padding, padding);
        cell.setGravity(Gravity.CENTER);
        cell.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        return cell;
    }

    private TextView createStatusCell(String status) {
        TextView cell = createTableCell(status);
        cell.setTypeface(null, Typeface.BOLD);
        
        int textColor;
        if (status.equals(getString(R.string.low)) || 
            status.equals(getString(R.string.high))) {
            textColor = getResources().getColor(R.color.colorError);
        } else {
            textColor = getResources().getColor(R.color.colorSuccess);
        }
        
        cell.setTextColor(textColor);
        return cell;
    }

    private ImageButton createEditButton(LogEntry log) {
        ImageButton editButton = new ImageButton(this);
        editButton.setImageResource(R.drawable.ic_edit);
        editButton.setBackgroundResource(android.R.color.transparent);
        editButton.setOnClickListener(v -> showEditDialog(log));
        
        int size = getResources().getDimensionPixelSize(R.dimen.edit_button_size);
        TableRow.LayoutParams params = new TableRow.LayoutParams(size, size);
        params.gravity = Gravity.CENTER;
        editButton.setLayoutParams(params);
        
        editButton.setColorFilter(getResources().getColor(R.color.colorPrimary));
        
        return editButton;
    }

    private String getStatusForBloodSugar(double value) {
        if (value < 70) {
            return getString(R.string.low);
        } else if (value > 180) {
            return getString(R.string.high);
        } else {
            return getString(R.string.normal);
        }
    }

    private void showEditDialog(LogEntry logEntry) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_reading, null);
        EditText readingInput = dialogView.findViewById(R.id.readingInput);
        RadioGroup unitGroup = dialogView.findViewById(R.id.unitGroup);

        unitGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String input = readingInput.getText().toString();
            if (!input.isEmpty()) {
                try {
                    double value = Double.parseDouble(input);
                    if (checkedId == R.id.mmolUnit) {
                        readingInput.setText(String.format(Locale.US, "%.1f", value / 18.0182));
                    } else {
                        readingInput.setText(String.format(Locale.US, "%.0f", value * 18.0182));
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error converting value: " + e.getMessage());
                }
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(getString(R.string.edit_reading))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.save), (dialogInterface, i) -> {
                String input = readingInput.getText().toString();
                if (!input.isEmpty()) {
                    try {
                        double value = Double.parseDouble(input);
                        if (unitGroup.getCheckedRadioButtonId() == R.id.mmolUnit) {
                            value = value * 18.0182;
                        }
                        updateReading(logEntry, value);
                    } catch (NumberFormatException e) {
                        Log.e(TAG, "Error parsing value: " + e.getMessage());
                    }
                }
            })
            .setNegativeButton(getString(R.string.cancel), null)
            .setNeutralButton(getString(R.string.delete), (dialog1, which) -> {
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_reading))
                    .setMessage(getString(R.string.delete_reading_confirm))
                    .setPositiveButton(getString(R.string.yes), (dialogConfirm, whichConfirm) -> {
                        deleteReading(logEntry);
                    })
                    .setNegativeButton(getString(R.string.no), null)
                    .show();
            })
            .create();

        readingInput.setText(String.format(Locale.US, "%.0f", logEntry.bloodSugar));
        dialog.show();
    }

    private void showDeleteConfirmationDialog(LogEntry log, AlertDialog editDialog) {
        try {
            if (!isFinishing()) {
                new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.delete_reading_title))
                    .setMessage(getString(R.string.delete_reading_message))
                    .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                        deleteReading(log);
                        editDialog.dismiss();
                    })
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing delete dialog: " + e.getMessage());
            Toast.makeText(this, getString(R.string.error_deleting), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteReading(LogEntry log) {
        new Thread(() -> {
            try {
                AppDatabase.getInstance(this).logEntryDao().delete(log);
                runOnUiThread(() -> {
                    logs.remove(log);
                    if (logs.isEmpty()) {
                        showEmptyState();
                    } else {
                        updateLogDisplay(logs);
                    }
                    Toast.makeText(this, getString(R.string.reading_deleted), Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error deleting reading: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error_deleting), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void updateReading(LogEntry log, double newValue) {
        new Thread(() -> {
            try {
                log.bloodSugar = newValue;
                AppDatabase.getInstance(this).logEntryDao().update(log);
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.reading_updated), Toast.LENGTH_SHORT).show();
                    loadLogs();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error updating reading: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error_updating), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void showEmptyState() {
        logTable.removeAllViews();
        addHeaderRow();
        
        TableRow emptyRow = new TableRow(this);
        TextView emptyText = new TextView(this);
        emptyText.setText(getString(R.string.no_readings));
        emptyText.setPadding(16, 32, 16, 32);
        emptyText.setGravity(Gravity.CENTER);
        
        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.span = 6;
        emptyText.setLayoutParams(params);
        
        emptyRow.addView(emptyText);
        logTable.addView(emptyRow);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.log_view_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_language) {
            showLanguageDialog();
            return true;
        } else if (id == R.id.action_export_csv) {
            exportToCsv();
            return true;
        } else if (id == R.id.action_share) {
            shareData();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportToCsv() {
        try {
            File directory = new File(getExternalFilesDir(null), "exports");
            if (!directory.exists() && !directory.mkdirs()) {
                Log.e(TAG, "Failed to create directory");
                return;
            }
            
            File csvFile = new File(directory, "blood_sugar_logs.csv");
            CSVWriter writer = new CSVWriter(new FileWriter(csvFile));

            // Write headers
            String[] headers = {"Date", "Time", "Blood Sugar (mg/dL)", "Status", "Notes"};
            writer.writeNext(headers);

            // Write data
            for (LogEntry log : logs) {
                String[] row = {
                    dateFormat.format(new Date(log.timestamp)),
                    timeFormat.format(new Date(log.timestamp)),
                    String.format(Locale.getDefault(), "%.0f", log.bloodSugar),
                    getStatusForBloodSugar(log.bloodSugar),
                    log.notes != null ? log.notes : ""
                };
                writer.writeNext(row);
            }

            writer.close();

            // Share the CSV
            Uri uri = FileProvider.getUriForFile(this, 
                "com.rkos.dialog.fileprovider", csvFile);
            
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Blood Sugar Logs CSV"));

        } catch (Exception e) {
            Log.e(TAG, "Error exporting to CSV: " + e.getMessage());
            Toast.makeText(this, "Error exporting CSV", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareData() {
        try {
            StringBuilder data = new StringBuilder();
            data.append("Blood Sugar Logs\n\n");
            data.append(userInfoText.getText()).append("\n\n");

            for (LogEntry log : logs) {
                data.append(dateFormat.format(new Date(log.timestamp)))
                    .append(" ")
                    .append(timeFormat.format(new Date(log.timestamp)))
                    .append(": ")
                    .append(String.format(Locale.getDefault(), "%.0f", log.bloodSugar))
                    .append(" mg/dL (")
                    .append(getStatusForBloodSugar(log.bloodSugar))
                    .append(")\n");
            }

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Blood Sugar Logs");
            shareIntent.putExtra(Intent.EXTRA_TEXT, data.toString());
            startActivity(Intent.createChooser(shareIntent, "Share Blood Sugar Logs"));

        } catch (Exception e) {
            Log.e(TAG, "Error sharing data: " + e.getMessage());
            Toast.makeText(this, "Error sharing data", Toast.LENGTH_SHORT).show();
        }
    }

    private float getOffset(float[] widths, int index) {
        float offset = 0;
        for (int i = 0; i < index; i++) {
            offset += widths[i];
        }
        return offset;
    }

    private void formatLogEntry(LogEntry log) {
        String formattedValue = String.format(Locale.getDefault(), "%.0f mg/dL", log.bloodSugar);
        // ... rest of method
    }

    private void writeFile(File file, byte[] data) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(data);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
} 