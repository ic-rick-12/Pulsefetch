package com.example.data.repository

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.data.local.DownloadTaskDao
import com.example.data.local.DownloadTaskEntity
import com.example.data.model.DetectedMediaItem
import com.example.data.model.DownloadStatus
import com.example.data.model.ExtractedFormat
import com.example.data.model.ExtractedMediaInfo
import com.example.data.model.ExtractorType
import com.example.data.model.StreamType
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.extractor.YtDlpExtractorEngine
import com.example.domain.storage.MediaStorePublisher
import com.example.domain.worker.MediaDownloadWorker
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class DownloadRepository(
    private val context: Context,
    private val dao: DownloadTaskDao,
    private val prefsRepo: UserPreferencesRepository,
    private val ytDlpEngine: YtDlpExtractorEngine,
    private val mediaStorePublisher: MediaStorePublisher
) {
    private val workManager = WorkManager.getInstance(context.applicationContext)

    val queueTasksFlow: Flow<List<DownloadTaskEntity>> = dao.observeQueueTasks()
    val historyTasksFlow: Flow<List<DownloadTaskEntity>> = dao.observeHistoryTasks()

    suspend fun enqueueExtractedFormat(
        mediaInfo: ExtractedMediaInfo,
        format: ExtractedFormat
    ): String {
        val taskId = UUID.randomUUID().toString()
        val task = DownloadTaskEntity(
            id = taskId,
            sourceUrl = mediaInfo.sourceUrl,
            pageUrl = mediaInfo.webpageUrl,
            title = mediaInfo.title,
            uploader = mediaInfo.uploader,
            thumbnailUrl = mediaInfo.thumbnailUrl,
            durationSeconds = mediaInfo.durationSeconds,
            extractorType = ExtractorType.YTDLP_FORMAT,
            formatId = format.formatId,
            formatLabel = format.label,
            ext = format.ext,
            isAudioOnly = format.isAudioOnly,
            status = DownloadStatus.QUEUED,
            totalBytes = format.fileSizeBytes,
            statusLine = "Queued for extraction…"
        )
        dao.insertTask(task)
        scheduleWork(taskId)
        return taskId
    }

    suspend fun enqueueBrowserStream(item: DetectedMediaItem): String {
        val taskId = UUID.randomUUID().toString()
        val extractorType = when (item.streamType) {
            StreamType.DIRECT_FILE -> ExtractorType.OKHTTP_DIRECT
            StreamType.HLS_M3U8, StreamType.DASH_MPD -> ExtractorType.YTDLP_HLS_DASH
            StreamType.YTDLP_PAGE, StreamType.BLOB_FALLBACK -> ExtractorType.YTDLP_FORMAT
        }
        val ext = when {
            item.isAudioOnly -> "m4a"
            item.mimeType.contains("webm") -> "webm"
            else -> "mp4"
        }
        val targetSourceUrl = if (item.isBlobFallback) item.pageUrl else item.mediaUrl
        val task = DownloadTaskEntity(
            id = taskId,
            sourceUrl = targetSourceUrl,
            pageUrl = item.pageUrl,
            title = item.title,
            uploader = "Browser Capture",
            thumbnailUrl = item.thumbnailUrl,
            durationSeconds = item.durationSeconds,
            extractorType = extractorType,
            formatId = "best",
            formatLabel = item.qualityLabel,
            ext = ext,
            isAudioOnly = item.isAudioOnly,
            status = DownloadStatus.QUEUED,
            totalBytes = item.estimatedSizeBytes,
            statusLine = "Queued from Browser…"
        )
        dao.insertTask(task)
        scheduleWork(taskId)
        return taskId
    }

    suspend fun pauseTask(taskId: String) {
        dao.updateStatus(taskId, DownloadStatus.PAUSED, "Paused by user")
        ytDlpEngine.cancelProcess(taskId)
        workManager.cancelUniqueWork(uniqueWorkName(taskId))
    }

    suspend fun resumeTask(taskId: String) {
        val task = dao.getTaskById(taskId) ?: return
        dao.updateProgress(
            id = taskId,
            status = DownloadStatus.QUEUED,
            progressPercent = task.progressPercent,
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes,
            speedBytesPerSec = 0L,
            etaSeconds = -1L,
            statusLine = "Resuming download…"
        )
        scheduleWork(taskId)
    }

    suspend fun retryTask(taskId: String) {
        val task = dao.getTaskById(taskId) ?: return
        dao.updateProgress(
            id = taskId,
            status = DownloadStatus.QUEUED,
            progressPercent = 0,
            downloadedBytes = 0L,
            totalBytes = task.totalBytes,
            speedBytesPerSec = 0L,
            etaSeconds = -1L,
            statusLine = "Retrying download…"
        )
        scheduleWork(taskId)
    }

    suspend fun cancelTask(taskId: String) {
        dao.updateStatus(taskId, DownloadStatus.CANCELLED, "Cancelled")
        ytDlpEngine.cancelProcess(taskId)
        workManager.cancelUniqueWork(uniqueWorkName(taskId))
        cleanupStagingFiles(taskId)
    }

    suspend fun removeTask(task: DownloadTaskEntity, deleteMediaStoreFile: Boolean) {
        ytDlpEngine.cancelProcess(task.id)
        workManager.cancelUniqueWork(uniqueWorkName(task.id))
        cleanupStagingFiles(task.id)
        if (deleteMediaStoreFile && !task.mediaStoreUri.isNullOrBlank()) {
            mediaStorePublisher.deleteFromMediaStore(task.mediaStoreUri)
        }
        dao.deleteTaskById(task.id)
    }

    suspend fun clearFailedAndCancelled() {
        dao.clearFailedAndCancelled()
    }

    private suspend fun scheduleWork(taskId: String) {
        val prefs = prefsRepo.getSnapshot()
        val networkType = if (prefs.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val inputData = Data.Builder()
            .putString(MediaDownloadWorker.KEY_TASK_ID, taskId)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MediaDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag(taskId)
            .build()

        workManager.enqueueUniqueWork(
            uniqueWorkName(taskId),
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun cleanupStagingFiles(taskId: String) {
        runCatching {
            val stagingDir = File(context.externalCacheDir ?: context.cacheDir, "downloads")
            stagingDir.listFiles()?.forEach { file ->
                if (file.name.contains(taskId)) {
                    file.delete()
                }
            }
        }
    }

    private fun uniqueWorkName(taskId: String): String = "pulsefetch_dl_$taskId"
}
