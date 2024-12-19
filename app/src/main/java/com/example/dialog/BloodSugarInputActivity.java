package com.example.dialog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.RadioButton;
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
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.text.Editable;
import android.text.TextWatcher;

public class BloodSugarInputActivity extends BaseActivity {
    private EditText bloodSugarInput;
    private Button saveButton;
    private Button viewLogsButton;
    private Button viewRemindersButton;
    private TextView welcomeText;
    private static final String TAG = "BloodSugarInput";
    private RadioGroup unitGroup;
    private double bloodSugarValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blood_sugar_input);
        
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.input_blood_sugar));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        unitGroup = findViewById(R.id.unitGroup);
        bloodSugarInput = findViewById(R.id.bloodSugarInput);
        saveButton = findViewById(R.id.saveButton);
        viewLogsButton = findViewById(R.id.viewLogsButton);
        viewRemindersButton = findViewById(R.id.viewRemindersButton);
        welcomeText = findViewById(R.id.welcomeText);

        initializeViews();
        setupListeners();
        setupWelcomeBanner();

        // Add TextWatcher to handle conversion in real-time
        bloodSugarInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                try {
                    if (!s.toString().isEmpty()) {
                        double value = Double.parseDouble(s.toString());
                        if (unitGroup.getCheckedRadioButtonId() == R.id.mmolUnit) {
                            bloodSugarValue = value * 18.0182;
                        } else {
                            bloodSugarValue = value;
                        }
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error converting value: " + e.getMessage());
                }
            }
        });

        // Add radio button change listener
        unitGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (!bloodSugarInput.getText().toString().isEmpty()) {
                try {
                    double value = Double.parseDouble(bloodSugarInput.getText().toString());
                    if (checkedId == R.id.mmolUnit) {
                        bloodSugarValue = value * 18.0182;
                    } else {
                        bloodSugarValue = value;
                    }
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Error converting value: " + e.getMessage());
                }
            }
        });

        toolbar.setNavigationOnClickListener(v -> {
            Intent intent = new Intent(this, UserListActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void initializeViews() {
        bloodSugarInput = findViewById(R.id.bloodSugarInput);
        saveButton = findViewById(R.id.saveButton);
        viewLogsButton = findViewById(R.id.viewLogsButton);
        viewRemindersButton = findViewById(R.id.viewRemindersButton);
        welcomeText = findViewById(R.id.welcomeText);

        // Set up root view click listener
        View rootView = findViewById(R.id.root_layout);
        if (rootView != null) {
            rootView.setOnClickListener(v -> KeyboardUtils.hideKeyboard(this));
        }
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
        if (item.getItemId() == android.R.id.home) {
            Intent intent = new Intent(this, UserListActivity.class);
            startActivity(intent);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_language) {
            showLanguageDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void saveReading() {
        try {
            String input = bloodSugarInput.getText().toString().trim();
            if (input.isEmpty()) {
                bloodSugarInput.setError(getString(R.string.error_empty_input));
                Toast.makeText(this, getString(R.string.error_empty_input), Toast.LENGTH_SHORT).show();
                return;
            }

            LogEntry logEntry = new LogEntry(bloodSugarValue, System.currentTimeMillis(), 
                ((DialogApp) getApplication()).getCurrentUserId());

            new Thread(() -> {
                AppDatabase.getInstance(this).logEntryDao().insert(logEntry);
                runOnUiThread(() -> {
                    // Hide keyboard
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null && getCurrentFocus() != null) {
                        imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                    }

                    // Show checkmark animation
                    ImageView checkmark = new ImageView(this);
                    checkmark.setImageResource(R.drawable.check);
                    
                    FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                        225,
                        225,
                        Gravity.CENTER
                    );
                    
                    FrameLayout container = new FrameLayout(this);
                    container.addView(checkmark, params);
                    addContentView(container, new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    ));
                    
                    checkmark.setScaleX(0);
                    checkmark.setScaleY(0);
                    checkmark.animate()
                        .scaleX(1)
                        .scaleY(1)
                        .setDuration(500)
                        .withEndAction(() -> {
                            new Handler().postDelayed(() -> {
                                ((ViewGroup) container.getParent()).removeView(container);
                                Intent intent = new Intent(this, LogViewActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                                startActivity(intent);
                            }, 200);
                        })
                        .start();
                });
            }).start();
        } catch (NumberFormatException e) {
            bloodSugarInput.setError(getString(R.string.error_invalid_input));
            Toast.makeText(this, getString(R.string.error_invalid_input), Toast.LENGTH_SHORT).show();
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
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if (v instanceof EditText) {
                android.graphics.Rect outRect = new android.graphics.Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    KeyboardUtils.hideKeyboard(this);
                }
            }
        }
        return super.dispatchTouchEvent(event);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(this, UserListActivity.class);
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