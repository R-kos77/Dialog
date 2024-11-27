package com.example.dialog;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.dialog.utils.LocaleHelper;

public class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.updateResources(newBase, LocaleHelper.getLanguage(newBase)));
    }

    protected void showLanguageDialog() {
        try {
            String currentLang = LocaleHelper.getLanguage(this);
            String[] languages = {getString(R.string.english), getString(R.string.amharic)};
            int checkedItem = currentLang.equals("am") ? 1 : 0;

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_language)
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    try {
                        String selectedLanguage = (which == 0) ? "en" : "am";
                        if (!selectedLanguage.equals(currentLang)) {
                            LocaleHelper.setLocale(this, selectedLanguage);
                        }
                        dialog.dismiss();
                    } catch (Exception e) {
                        Log.e("Language", "Error changing language: " + e.getMessage());
                        Toast.makeText(this, "Error changing language", Toast.LENGTH_SHORT).show();
                    }
                });

            AlertDialog dialog = builder.create();
            dialog.show();
        } catch (Exception e) {
            Log.e("Language", "Error showing language dialog: " + e.getMessage());
            Toast.makeText(this, "Error showing language options", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Ensure correct locale is set
        LocaleHelper.updateResources(this, LocaleHelper.getLanguage(this));
    }
} 