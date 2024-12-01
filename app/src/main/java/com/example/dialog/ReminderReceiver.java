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
import java.util.List;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // If this is a boot completed intent, restore all reminders
        if (intent.getAction() != null && 
            intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            restoreReminders(context);
            return;
        }

        String reminderType = intent.getStringExtra("reminderType");
        int reminderId = intent.getIntExtra("reminderId", 0);
        
        // Show notification
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
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
            context, 
            reminderId, 
            openAppIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setContentIntent(notifyPendingIntent);

        // Reschedule for tomorrow with exact same time
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            calendar.set(Calendar.HOUR_OF_DAY, intent.getIntExtra("hourOfDay", 0));
            calendar.set(Calendar.MINUTE, intent.getIntExtra("minute", 0));
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(
                context,
                intent.getIntExtra("reminderId", 0),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.setAlarmClock(
                new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), alarmPendingIntent),
                alarmPendingIntent
            );
        } catch (Exception e) {
            Log.e("ReminderReceiver", "Error rescheduling: " + e.getMessage());
        }

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) 
            == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(reminderId, builder.build());
        }
    }

    private String getReminderText(Context context, String reminderType) {
        if (reminderType == null) {
            return context.getString(R.string.reminder_check_blood_sugar);  // Default message
        }
        
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

    private void restoreReminders(Context context) {
        new Thread(() -> {
            try {
                // Get all active reminders from database
                List<Reminder> reminders = AppDatabase.getInstance(context)
                    .reminderDao()
                    .getAllReminders();  // You'll need to add this method to ReminderDao

                // Reschedule each active reminder
                for (Reminder reminder : reminders) {
                    if (reminder.isEnabled) {
                        scheduleReminder(context, reminder);
                    }
                }
            } catch (Exception e) {
                Log.e("ReminderReceiver", "Error restoring reminders: " + e.getMessage());
            }
        }).start();
    }

    private void scheduleReminder(Context context, Reminder reminder) {
        // Copy of the scheduling code from ReminderActivity
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, ReminderReceiver.class);
            intent.putExtra("reminderType", reminder.type);
            intent.putExtra("reminderId", reminder.id);
            intent.putExtra("hourOfDay", reminder.hourOfDay);
            intent.putExtra("minute", reminder.minute);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, reminder.hourOfDay);
            calendar.set(Calendar.MINUTE, reminder.minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            alarmManager.setAlarmClock(
                new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent),
                pendingIntent
            );
        } catch (Exception e) {
            Log.e("ReminderReceiver", "Error scheduling reminder: " + e.getMessage());
        }
    }
} 