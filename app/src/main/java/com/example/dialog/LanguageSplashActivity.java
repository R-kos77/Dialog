package com.example.dialog;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dialog.utils.LocaleHelper;

public class LanguageSplashActivity extends AppCompatActivity {

    private static final String LANGUAGE_PREF = "Settings";
    private static final String LANGUAGE_KEY = "Locale.Helper.Selected.Language";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if language is already selected
        SharedPreferences prefs = getSharedPreferences(LANGUAGE_PREF, MODE_PRIVATE);
        String savedLanguage = prefs.getString(LANGUAGE_KEY, null);

        if (savedLanguage != null) {
            // Apply saved language and go to next screen
            LocaleHelper.setLocale(this, savedLanguage);
            Context context = LocaleHelper.updateResources(this, savedLanguage);
            startActivity(new Intent(context, UserListActivity.class));
            finish();
            return;
        }

        // If no language selected yet, show language selection screen
        setContentView(R.layout.activity_language_splash);

        View englishOption = findViewById(R.id.english_option);
        View amharicOption = findViewById(R.id.amharic_option);

        englishOption.setOnClickListener(v -> {
            saveAndSetLanguage("en");
            startActivity(new Intent(this, UserListActivity.class));
            finish();
        });

        amharicOption.setOnClickListener(v -> {
            saveAndSetLanguage("am");
            startActivity(new Intent(this, UserListActivity.class));
            finish();
        });
    }

    private void saveAndSetLanguage(String languageCode) {
        SharedPreferences prefs = getSharedPreferences(LANGUAGE_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(LANGUAGE_KEY, languageCode);
        editor.apply();

        LocaleHelper.setLocale(this, languageCode);
        LocaleHelper.updateResources(this, languageCode);
    }
} 