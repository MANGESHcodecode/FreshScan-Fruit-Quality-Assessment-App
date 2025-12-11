package com.surendramaran.yolov8tflite

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fruitName: String,
    val condition: String, // Fresh or Rotten
    val confidence: Float,
    val shelfLifeDays: Int,
    val scanTimestamp: Long = System.currentTimeMillis(),
    val qualityScore: Float = 0f, // 0-10 scale
    val detectedIssues: String = "", // Comma-separated issues
    val imagePath: String? = null // Path to saved image if available
)

