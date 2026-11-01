package com.example.domain.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.R
import com.example.data.local.PulseFetchDatabase
import com.example.data.model.DownloadStatus
import com.example.data.model.ExtractorType
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.extractor.YtDlpExtractorEngine
import com.example.domain.storage.MediaStorePublisher
import com.example.util.FormatUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class MediaDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val database = PulseFetchDatabase.getInstance(appContext)
    private val dao = database.downloadTaskDao()
    private val prefsRepo = UserPreferencesRepository(appContext)
    private val ytDlpEngine = YtDlpExtractorEngine(appContext)
    private val mediaStorePublisher = MediaStorePublisher(appContext)
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val taskId = inputData.getString(KEY_TASK_ID) ?: return@withContext Result.failure()
        val task = dao.getTaskById(taskId) ?: return@withContext Result.failure()

        ensureNotificationChannel()
        val notificationId = taskId.hashCode()
        runCatching {
            setForeground(createForegroundInfo(notificationId, task.title, 0, "Starting download…"))
        }

        val stagingDir = File(applicationContext.externalCacheDir ?: applicationContext.cacheDir, "downloads")
        if (!stagingDir.exists()) stagingDir.mkdirs()

        dao.updateProgress(
            id = taskId,
            status = DownloadStatus.DOWNLOADING,
            progressPercent = task.progressPercent.coerceIn(0, 99),
            downloadedBytes = task.downloadedBytes,
            totalBytes = task.totalBytes,
            speedBytesPerSec = 0L,
            etaSeconds = -1L,
            statusLine = "Preparing media stream…"
        )

        try {
            val downloadedFile: File = when (task.extractorType) {
                ExtractorType.OKHTTP_DIRECT -> {
                    executeOkHttpRangeDownload(
                        taskId = taskId,
                        notificationId = notificationId,
                        sourceUrl = task.sourceUrl,
                        pageUrl = task.pageUrl,
                        title = task.title,
                        ext = task.ext,
                        stagingDir = stagingDir
                    )
                }
                ExtractorType.YTDLP_FORMAT,
                ExtractorType.YTDLP_HLS_DASH -> {
                    ytDlpEngine.ensureInitialized().getOrThrow()
                    var lastUpdateMillis = 0L
                    val result = ytDlpEngine.executeDownload(
                        taskId = taskId,
                        sourceUrl = task.sourceUrl,
                        pageUrl = task.pageUrl,
                        extractorType = task.extractorType,
                        formatId = task.formatId,
                        ext = task.ext,
                        isAudioOnly = task.isAudioOnly,
                        outputDir = stagingDir
                    ) { progress, etaInSeconds, line ->
                        val now = System.currentTimeMillis()
                        if (now - lastUpdateMillis >= 400L || progress >= 99f) {
                            lastUpdateMillis = now
                            val pct = progress.toInt().coerceIn(0, 99)
                            val cleanLine = line.trim().take(90)
                            runBlocking {
                                dao.updateProgress(
                                    id = taskId,
                                    status = DownloadStatus.DOWNLOADING,
                                    progressPercent = pct,
                                    downloadedBytes = 0L,
                                    totalBytes = task.totalBytes,
                                    speedBytesPerSec = 0L,
                                    etaSeconds = etaInSeconds,
                                    statusLine = cleanLine.ifEmpty { "Downloading ($pct%)…" }
                                )
                                runCatching {
                                    setForeground(
                                        createForegroundInfo(
                                            notificationId = notificationId,
                                            title = task.title,
                                            progress = pct,
                                            subText = if (etaInSeconds > 0) "ETA ${FormatUtils.formatEta(etaInSeconds)}" else "$pct%"
                                        )
                                    )
                                }
                            }
                        }
                    }
                    result.getOrThrow()
                }
            }

            // Export completed file to MediaStore Movies/ or Music/
            val prefs = prefsRepo.getSnapshot()
            val publishResult = mediaStorePublisher.publishToMediaStore(
                sourceFile = downloadedFile,
                desiredDisplayName = task.title,
                isAudioOnly = task.isAudioOnly,
                subfolder = prefs.customSubfolder
            ).getOrThrow()

            dao.markCompleted(
                id = taskId,
                mediaStoreUri = publishResult.uri.toString(),
                finalFileName = publishResult.displayName,
                fileSize = publishResult.sizeBytes
            )

            showCompletedNotification(
                notificationId = notificationId + 1000,
                title = task.title,
                folder = if (task.isAudioOnly) "Music/${prefs.customSubfolder}" else "Movies/${prefs.customSubfolder}"
            )

            Result.success()
        } catch (ce: CancellationException) {
            ytDlpEngine.cancelProcess(taskId)
            val latest = dao.getTaskById(taskId)
            if (latest != null && latest.status != DownloadStatus.PAUSED && latest.status != DownloadStatus.CANCELLED) {
                dao.updateStatus(taskId, DownloadStatus.PAUSED, "Download paused")
            }
            Result.failure()
        } catch (e: Exception) {
            val latest = dao.getTaskById(taskId)
            if (latest?.status == DownloadStatus.PAUSED || latest?.status == DownloadStatus.CANCELLED) {
                return@withContext Result.failure()
            }
            val msg = e.localizedMessage ?: "Download interrupted"
            dao.updateStatus(taskId, DownloadStatus.FAILED, msg)
            Result.failure()
        }
    }

    private suspend fun executeOkHttpRangeDownload(
        taskId: String,
        notificationId: Int,
        sourceUrl: String,
        pageUrl: String?,
        title: String,
        ext: String,
        stagingDir: File
    ): File {
        val safeExt = ext.ifBlank { "mp4" }
        val targetFile = File(stagingDir, "direct_${taskId}.$safeExt")
        val existingBytes = if (targetFile.exists()) targetFile.length() else 0L

        val reqBuilder = Request.Builder().url(sourceUrl)
        if (!pageUrl.isNullOrBlank()) {
            reqBuilder.header("Referer", pageUrl)
        }
        if (existingBytes > 0L) {
            reqBuilder.header("Range", "bytes=$existingBytes-")
        }

        okHttpClient.newCall(reqBuilder.build()).execute().use { response ->
            if (!response.isSuccessful && response.code != 206) {
                throw IllegalStateException("Server returned HTTP ${response.code}")
            }

            val isPartial = response.code == 206
            val body = response.body ?: throw IllegalStateException("Empty response body")
            val contentLength = body.contentLength()
            val startOffset = if (isPartial) existingBytes else 0L
            val totalBytes = if (contentLength > 0) startOffset + contentLength else -1L

            FileOutputStream(targetFile, isPartial).use { output ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead: Int
                var currentBytes = startOffset
                var lastTickTime = System.currentTimeMillis()
                var bytesAtLastTick = currentBytes

                val sourceStream = body.byteStream()
                while (sourceStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isStopped) {
                        throw CancellationException("Worker stopped")
                    }
                    output.write(buffer, 0, bytesRead)
                    currentBytes += bytesRead

                    val now = System.currentTimeMillis()
                    val elapsedMs = now - lastTickTime
                    if (elapsedMs >= 450L) {
                        val deltaBytes = currentBytes - bytesAtLastTick
                        val speed = if (elapsedMs > 0) (deltaBytes * 1000L) / elapsedMs else 0L
                        val pct = if (totalBytes > 0L) {
                            ((currentBytes * 100L) / totalBytes).toInt().coerceIn(0, 99)
                        } else {
                            0
                        }
                        val eta = if (speed > 0L && totalBytes > currentBytes) {
                            (totalBytes - currentBytes) / speed
                        } else {
                            -1L
                        }

                        dao.updateProgress(
                            id = taskId,
                            status = DownloadStatus.DOWNLOADING,
                            progressPercent = pct,
                            downloadedBytes = currentBytes,
                            totalBytes = totalBytes.coerceAtLeast(0L),
                            speedBytesPerSec = speed,
                            etaSeconds = eta,
                            statusLine = "${FormatUtils.formatBytes(currentBytes)} / ${FormatUtils.formatBytes(totalBytes)} • ${FormatUtils.formatSpeed(speed)}"
                        )

                        runCatching {
                            setForeground(
                                createForegroundInfo(
                                    notificationId = notificationId,
                                    title = title,
                                    progress = pct,
                                    subText = "${FormatUtils.formatSpeed(speed)} • $pct%"
                                )
                            )
                        }

                        lastTickTime = now
                        bytesAtLastTick = currentBytes
                    }
                }
                output.flush()
            }
        }

        return targetFile
    }

    private fun createForegroundInfo(
        notificationId: Int,
        title: String,
        progress: Int,
        subText: String
    ): ForegroundInfo {
        val cancelIntent = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title.take(60))
            .setContentText(subText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress.coerceIn(0, 100), progress <= 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Pause / Cancel", cancelIntent)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }

    private fun showCompletedNotification(notificationId: Int, title: String, folder: String) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Saved: ${title.take(50)}")
            .setContentText("Saved to $folder")
            .setAutoCancel(true)
            .build()
        runCatching {
            notificationManager.notify(notificationId, notification)
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PulseFetch Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Active video and audio download progress"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "pulsefetch_downloads_channel"
        const val KEY_TASK_ID = "key_task_id"
    }
}
