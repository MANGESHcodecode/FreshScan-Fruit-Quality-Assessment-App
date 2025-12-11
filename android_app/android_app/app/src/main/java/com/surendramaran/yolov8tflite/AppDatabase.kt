package com.surendramaran.yolov8tflite

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScanHistory::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fruit_detection_database"
                )
                .fallbackToDestructiveMigration() // For development - in production use proper migrations
                .allowMainThreadQueries() // Temporary for debugging - remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

