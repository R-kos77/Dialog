package com.example.dialog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.example.dialog.utils.LocaleHelper;

public class LanguageSplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if language is already selected
        if (isLanguageSelected()) {
            startUserListActivity();
            finish();
            return;
        }

        setContentView(R.layout.activity_language_splash);

        CardView englishCard = findViewById(R.id.englishCard);
        CardView amharicCard = findViewById(R.id.amharicCard);

        englishCard.setOnClickListener(v -> selectLanguage("en"));
        amharicCard.setOnClickListener(v -> selectLanguage("am"));
    }

    private boolean isLanguageSelected() {
        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
        return prefs.contains("Locale.Helper.Selected.Language");
    }

    private void selectLanguage(String languageCode) {
        LocaleHelper.setLocale(this, languageCode);
        startUserListActivity();
    }

    private void startUserListActivity() {
        Intent intent = new Intent(this, UserListActivity.class);
        startActivity(intent);
        finish();
    }
} 