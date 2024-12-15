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
import android.provider.Settings;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = TimeUnit.MINUTES.toMillis(1);

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && 
            intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            restoreReminders(context);
            return;
        }

        if (intent.getAction() != null && 
            (intent.getAction().equals(Intent.ACTION_TIME_CHANGED) ||
             intent.getAction().equals(Intent.ACTION_TIMEZONE_CHANGED))) {
            resyncReminders(context);
            return;
        }

        int reminderId = intent.getIntExtra("reminderId", 0);
        int retryCount = intent.getIntExtra("retryCount", 0);
        
        new Thread(() -> {
            try {
                List<Reminder> reminders = AppDatabase.getInstance(context)
                    .reminderDao()
                    .getAllReminders();
                
                boolean reminderExists = false;
                boolean reminderEnabled = false;
                
                for (Reminder r : reminders) {
                    if (r.id == reminderId) {
                        reminderExists = true;
                        reminderEnabled = r.isEnabled;
                        break;
                    }
                }
                
                if (reminderExists && reminderEnabled) {
                    showNotificationAndReschedule(context, intent, retryCount);
                } else {
                    cancelReminder(context, reminderId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error verifying reminder: " + e.getMessage());
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    scheduleRetry(context, intent, retryCount);
                }
            }
        }).start();
    }

    private void resyncReminders(Context context) {
        new Thread(() -> {
            try {
                List<Reminder> reminders = AppDatabase.getInstance(context)
                    .reminderDao()
                    .getAllReminders();

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                long currentTimeMillis = System.currentTimeMillis();
                TimeZone currentTimeZone = TimeZone.getDefault();

                for (Reminder reminder : reminders) {
                    if (reminder.isEnabled) {
                        cancelReminder(context, reminder.id);
                        
                        Calendar calendar = Calendar.getInstance(currentTimeZone);
                        calendar.setTimeInMillis(currentTimeMillis);
                        calendar.set(Calendar.HOUR_OF_DAY, reminder.hourOfDay);
                        calendar.set(Calendar.MINUTE, reminder.minute);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);

                        if (calendar.getTimeInMillis() <= currentTimeMillis) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1);
                        }

                        scheduleReminderAlarm(context, reminder, calendar.getTimeInMillis());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error resyncing reminders: " + e.getMessage());
            }
        }).start();
    }

    private void scheduleReminderAlarm(Context context, Reminder reminder, long triggerTimeMillis) {
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

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(triggerTimeMillis, pendingIntent),
                    pendingIntent
                );
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
        }
    }

    private void scheduleRetry(Context context, Intent originalIntent, int currentRetryCount) {
        Intent retryIntent = new Intent(context, ReminderReceiver.class);
        retryIntent.putExtras(originalIntent);
        retryIntent.putExtra("retryCount", currentRetryCount + 1);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            context,
            originalIntent.getIntExtra("reminderId", 0),
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long nextTriggerTime = System.currentTimeMillis() + RETRY_DELAY_MS;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextTriggerTime, pendingIntent);
        }
    }

    private void cancelReminder(Context context, int reminderId) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, ReminderReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(pendingIntent);
        } catch (Exception e) {
            Log.e(TAG, "Error canceling reminder: " + e.getMessage());
        }
    }

    private void showNotificationAndReschedule(Context context, Intent intent, int retryCount) {
        String reminderType = intent.getStringExtra("reminderType");
        
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

        Intent openAppIntent = new Intent(context, BloodSugarInputActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(
            context, 
            intent.getIntExtra("reminderId", 0), 
            openAppIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setContentIntent(notifyPendingIntent);

        // Schedule next reminder before showing notification
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        calendar.setTimeInMillis(System.currentTimeMillis());
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, intent.getIntExtra("hourOfDay", 0));
        calendar.set(Calendar.MINUTE, intent.getIntExtra("minute", 0));
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        Intent nextIntent = new Intent(context, ReminderReceiver.class);
        nextIntent.putExtras(intent);
        nextIntent.removeExtra("retryCount");

        scheduleReminderAlarm(context, new Reminder(
            intent.getStringExtra("userId"),
            intent.getStringExtra("reminderType"),
            intent.getIntExtra("hourOfDay", 0),
            intent.getIntExtra("minute", 0)
        ), calendar.getTimeInMillis());

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) 
            == PackageManager.PERMISSION_GRANTED) {
            try {
                notificationManager.notify(intent.getIntExtra("reminderId", 0), builder.build());
            } catch (Exception e) {
                Log.e(TAG, "Error showing notification: " + e.getMessage());
                if (retryCount < MAX_RETRY_ATTEMPTS) {
                    scheduleRetry(context, intent, retryCount);
                }
            }
        }
    }

    private String getReminderText(Context context, String reminderType) {
        if (reminderType == null) {
            return context.getString(R.string.reminder_check_blood_sugar);
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
                List<Reminder> reminders = AppDatabase.getInstance(context)
                    .reminderDao()
                    .getAllReminders();

                long currentTimeMillis = System.currentTimeMillis();
                TimeZone currentTimeZone = TimeZone.getDefault();

                for (Reminder reminder : reminders) {
                    if (reminder.isEnabled) {
                        Calendar calendar = Calendar.getInstance(currentTimeZone);
                        calendar.setTimeInMillis(currentTimeMillis);
                        calendar.set(Calendar.HOUR_OF_DAY, reminder.hourOfDay);
                        calendar.set(Calendar.MINUTE, reminder.minute);
                        calendar.set(Calendar.SECOND, 0);
                        calendar.set(Calendar.MILLISECOND, 0);

                        if (calendar.getTimeInMillis() <= currentTimeMillis) {
                            calendar.add(Calendar.DAY_OF_YEAR, 1);
                        }

                        scheduleReminderAlarm(context, reminder, calendar.getTimeInMillis());
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error restoring reminders after boot: " + e.getMessage());
            }
        }).start();
    }
}