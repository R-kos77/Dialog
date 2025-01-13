package com.example.dialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.util.Map;

import com.example.dialog.utils.KeyboardUtils;
import com.example.dialog.utils.LocaleHelper;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.ContextThemeWrapper;

import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {
    private EditText firstNameInput;
    private EditText lastNameInput;

    private RadioGroup genderGroup;
    private Button saveButton;
    private String editingUser = null;
    private static final String TAG = "UserProfileActivity";
    private boolean isNewUser = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);
        
        initializeViews();
        setupToolbar();
        setupSaveButton();
        setupTouchListener();
        
        isNewUser = getIntent().getBooleanExtra("isNewUser", false);
        
        if (!isNewUser) {
            loadUserData();
        } else {
            clearFields();
        }
    }

    private void initializeViews() {
        firstNameInput = findViewById(R.id.firstNameInput);
        lastNameInput = findViewById(R.id.lastNameInput);

        genderGroup = findViewById(R.id.genderGroup);
        saveButton = findViewById(R.id.saveButton);
    }

    private void clearFields() {
        firstNameInput.setText("");
        lastNameInput.setText("");

        genderGroup.clearCheck();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(isNewUser ? 
            getString(R.string.new_profile) : 
            getString(R.string.edit_profile));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadUserData() {
        try {
            SharedPreferences prefs = getSharedPreferences("CurrentUser", MODE_PRIVATE);
            String userJson = prefs.getString("userData", "");
            if (!userJson.isEmpty()) {
                JSONObject user = new JSONObject(userJson);
                firstNameInput.setText(user.getString("firstName"));
                lastNameInput.setText(user.getString("lastName"));

                
                if (user.getString("gender").equals("Male")) {
                    genderGroup.check(R.id.maleRadio);
                } else {
                    genderGroup.check(R.id.femaleRadio);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading user data: " + e.getMessage());
        }
    }

    private void setupSaveButton() {
        saveButton.setOnClickListener(v -> {
            if (validateInputs()) {
                saveUserProfile();
            }
        });
    }

    private boolean validateInputs() {
        if (firstNameInput.getText().toString().trim().isEmpty()) {
            firstNameInput.setError(getString(R.string.first_name_required));
            return false;
        }
        if (lastNameInput.getText().toString().trim().isEmpty()) {
            lastNameInput.setError(getString(R.string.last_name_required));
            return false;
        }

        if (genderGroup.getCheckedRadioButtonId() == -1) {
            Toast.makeText(this, getString(R.string.select_gender), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void saveUserProfile() {
        try {
            JSONObject userJson = new JSONObject();
            userJson.put("firstName", firstNameInput.getText().toString().trim());
            userJson.put("lastName", lastNameInput.getText().toString().trim());

            userJson.put("gender", 
                genderGroup.getCheckedRadioButtonId() == R.id.maleRadio ? "Male" : "Female");

            SharedPreferences prefs = getSharedPreferences("UserProfiles", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            // Generate new ID for new user
            String userId = getIntent().getStringExtra("userId");
            if (userId == null) {  // This is a new user
                userId = "user_" + System.currentTimeMillis();
            }
                
            editor.putString(userId, userJson.toString());
            editor.apply();

            // If this is a new user, set them as current user
            if (getIntent().getBooleanExtra("isNewUser", false)) {
                SharedPreferences currentUserPrefs = getSharedPreferences("CurrentUser", MODE_PRIVATE);
                currentUserPrefs.edit().putString("userData", userJson.toString()).apply();
            }

            // Hide keyboard before finishing
            KeyboardUtils.hideKeyboard(this);
            
            Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Log.e(TAG, "Error saving profile: " + e.getMessage());
            Toast.makeText(this, R.string.error_saving, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupTouchListener() {
        // Set up touch listener for non-text box views to hide keyboard
        View mainLayout = findViewById(android.R.id.content);
        mainLayout.setOnTouchListener((v, event) -> {
            if (getCurrentFocus() != null) {
                KeyboardUtils.hideKeyboard(this);
                getCurrentFocus().clearFocus();
            }
            return false;
        });

        // Set up focus change listeners for EditText fields
        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (!hasFocus) {
                KeyboardUtils.hideKeyboard(this);
            }
        };

        firstNameInput.setOnFocusChangeListener(focusChangeListener);
        lastNameInput.setOnFocusChangeListener(focusChangeListener);

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

    private void showLanguageDialog() {
        String[] languages = {"English", "አማርኛ"};
        new AlertDialog.Builder(this)
            .setTitle("Select Language / ቋንቋ ይምረጡ")
            .setItems(languages, (dialog, which) -> {
                String languageCode = (which == 0) ? "en" : "am";
                LocaleHelper.setLocale(this, languageCode);
            })
            .show();
    }

    @Override
    public void onBackPressed() {
        if (hasUnsavedChanges()) {
            showQuitDialog();
        } else {
            super.onBackPressed();
        }
    }

    private boolean hasUnsavedChanges() {
        try {
            SharedPreferences prefs = getSharedPreferences("CurrentUser", MODE_PRIVATE);
            String userJson = prefs.getString("userData", "");
            if (!userJson.isEmpty()) {
                JSONObject user = new JSONObject(userJson);
                String currentFirstName = firstNameInput.getText().toString();
                String currentLastName = lastNameInput.getText().toString();

                String currentGender = genderGroup.getCheckedRadioButtonId() == R.id.maleRadio ? "Male" : "Female";

                return !currentFirstName.equals(user.getString("firstName")) ||
                       !currentLastName.equals(user.getString("lastName")) ||

                       !currentGender.equals(user.getString("gender"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking changes: " + e.getMessage());
        }
        return false;
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.updateResources(newBase, LocaleHelper.getLanguage(newBase)));
    }

    private void showQuitDialog() {
        try {
            if (!isFinishing()) {
                Context dialogContext = new ContextThemeWrapper(
                    LocaleHelper.updateResources(this, LocaleHelper.getLanguage(this)),
                    androidx.appcompat.R.style.Theme_AppCompat_DayNight_Dialog_Alert
                );
                
                new AlertDialog.Builder(dialogContext)
                    .setTitle(R.string.quit_without_saving)
                    .setMessage(R.string.quit_without_saving_message)
                    .setPositiveButton(R.string.quit, (dialog, which) -> {
                        super.onBackPressed();
                    })
                    .setNegativeButton(R.string.stay, null)
                    .show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing quit dialog: " + e.getMessage());
            // If dialog fails, just do the default back action
            super.onBackPressed();
        }
    }
} 