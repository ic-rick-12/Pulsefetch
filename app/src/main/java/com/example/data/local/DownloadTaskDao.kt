package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadTaskDao {
    @Query("SELECT * FROM download_tasks WHERE status != 'COMPLETED' ORDER BY createdAt DESC")
    fun observeQueueTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE status = 'COMPLETED' ORDER BY completedAt DESC, createdAt DESC")
    fun observeHistoryTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun observeAllTasks(): Flow<List<DownloadTaskEntity>>

    @Query("SELECT * FROM download_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): DownloadTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTaskEntity)

    @Update
    suspend fun updateTask(task: DownloadTaskEntity)

    @Query(
        """
        UPDATE download_tasks 
        SET status = :status,
            progressPercent = :progressPercent,
            downloadedBytes = :downloadedBytes,
            totalBytes = :totalBytes,
            speedBytesPerSec = :speedBytesPerSec,
            etaSeconds = :etaSeconds,
            statusLine = :statusLine
        WHERE id = :id
        """
    )
    suspend fun updateProgress(
        id: String,
        status: DownloadStatus,
        progressPercent: Int,
        downloadedBytes: Long,
        totalBytes: Long,
        speedBytesPerSec: Long,
        etaSeconds: Long,
        statusLine: String
    )

    @Query("UPDATE download_tasks SET status = :status, errorMessage = :errorMessage, speedBytesPerSec = 0, etaSeconds = -1 WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus, errorMessage: String? = null)

    @Query(
        """
        UPDATE download_tasks 
        SET status = 'COMPLETED',
            progressPercent = 100,
            mediaStoreUri = :mediaStoreUri,
            finalFileName = :finalFileName,
            downloadedBytes = :fileSize,
            totalBytes = :fileSize,
            speedBytesPerSec = 0,
            etaSeconds = 0,
            statusLine = 'Saved to MediaStore',
            completedAt = :completedAt
        WHERE id = :id
        """
    )
    suspend fun markCompleted(
        id: String,
        mediaStoreUri: String,
        finalFileName: String,
        fileSize: Long,
        completedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: String)

    @Query("DELETE FROM download_tasks WHERE status IN ('CANCELLED', 'FAILED')")
    suspend fun clearFailedAndCancelled()
}
