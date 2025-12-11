package com.surendramaran.yolov8tflite

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY scanTimestamp DESC")
    fun getAllScans(): Flow<List<ScanHistory>>

    @Query("SELECT * FROM scan_history WHERE fruitName = :fruitName ORDER BY scanTimestamp DESC")
    fun getScansByFruit(fruitName: String): Flow<List<ScanHistory>>

    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun getScanById(id: Long): ScanHistory?

    @Insert
    suspend fun insertScan(scan: ScanHistory)

    @Delete
    suspend fun deleteScan(scan: ScanHistory)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScans()
}

