package com.example.dialog;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.ContextThemeWrapper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dialog.utils.LocaleHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReminderActivity extends BaseActivity {
    private ListView reminderList;
    private FloatingActionButton addReminderFab;
    private final List<Reminder> reminders = new ArrayList<>();
    private ReminderAdapter adapter;
    private static final String TAG = "ReminderActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.reminders_header));
        }
        
        setupToolbar();
        initializeViews();
        setupListeners();
        loadReminders();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.reminders_header));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeViews() {
        reminderList = findViewById(R.id.reminderList);
        addReminderFab = findViewById(R.id.addReminderFab);
        adapter = new ReminderAdapter(this, reminders);
        reminderList.setAdapter(adapter);
    }

    private void setupListeners() {
        addReminderFab.setOnClickListener(v -> showAddReminderDialog());
    }

    private void loadReminders() {
        new Thread(() -> {
            List<Reminder> loadedReminders = AppDatabase.getInstance(this)
                .reminderDao()
                .getAllForUser(((DialogApp) getApplication()).getCurrentUserId());
            
            runOnUiThread(() -> {
                reminders.clear();
                if (loadedReminders != null) {
                    reminders.addAll(loadedReminders);
                }
                adapter.notifyDataSetChanged();
            });
        }).start();
    }

    private void showAddReminderDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_reminder, null);
        RadioGroup mealTimeGroup = dialogView.findViewById(R.id.mealTimeGroup);
        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
        Button okButton = dialogView.findViewById(R.id.okButton);
        Button cancelButton = dialogView.findViewById(R.id.cancelButton);

        timePicker.setIs24HourView(false);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(dialogView)
            .create();

        mealTimeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rb_other) {
                timePicker.setVisibility(View.VISIBLE);
            } else {
                timePicker.setVisibility(View.GONE);
            }
        });

        okButton.setOnClickListener(v -> {
            int selectedId = mealTimeGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(this, R.string.select_time, Toast.LENGTH_SHORT).show();
                return;
            }

            String reminderType;
            if (selectedId == R.id.rb_other) {
                int hour = timePicker.getCurrentHour();
                int minute = timePicker.getCurrentMinute();
                reminderType = "other";
                saveReminder(hour, minute, reminderType);
                dialog.dismiss();
            } else {
                dialog.dismiss();
                showTimePickerForMeal(selectedId);
            }
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showTimePickerForMeal(int mealTimeId) {
        int suggestedHour, suggestedMinute;
        String reminderType;
        
        if (mealTimeId == R.id.rb_before_breakfast) {
            suggestedHour = 7;
            suggestedMinute = 30;
            reminderType = "before_breakfast";
        } else if (mealTimeId == R.id.rb_after_breakfast) {
            suggestedHour = 9;
            suggestedMinute = 0;
            reminderType = "after_breakfast";
        } else if (mealTimeId == R.id.rb_before_lunch) {
            suggestedHour = 11;
            suggestedMinute = 30;
            reminderType = "before_lunch";
        } else if (mealTimeId == R.id.rb_after_lunch) {
            suggestedHour = 13;
            suggestedMinute = 30;
            reminderType = "after_lunch";
        } else if (mealTimeId == R.id.rb_before_dinner) {
            suggestedHour = 17;
            suggestedMinute = 30;
            reminderType = "before_dinner";
        } else if (mealTimeId == R.id.rb_after_dinner) {
            suggestedHour = 19;
            suggestedMinute = 30;
            reminderType = "after_dinner";
        } else {
            suggestedHour = 12;
            suggestedMinute = 0;
            reminderType = "other";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_time_picker, null);
        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);
        
        timePicker.setIs24HourView(false);
        timePicker.setCurrentHour(suggestedHour);
        timePicker.setCurrentMinute(suggestedMinute);

        builder.setView(dialogView)
               .setTitle(R.string.select_time)
               .setPositiveButton(R.string.ok, (dialog, which) -> {
                   int hourOfDay = timePicker.getCurrentHour();
                   int minute = timePicker.getCurrentMinute();
                   saveReminder(hourOfDay, minute, reminderType);
               })
               .setNegativeButton(R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveReminder(int hour, int minute, String type) {
        new Thread(() -> {
            try {
                Reminder reminder = new Reminder(
                    ((DialogApp) getApplication()).getCurrentUserId(),
                    type,
                    hour,
                    minute
                );
                
                long id = AppDatabase.getInstance(this).reminderDao().insert(reminder);
                reminder.id = (int) id;
                
                runOnUiThread(() -> {
                    reminders.add(reminder);
                    adapter.notifyDataSetChanged();
                    scheduleReminder(reminder);
                    Toast.makeText(this, getString(R.string.reminder_set, 
                        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)), 
                        Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error saving reminder: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, R.string.error_saving_reminder, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void scheduleReminder(Reminder reminder) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(this, ReminderReceiver.class);
            intent.putExtra("reminderType", reminder.type);
            intent.putExtra("reminderId", reminder.id);
            intent.putExtra("hourOfDay", reminder.hourOfDay);
            intent.putExtra("minute", reminder.minute);
            intent.putExtra("userId", reminder.userId);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                reminder.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Set time for alarm
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, reminder.hourOfDay);
            calendar.set(Calendar.MINUTE, reminder.minute);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            // If time has passed today, schedule for tomorrow
            if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1);
            }

            if (reminder.isEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setAlarmClock(
                            new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent),
                            pendingIntent
                        );
                    } else {
                        Intent permissionIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        startActivity(permissionIntent);
                    }
                } else {
                    alarmManager.setAlarmClock(
                        new AlarmManager.AlarmClockInfo(calendar.getTimeInMillis(), pendingIntent),
                        pendingIntent
                    );
                }
                Toast.makeText(this, 
                    String.format(Locale.getDefault(), "Daily reminder set for %02d:%02d", 
                        reminder.hourOfDay, reminder.minute),
                    Toast.LENGTH_SHORT).show();
            } else {
                alarmManager.cancel(pendingIntent);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scheduling reminder: " + e.getMessage());
            Toast.makeText(this, "Error setting reminder", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelReminder(Reminder reminder) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
            this,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    private class ReminderAdapter extends ArrayAdapter<Reminder> {
        public ReminderAdapter(Context context, List<Reminder> reminders) {
            super(context, 0, reminders);
        }

        @Override
        @NonNull
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.reminder_list_item, parent, false);
            }

            Reminder reminder = getItem(position);
            if (reminder == null) return convertView;

            TextView timeText = convertView.findViewById(R.id.reminderTime);
            TextView typeText = convertView.findViewById(R.id.reminderType);
            SwitchMaterial enabledSwitch = convertView.findViewById(R.id.reminderEnabled);
            ImageButton deleteButton = convertView.findViewById(R.id.deleteButton);

            String amPm = (reminder.hourOfDay >= 12 ? "PM" : "AM");
            int hour12 = (reminder.hourOfDay > 12 ? reminder.hourOfDay - 12 : 
                         (reminder.hourOfDay == 0 ? 12 : reminder.hourOfDay));
            String timeString = String.format(Locale.US, "%d:%02d %s", 
                hour12,
                reminder.minute,
                amPm);
            timeText.setText(timeString);

            String typeString = "";
            switch(reminder.type) {
                case "before_breakfast": typeString = getString(R.string.before_breakfast); break;
                case "after_breakfast": typeString = getString(R.string.after_breakfast); break;
                case "before_lunch": typeString = getString(R.string.before_lunch); break;
                case "after_lunch": typeString = getString(R.string.after_lunch); break;
                case "before_dinner": typeString = getString(R.string.before_dinner); break;
                case "after_dinner": typeString = getString(R.string.after_dinner); break;
                case "other": typeString = getString(R.string.other); break;
            }
            typeText.setText(typeString);

            enabledSwitch.setChecked(reminder.isEnabled);
            enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                reminder.isEnabled = isChecked;
                updateReminder(reminder);
            });

            deleteButton.setOnClickListener(v -> {
                showDeleteConfirmationDialog(reminder);
            });

            return convertView;
        }
    }

    private void updateReminder(Reminder reminder) {
        new Thread(() -> {
            try {
                AppDatabase.getInstance(this).reminderDao().update(reminder);
                if (reminder.isEnabled) {
                    scheduleReminder(reminder);
                } else {
                    cancelReminder(reminder);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating reminder: " + e.getMessage());
            }
        }).start();
    }

    private void deleteReminder(Reminder reminder) {
        new Thread(() -> {
            try {
                AppDatabase.getInstance(this).reminderDao().delete(reminder);
                cancelReminder(reminder);
                runOnUiThread(() -> {
                    reminders.remove(reminder);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, getString(R.string.reminder_deleted), Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error deleting reminder: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error_deleting_reminder), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;  // Return true with no menu inflation to show no menu items
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_language) {
            showLanguageDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void showDeleteConfirmationDialog(Reminder reminder) {
        try {
            if (!isFinishing()) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
                builder.setTitle(getString(R.string.delete_reminder_title))
                       .setMessage(getString(R.string.delete_reminder_message))
                       .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                           deleteReminder(reminder);
                       })
                       .setNegativeButton(getString(R.string.cancel), (dialog, which) -> {
                           dialog.dismiss();
                       });

                AlertDialog dialog = builder.create();
                dialog.show();

                // Style the buttons
                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                
                if (positiveButton != null) {
                    positiveButton.setTextColor(getResources().getColor(R.color.colorError));
                }
                if (negativeButton != null) {
                    negativeButton.setTextColor(getResources().getColor(R.color.colorPrimary));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error showing delete dialog: " + e.getMessage());
            Toast.makeText(this, getString(R.string.error_deleting_reminder), Toast.LENGTH_SHORT).show();
        }
    }
} 