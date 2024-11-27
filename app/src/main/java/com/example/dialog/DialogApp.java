package com.example.dialog;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;
import org.json.JSONObject;
import com.example.dialog.utils.LocaleHelper;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class DialogApp extends Application {
    private static final String TAG = "DialogApp";
    private String currentUserId;
    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    public static final String CHANNEL_ID = "blood_sugar_reminders";

    @Override
    public void onCreate() {
        super.onCreate();
        String savedLanguage = LocaleHelper.getLanguage(this);
        LocaleHelper.updateResources(this, savedLanguage);
        
        // Set up preference change listener
        prefListener = (prefs, key) -> {
            if ("userData".equals(key)) {
                loadCurrentUser();
            }
        };
        
        getSharedPreferences("CurrentUser", MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefListener);
            
        loadCurrentUser();
        createNotificationChannel();
    }

    public String getCurrentUserId() {
        if (currentUserId == null) {
            loadCurrentUser();
        }
        return currentUserId;
    }

    private void loadCurrentUser() {
        try {
            SharedPreferences prefs = getSharedPreferences("CurrentUser", MODE_PRIVATE);
            String userJson = prefs.getString("userData", "");
            if (!userJson.isEmpty()) {
                JSONObject user = new JSONObject(userJson);
                String newUserId = user.getString("firstName") + "_" + user.getString("lastName");
                if (!newUserId.equals(currentUserId)) {
                    Log.d(TAG, "User ID changed from " + currentUserId + " to " + newUserId);
                    currentUserId = newUserId;
                }
            } else {
                currentUserId = null;
                Log.d(TAG, "No current user found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading current user: " + e.getMessage());
            currentUserId = null;
        }
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        // Unregister the listener to prevent memory leaks
        getSharedPreferences("CurrentUser", MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefListener);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Blood Sugar Reminders",
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders to check blood sugar");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
} 