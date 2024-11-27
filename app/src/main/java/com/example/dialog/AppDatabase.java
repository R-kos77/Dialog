package com.example.dialog;

import android.content.Context;
import android.util.Log;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
    entities = {LogEntry.class, Reminder.class}, 
    version = 3,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {
    private static final String TAG = "AppDatabase";
    private static AppDatabase instance;
    
    public abstract LogEntryDao logEntryDao();
    public abstract ReminderDao reminderDao();
    
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            try {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS blood_sugar_logs_temp (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "bloodSugar REAL NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "userId TEXT, " +
                    "notes TEXT DEFAULT '')"
                );

                database.execSQL(
                    "INSERT INTO blood_sugar_logs_temp (id, bloodSugar, timestamp, userId) " +
                    "SELECT id, bloodSugar, timestamp, userId FROM blood_sugar_logs"
                );

                database.execSQL("DROP TABLE blood_sugar_logs");

                database.execSQL("ALTER TABLE blood_sugar_logs_temp RENAME TO blood_sugar_logs");
                
                Log.d(TAG, "Migration 1->2 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error in migration 1->2: " + e.getMessage());
                throw e;
            }
        }
    };
    
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            try {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS reminders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "hourOfDay INTEGER NOT NULL, " +
                    "minute INTEGER NOT NULL, " +
                    "isEnabled INTEGER NOT NULL DEFAULT 1)"
                );
                
                Log.d(TAG, "Migration 2->3 completed successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error in migration 2->3: " + e.getMessage());
                throw e;
            }
        }
    };
    
    private static final RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onCreate(SupportSQLiteDatabase db) {
            super.onCreate(db);
            Log.d(TAG, "Database created");
            
            try {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS blood_sugar_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "bloodSugar REAL NOT NULL, " +
                    "timestamp INTEGER NOT NULL, " +
                    "userId TEXT, " +
                    "notes TEXT DEFAULT '')"
                );
                
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS reminders (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "userId TEXT NOT NULL, " +
                    "type TEXT NOT NULL, " +
                    "hourOfDay INTEGER NOT NULL, " +
                    "minute INTEGER NOT NULL, " +
                    "isEnabled INTEGER NOT NULL DEFAULT 1)"
                );
            } catch (Exception e) {
                Log.e(TAG, "Error creating initial tables: " + e.getMessage());
                throw e;
            }
        }

        @Override
        public void onOpen(SupportSQLiteDatabase db) {
            super.onOpen(db);
            Log.d(TAG, "Database opened");
        }
    };
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            try {
                instance = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "dialog_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .addCallback(roomCallback)
                .build();
                
                Log.d(TAG, "Database instance created successfully");
            } catch (Exception e) {
                Log.e(TAG, "Error creating database instance: " + e.getMessage(), e);
                throw new RuntimeException("Database creation failed", e);
            }
        }
        return instance;
    }
    
    public static void destroyInstance() {
        if (instance != null && instance.isOpen()) {
            instance.close();
        }
        instance = null;
        Log.d(TAG, "Database instance destroyed");
    }
} 