package com.example.dialog;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;
import android.util.Log;
import java.util.List;

@Dao
public interface LogEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LogEntry entry);

    @Update
    void update(LogEntry entry);

    @Delete
    void delete(LogEntry entry);

    @Query("SELECT * FROM blood_sugar_logs WHERE userId = :userId ORDER BY timestamp DESC")
    List<LogEntry> getAllForUser(String userId);

    @Query("SELECT * FROM blood_sugar_logs WHERE id = :id AND userId = :userId")
    LogEntry getById(int id, String userId);

    // For debugging only
    @Query("SELECT DISTINCT userId FROM blood_sugar_logs")
    List<String> getAllUserIds();
} 