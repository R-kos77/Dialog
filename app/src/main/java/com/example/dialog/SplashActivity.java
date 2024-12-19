package com.example.dialog;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logoImage = findViewById(R.id.logoImage);
        
        // Start fade in animation
        logoImage.setAlpha(0f);
        logoImage.animate()
                .alpha(1f)
                .setDuration(1500)
                .withEndAction(() -> {
                    // After animation, wait longer then check where to go
                    new Handler().postDelayed(() -> {
                        // Check if language is already selected
                        SharedPreferences prefs = getSharedPreferences("Settings", MODE_PRIVATE);
                        String savedLanguage = prefs.getString("Locale.Helper.Selected.Language", null);

                        Intent intent;
                        if (savedLanguage == null) {
                            // First time - go to language selection
                            intent = new Intent(this, LanguageSplashActivity.class);
                        } else {
                            // Language already selected - go to user list
                            intent = new Intent(this, UserListActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    }, 1500);
                })
                .start();
    }
} 