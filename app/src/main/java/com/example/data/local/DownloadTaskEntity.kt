package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadStatus
import com.example.data.model.ExtractorType

@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey val id: String,
    val sourceUrl: String,
    val pageUrl: String?,
    val title: String,
    val uploader: String,
    val thumbnailUrl: String?,
    val durationSeconds: Long,
    val extractorType: ExtractorType,
    val formatId: String,
    val formatLabel: String,
    val ext: String,
    val isAudioOnly: Boolean,
    val status: DownloadStatus,
    val progressPercent: Int = 0,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val etaSeconds: Long = -1L,
    val statusLine: String = "",
    val tempFilePath: String? = null,
    val mediaStoreUri: String? = null,
    val finalFileName: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)
