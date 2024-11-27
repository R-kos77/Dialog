package com.example.dialog;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

@Entity(tableName = "blood_sugar_logs")
public class LogEntry {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public double bloodSugar; // Stored in mg/dL
    public long timestamp;
    public String userId;
    public String notes;

    @Ignore
    public LogEntry(double bloodSugar, long timestamp, String userId) {
        this.bloodSugar = bloodSugar;
        this.timestamp = timestamp;
        this.userId = userId;
        this.notes = "";
    }

    public LogEntry(double bloodSugar, long timestamp, String userId, String notes) {
        this.bloodSugar = bloodSugar;
        this.timestamp = timestamp;
        this.userId = userId;
        this.notes = notes != null ? notes : "";
    }
} 