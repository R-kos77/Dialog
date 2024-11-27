package com.example.dialog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders")
public class Reminder {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String userId;
    public String type;
    public int hourOfDay;
    public int minute;
    public boolean isEnabled = true;

    public Reminder(String userId, String type, int hourOfDay, int minute) {
        this.userId = userId;
        this.type = type;
        this.hourOfDay = hourOfDay;
        this.minute = minute;
    }
} 