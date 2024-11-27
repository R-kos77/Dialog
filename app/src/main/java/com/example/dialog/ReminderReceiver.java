package com.example.dialog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String reminderType = intent.getStringExtra("reminderType");
        int reminderId = intent.getIntExtra("reminderId", 0);
        
        // Play notification sound
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DialogApp.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Blood Sugar Check")
            .setContentText(getReminderText(context, reminderType))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(alarmSound)
            .setVibrate(new long[]{0, 500, 200, 500})
            .setLights(Color.RED, 3000, 3000);

        // Create intent for opening app
        Intent openAppIntent = new Intent(context, BloodSugarInputActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context, 
            reminderId, 
            openAppIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setContentIntent(pendingIntent);

        // Play sound even if device is in silent mode
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, 
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0);
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) 
            == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(reminderId, builder.build());
            
            // Schedule next alarm
            scheduleNextAlarm(context, reminderType, reminderId);
        }
    }

    private void scheduleNextAlarm(Context context, String reminderType, int reminderId) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("reminderType", reminderType);
            intent.putExtra("reminderId", reminderId);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Set time for tomorrow
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 1);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
                );
            }
        } catch (Exception e) {
            Log.e("ReminderReceiver", "Error scheduling next alarm: " + e.getMessage());
        }
    }

    private String getReminderText(Context context, String reminderType) {
        switch (reminderType) {
            case "before_breakfast": return context.getString(R.string.reminder_before_breakfast);
            case "after_breakfast": return context.getString(R.string.reminder_after_breakfast);
            case "before_lunch": return context.getString(R.string.reminder_before_lunch);
            case "after_lunch": return context.getString(R.string.reminder_after_lunch);
            case "before_dinner": return context.getString(R.string.reminder_before_dinner);
            case "after_dinner": return context.getString(R.string.reminder_after_dinner);
            default: return context.getString(R.string.reminder_check_blood_sugar);
        }
    }
} 