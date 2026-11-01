package com.example.data.model

enum class ExtractorType {
    YTDLP_FORMAT,
    YTDLP_HLS_DASH,
    OKHTTP_DIRECT
}

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class DefaultQuality(val label: String, val description: String) {
    BEST("Best Quality", "Highest available video resolution (up to 4K)"),
    P1080("1080p Full HD", "Balanced crisp video & file size"),
    P720("720p HD", "Fast download, compact video size"),
    P480("480p SD", "Data-saving video resolution"),
    AUDIO_MP3("Audio Only (MP3)", "High-bitrate MP3 audio extraction"),
    AUDIO_M4A("Audio Only (M4A)", "Original AAC/M4A audio stream")
}

enum class StreamType(val badge: String) {
    YTDLP_PAGE("YT-DLP PAGE"),
    HLS_M3U8("HLS .M3U8"),
    DASH_MPD("DASH .MPD"),
    DIRECT_FILE("DIRECT MEDIA"),
    BLOB_FALLBACK("BLOB -> YT-DLP")
}

data class ExtractedFormat(
    val formatId: String,
    val label: String,
    val ext: String,
    val resolution: String,
    val height: Int,
    val fps: Int,
    val vcodec: String,
    val acodec: String,
    val fileSizeBytes: Long,
    val isAudioOnly: Boolean,
    val needsAudioMerge: Boolean,
    val bitrateKbps: Int
)

data class ExtractedMediaInfo(
    val sourceUrl: String,
    val webpageUrl: String,
    val title: String,
    val uploader: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val videoFormats: List<ExtractedFormat>,
    val audioFormats: List<ExtractedFormat>,
    val isProtected: Boolean = false,
    val protectionReason: String? = null
)

data class DetectedMediaItem(
    val id: String,
    val mediaUrl: String,
    val pageUrl: String,
    val title: String,
    val mimeType: String,
    val streamType: StreamType,
    val estimatedSizeBytes: Long,
    val qualityLabel: String,
    val isProtected: Boolean = false,
    val protectionReason: String? = null,
    val isBlobFallback: Boolean = false,
    val thumbnailUrl: String? = null,
    val durationSeconds: Long = 0L,
    val isAudioOnly: Boolean = false
)
