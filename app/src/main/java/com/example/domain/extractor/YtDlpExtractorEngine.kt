package com.example.domain.extractor

import android.content.Context
import android.util.Log
import com.example.data.model.ExtractedFormat
import com.example.data.model.ExtractedMediaInfo
import com.example.data.model.ExtractorType
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

sealed class ExtractorUpdateResult {
    data class Updated(val version: String) : ExtractorUpdateResult()
    data class AlreadyUpToDate(val version: String) : ExtractorUpdateResult()
    data class Error(val message: String) : ExtractorUpdateResult()
}

class YtDlpExtractorEngine(private val context: Context) {

    private val initMutex = Mutex()
    @Volatile
    private var isInitialized = false

    suspend fun ensureInitialized(): Result<Unit> = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext Result.success(Unit)
        initMutex.withLock {
            if (isInitialized) return@withLock Result.success(Unit)
            runCatching {
                YoutubeDL.getInstance().init(context.applicationContext)
                FFmpeg.getInstance().init(context.applicationContext)
                isInitialized = true
            }.onFailure { err ->
                Log.e(TAG, "Failed to initialize youtubedl-android / FFmpeg", err)
            }
        }
    }

    suspend fun getVersionString(): String = withContext(Dispatchers.IO) {
        ensureInitialized()
        runCatching {
            YoutubeDL.getInstance().version(context.applicationContext) ?: "Bundled yt-dlp"
        }.getOrDefault("Bundled yt-dlp")
    }

    suspend fun updateExtractor(
        useNightly: Boolean = false
    ): ExtractorUpdateResult = withContext(Dispatchers.IO) {
        ensureInitialized().onFailure {
            return@withContext ExtractorUpdateResult.Error(
                "Engine initialization failed: ${it.localizedMessage ?: "Native binaries unavailable"}"
            )
        }
        try {
            val channel = if (useNightly) {
                YoutubeDL.UpdateChannel._NIGHTLY
            } else {
                YoutubeDL.UpdateChannel._STABLE
            }
            val status = YoutubeDL.getInstance().updateYoutubeDL(context.applicationContext, channel)
            val version = getVersionString()
            when (status) {
                YoutubeDL.UpdateStatus.DONE -> ExtractorUpdateResult.Updated(version)
                YoutubeDL.UpdateStatus.ALREADY_UP_TO_DATE -> ExtractorUpdateResult.AlreadyUpToDate(version)
                else -> ExtractorUpdateResult.AlreadyUpToDate(version)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Extractor update error", e)
            ExtractorUpdateResult.Error(
                e.localizedMessage ?: "Network error while checking yt-dlp releases."
            )
        }
    }

    suspend fun extractMediaInfo(
        url: String,
        cookiesHeader: String? = null,
        userAgent: String? = null
    ): Result<ExtractedMediaInfo> = withContext(Dispatchers.IO) {
        ensureInitialized().onFailure { err ->
            return@withContext Result.failure(
                IllegalStateException("Native yt-dlp engine could not initialize: ${err.localizedMessage}")
            )
        }

        try {
            val request = YoutubeDLRequest(url).apply {
                addOption("--no-playlist")
                addOption("--socket-timeout", "20")
                if (!cookiesHeader.isNullOrBlank()) {
                    addOption("--add-header", "Cookie:$cookiesHeader")
                }
                if (!userAgent.isNullOrBlank()) {
                    addOption("--user-agent", userAgent)
                }
            }

            val videoInfo: VideoInfo = YoutubeDL.getInstance().getInfo(request)
            val mapped = mapVideoInfo(url, videoInfo)
            Result.success(mapped)
        } catch (e: YoutubeDLException) {
            val rawMessage = e.message ?: e.localizedMessage ?: "Extraction failed"
            if (isDrmOrProtectedError(rawMessage)) {
                Result.success(
                    ExtractedMediaInfo(
                        sourceUrl = url,
                        webpageUrl = url,
                        title = "Protected or Private Media",
                        uploader = "",
                        durationSeconds = 0L,
                        thumbnailUrl = null,
                        videoFormats = emptyList(),
                        audioFormats = emptyList(),
                        isProtected = true,
                        protectionReason = classifyProtectionReason(rawMessage)
                    )
                )
            } else {
                Result.failure(IllegalStateException(cleanExtractorError(rawMessage)))
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(e.localizedMessage ?: "Unable to analyze link."))
        }
    }

    fun executeDownload(
        taskId: String,
        sourceUrl: String,
        pageUrl: String?,
        extractorType: ExtractorType,
        formatId: String,
        ext: String,
        isAudioOnly: Boolean,
        outputDir: File,
        onProgress: (progressPercent: Float, etaSeconds: Long, line: String) -> Unit
    ): Result<File> {
        return runCatching {
            if (!outputDir.exists()) outputDir.mkdirs()

            val outputTemplate = File(outputDir, "dl_${taskId}.%(ext)s").absolutePath
            val request = YoutubeDLRequest(sourceUrl).apply {
                addOption("--no-playlist")
                addOption("--continue")
                addOption("--newline")
                addOption("-o", outputTemplate)
                if (!pageUrl.isNullOrBlank()) {
                    addOption("--referer", pageUrl)
                }

                when {
                    isAudioOnly -> {
                        val targetAudioExt = if (ext.lowercase() == "mp3") "mp3" else "m4a"
                        if (formatId.isNotBlank() && !formatId.startsWith("audio_")) {
                            addOption("-f", formatId)
                        } else {
                            addOption("-f", "bestaudio/best")
                        }
                        addOption("-x")
                        addOption("--audio-format", targetAudioExt)
                        addOption("--audio-quality", "0")
                    }
                    extractorType == ExtractorType.YTDLP_HLS_DASH -> {
                        addOption("--concurrent-fragments", "4")
                        addOption("--merge-output-format", "mp4")
                    }
                    else -> {
                        val formatSpec = when {
                            formatId.isBlank() || formatId == "best" -> "bestvideo+bestaudio/best"
                            formatId == "1080p" -> "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best"
                            formatId == "720p" -> "bestvideo[height<=720]+bestaudio/best[height<=720]/best"
                            formatId == "480p" -> "bestvideo[height<=480]+bestaudio/best[height<=480]/best"
                            formatId.contains("+") -> formatId
                            else -> "$formatId+bestaudio/$formatId/best"
                        }
                        addOption("-f", formatSpec)
                        addOption("--merge-output-format", "mp4")
                    }
                }
            }

            YoutubeDL.getInstance().execute(request, taskId) { progress, etaInSeconds, line ->
                onProgress(progress, etaInSeconds, line)
            }

            val downloadedFile = outputDir.listFiles()
                ?.filter {
                    it.isFile &&
                        it.name.startsWith("dl_$taskId") &&
                        !it.name.endsWith(".part") &&
                        !it.name.endsWith(".ytdl")
                }
                ?.maxByOrNull { it.length() }
                ?: throw IllegalStateException("Download finished, but output file was not found in staging.")

            downloadedFile
        }
    }

    fun cancelProcess(taskId: String): Boolean {
        return runCatching {
            YoutubeDL.getInstance().destroyProcessById(taskId)
        }.getOrDefault(false)
    }

    private fun mapVideoInfo(originalUrl: String, info: VideoInfo): ExtractedMediaInfo {
        val rawFormats: List<VideoFormat> = info.formats ?: emptyList()

        // Check if formats indicate DRM protection
        val hasAnyPlayableFormat = rawFormats.any { f ->
            !f.url.isNullOrBlank() || !f.formatId.isNullOrBlank()
        }
        if (!hasAnyPlayableFormat && info.title.isNullOrBlank()) {
            return ExtractedMediaInfo(
                sourceUrl = originalUrl,
                webpageUrl = info.webpageUrl ?: originalUrl,
                title = "Protected or Unavailable Media",
                uploader = info.uploader.orEmpty(),
                durationSeconds = info.duration.toLong(),
                thumbnailUrl = info.thumbnail,
                videoFormats = emptyList(),
                audioFormats = emptyList(),
                isProtected = true,
                protectionReason = "This video is protected and can't be downloaded"
            )
        }

        val bestAudioSize = rawFormats
            .filter { it.vcodec == "none" && it.acodec != "none" }
            .maxOfOrNull { maxOf(it.fileSize, it.fileSizeApproximate) }
            ?.coerceAtLeast(0L) ?: 0L

        // Build clean, deduplicated video quality tiers sorted from highest to lowest resolution
        val videoByHeight = mutableMapOf<Int, ExtractedFormat>()
        for (vf in rawFormats) {
            val height = vf.height
            val vcodec = vf.vcodec.orEmpty()
            val acodec = vf.acodec.orEmpty()
            if (height <= 0 || vcodec == "none") continue

            val isVideoOnly = acodec == "none" || acodec.isBlank()
            val rawSize = maxOf(vf.fileSize, vf.fileSizeApproximate).coerceAtLeast(0L)
            val totalEstimatedSize = if (isVideoOnly && rawSize > 0L) rawSize + bestAudioSize else rawSize
            val formatSpec = if (isVideoOnly) "${vf.formatId}+bestaudio" else vf.formatId.orEmpty()
            val ext = "mp4"
            val fps = vf.fps
            val qualityTag = when {
                height >= 2160 -> "2160p 4K"
                height >= 1440 -> "1440p QHD"
                height >= 1080 -> "1080p FHD"
                height >= 720 -> "720p HD"
                height >= 480 -> "480p SD"
                height >= 360 -> "360p"
                else -> "${height}p"
            }

            val candidate = ExtractedFormat(
                formatId = formatSpec,
                label = if (fps > 30) "$qualityTag • ${fps}fps" else qualityTag,
                ext = ext,
                resolution = "${vf.width.coerceAtLeast(0)}x$height",
                height = height,
                fps = fps,
                vcodec = vcodec.substringBefore("."),
                acodec = if (isVideoOnly) "aac (merged)" else acodec.substringBefore("."),
                fileSizeBytes = totalEstimatedSize,
                isAudioOnly = false,
                needsAudioMerge = isVideoOnly,
                bitrateKbps = vf.tbr
            )

            val existing = videoByHeight[height]
            if (existing == null || candidate.fileSizeBytes > existing.fileSizeBytes) {
                videoByHeight[height] = candidate
            }
        }

        val sortedVideos = videoByHeight.values.sortedByDescending { it.height }.toMutableList()
        if (sortedVideos.isEmpty()) {
            sortedVideos.add(
                ExtractedFormat(
                    formatId = "bestvideo+bestaudio/best",
                    label = "Best Available Video (Auto Merge)",
                    ext = "mp4",
                    resolution = "Auto",
                    height = 1080,
                    fps = 30,
                    vcodec = "h264",
                    acodec = "aac",
                    fileSizeBytes = maxOf(info.fileSize, info.fileSizeApproximate).coerceAtLeast(0L),
                    isAudioOnly = false,
                    needsAudioMerge = true,
                    bitrateKbps = 0
                )
            )
        }

        val audioFormats = mutableListOf(
            ExtractedFormat(
                formatId = "audio_mp3_320",
                label = "MP3 Audio • High Quality (320 kbps)",
                ext = "mp3",
                resolution = "Audio Only",
                height = 0,
                fps = 0,
                vcodec = "none",
                acodec = "mp3",
                fileSizeBytes = bestAudioSize,
                isAudioOnly = true,
                needsAudioMerge = false,
                bitrateKbps = 320
            ),
            ExtractedFormat(
                formatId = "audio_m4a_best",
                label = "M4A Audio • Original Stream (AAC)",
                ext = "m4a",
                resolution = "Audio Only",
                height = 0,
                fps = 0,
                vcodec = "none",
                acodec = "m4a",
                fileSizeBytes = bestAudioSize,
                isAudioOnly = true,
                needsAudioMerge = false,
                bitrateKbps = 192
            )
        )

        rawFormats
            .filter { it.vcodec == "none" && it.acodec != "none" && !it.formatId.isNullOrBlank() }
            .sortedByDescending { it.abr }
            .take(2)
            .forEach { af ->
                val size = maxOf(af.fileSize, af.fileSizeApproximate).coerceAtLeast(0L)
                val abr = af.abr.takeIf { it > 0 } ?: af.tbr
                val ext = if (af.ext.equals("webm", ignoreCase = true)) "m4a" else af.ext.orEmpty().ifEmpty { "m4a" }
                audioFormats.add(
                    ExtractedFormat(
                        formatId = af.formatId.orEmpty(),
                        label = "${ext.uppercase()} Stream • ${if (abr > 0) "${abr}kbps" else af.formatNote.orEmpty()}",
                        ext = ext,
                        resolution = "Audio Only",
                        height = 0,
                        fps = 0,
                        vcodec = "none",
                        acodec = af.acodec.orEmpty().substringBefore("."),
                        fileSizeBytes = size,
                        isAudioOnly = true,
                        needsAudioMerge = false,
                        bitrateKbps = abr
                    )
                )
            }

        return ExtractedMediaInfo(
            sourceUrl = originalUrl,
            webpageUrl = info.webpageUrl ?: originalUrl,
            title = info.title?.takeIf { it.isNotBlank() } ?: "Extracted Media",
            uploader = info.uploader?.takeIf { it.isNotBlank() } ?: "Web Source",
            durationSeconds = info.duration.toLong().coerceAtLeast(0L),
            thumbnailUrl = info.thumbnail,
            videoFormats = sortedVideos,
            audioFormats = audioFormats,
            isProtected = false,
            protectionReason = null
        )
    }

    private fun isDrmOrProtectedError(raw: String): Boolean {
        val lower = raw.lowercase()
        return lower.contains("drm") ||
            lower.contains("widevine") ||
            lower.contains("private video") ||
            lower.contains("this video is private") ||
            lower.contains("sign in to confirm") ||
            lower.contains("login required") ||
            lower.contains("members-only") ||
            lower.contains("subscribers only") ||
            lower.contains("paywall") ||
            lower.contains("rental")
    }

    private fun classifyProtectionReason(raw: String): String {
        val lower = raw.lowercase()
        return when {
            lower.contains("drm") || lower.contains("widevine") ->
                "This video is protected and can't be downloaded (DRM encryption detected)."
            lower.contains("private") ->
                "This video is protected and can't be downloaded (Private video)."
            lower.contains("members-only") || lower.contains("paywall") || lower.contains("subscribers") ->
                "This video is protected and can't be downloaded (Paywall / Members-only content)."
            lower.contains("sign in") || lower.contains("login") ->
                "This video is protected and can't be downloaded (Authentication required)."
            else ->
                "This video is protected and can't be downloaded."
        }
    }

    private fun cleanExtractorError(raw: String): String {
        val errorLine = raw.lines()
            .firstOrNull { it.contains("ERROR:", ignoreCase = true) }
            ?.substringAfter("ERROR:")
            ?.trim()
        return when {
            !errorLine.isNullOrBlank() -> errorLine
            raw.contains("Unsupported URL", ignoreCase = true) ->
                "Unsupported link: No extractable media was found on this URL."
            raw.contains("Unable to download webpage", ignoreCase = true) ||
                raw.contains("Name or service not known", ignoreCase = true) ->
                "Network failure: Unable to reach the server. Check your internet connection."
            else -> raw.take(240)
        }
    }

    companion object {
        private const val TAG = "YtDlpExtractorEngine"
    }
}
