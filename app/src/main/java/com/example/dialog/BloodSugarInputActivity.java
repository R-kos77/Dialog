package com.example.dialog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import org.json.JSONObject;

import com.example.dialog.utils.KeyboardUtils;
import com.example.dialog.utils.LocaleHelper;

import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.Context;

public class BloodSugarInputActivity extends BaseActivity {
    private EditText bloodSugarInput;
    private RadioGroup unitGroup;
    private Button saveButton;
    private Button viewLogsButton;
    private Button viewRemindersButton;
    private TextView welcomeText;
    private static final String TAG = "BloodSugarInput";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blood_sugar_input);
        setupToolbar();
        initializeViews();
        setupListeners();
        setupWelcomeBanner();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.input_header));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeViews() {
        bloodSugarInput = findViewById(R.id.bloodSugarInput);
        unitGroup = findViewById(R.id.unitGroup);
        saveButton = findViewById(R.id.saveButton);
        viewLogsButton = findViewById(R.id.viewLogsButton);
        viewRemindersButton = findViewById(R.id.viewRemindersButton);
        welcomeText = findViewById(R.id.welcomeText);
    }

    private void setupListeners() {
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> saveReading());
        } else {
            Log.e(TAG, "saveButton is null");
        }
        
        if (viewLogsButton != null) {
            viewLogsButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, LogViewActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "viewLogsButton is null");
        }
        
        if (viewRemindersButton != null) {
            viewRemindersButton.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReminderActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e(TAG, "viewRemindersButton is null");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_language) {
            showLanguageDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveReading() {
        String bloodSugar = bloodSugarInput.getText().toString().trim();
        if (bloodSugar.isEmpty()) {
            bloodSugarInput.setError(getString(R.string.enter_blood_sugar));
            return;
        }

        try {
            final double value = Double.parseDouble(bloodSugar);
            boolean isMmol = unitGroup.getCheckedRadioButtonId() == R.id.mmolRadio;
            
            // Convert to mg/dL if needed
            final double finalValue = isMmol ? value * 18.0182 : value;

            String userId = getCurrentUserId();
            Log.d(TAG, "Saving reading for user: " + userId);

            // Create the LogEntry object
            final LogEntry entry = new LogEntry(
                finalValue,
                System.currentTimeMillis(),
                userId,
                "" // Empty notes for now
            );
            
            // Save in background thread
            new Thread(() -> {
                try {
                    AppDatabase db = AppDatabase.getInstance(this);
                    db.logEntryDao().insert(entry);
                    
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.reading_saved), Toast.LENGTH_SHORT).show();
                        bloodSugarInput.setText("");
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error saving: " + e.getMessage(), e);
                    runOnUiThread(() -> {
                        Toast.makeText(this, getString(R.string.error_saving) + ": " + e.getMessage(), 
                            Toast.LENGTH_LONG).show();
                    });
                }
            }).start();
            
            // Hide keyboard after saving
            KeyboardUtils.hideKeyboard(this);
            
        } catch (NumberFormatException e) {
            bloodSugarInput.setError(getString(R.string.invalid_number));
        }
    }

    private String getCurrentUserId() {
        try {
            SharedPreferences prefs = getSharedPreferences("CurrentUser", MODE_PRIVATE);
            String userJson = prefs.getString("userData", "");
            if (!userJson.isEmpty()) {
                JSONObject user = new JSONObject(userJson);
                String userId = user.getString("firstName") + "_" + user.getString("lastName");
                Log.d(TAG, "Current user ID: " + userId);
                return userId;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting user ID: " + e.getMessage());
        }
        return "";
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, UserListActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void setupWelcomeBanner() {
        try {
            SharedPreferences prefs = getSharedPreferences("CurrentUser", MODE_PRIVATE);
            String userJson = prefs.getString("userData", "");
            if (!userJson.isEmpty()) {
                JSONObject user = new JSONObject(userJson);
                String firstName = user.getString("firstName");
                String welcomeMessage = getString(R.string.welcome_user, firstName);
                welcomeText.setText(welcomeMessage);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up welcome banner: " + e.getMessage());
        }
    }
} 