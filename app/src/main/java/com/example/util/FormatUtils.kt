package com.example.util

import com.example.data.model.SearchEngine
import java.util.Locale

object FormatUtils {
    private val URL_REGEX = Regex("""(https?://[^\s"'<>]+)""", RegexOption.IGNORE_CASE)

    fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = URL_REGEX.find(text.trim())
        return match?.value?.trimEnd('.', ',', ')', ']', ';')
    }

    fun isLikelyUrl(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.contains(" ")) return false
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return true
        }
        return trimmed.contains(".") && !trimmed.startsWith(".") && !trimmed.endsWith(".")
    }

    fun normalizeBrowserInput(
        input: String,
        searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO
    ): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return searchEngine.homeUrl
        return if (isLikelyUrl(trimmed)) {
            if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "https://$trimmed"
            }
        } else {
            val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
            String.format(Locale.US, searchEngine.queryTemplate, encoded)
        }
    }

    fun extractDisplayDomain(url: String): String {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return ""
        return runCatching {
            val withoutScheme = trimmed
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
            withoutScheme.substringBefore("/").substringBefore("?").ifBlank { trimmed }
        }.getOrDefault(trimmed)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0L) return "Unknown size"
        val kb = 1024.0
        val mb = kb * 1024.0
        val gb = mb * 1024.0
        return when {
            bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
            bytes >= mb -> String.format(Locale.US, "%.1f MB", bytes / mb)
            bytes >= kb -> String.format(Locale.US, "%.0f KB", bytes / kb)
            else -> "$bytes B"
        }
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0 KB/s"
        val kb = 1024.0
        val mb = kb * 1024.0
        return when {
            bytesPerSec >= mb -> String.format(Locale.US, "%.1f MB/s", bytesPerSec / mb)
            else -> String.format(Locale.US, "%.0f KB/s", bytesPerSec / kb)
        }
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0L) return "LIVE / --:--"
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hrs > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }
    }

    fun formatEta(etaSeconds: Long): String {
        if (etaSeconds < 0L) return "--:--"
        if (etaSeconds == 0L) return "0s"
        val mins = etaSeconds / 60
        val secs = etaSeconds % 60
        return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
    }

    fun formatRelativeTime(timestampMillis: Long): String {
        if (timestampMillis <= 0L) return "Recently"
        val diffSeconds = ((System.currentTimeMillis() - timestampMillis) / 1000L).coerceAtLeast(0L)
        return when {
            diffSeconds < 60L -> "Just now"
            diffSeconds < 3600L -> "${diffSeconds / 60L}m ago"
            diffSeconds < 86400L -> "${diffSeconds / 3600L}h ago"
            else -> "${diffSeconds / 86400L}d ago"
        }
    }
}
