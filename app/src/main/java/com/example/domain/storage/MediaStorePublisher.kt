package com.example.domain.storage

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

data class PublishedMediaResult(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long
)

class MediaStorePublisher(private val context: Context) {

    /**
     * Saves a downloaded local staging file to MediaStore Movies/<subfolder> or Music/<subfolder>
     * without requiring legacy storage permissions.
     */
    suspend fun publishToMediaStore(
        sourceFile: File,
        desiredDisplayName: String,
        isAudioOnly: Boolean,
        subfolder: String
    ): Result<PublishedMediaResult> = withContext(Dispatchers.IO) {
        runCatching {
            if (!sourceFile.exists() || sourceFile.length() == 0L) {
                throw IllegalStateException("Downloaded file is missing or empty.")
            }

            val resolver = context.contentResolver
            val extension = sourceFile.extension.lowercase().ifEmpty {
                if (isAudioOnly) "m4a" else "mp4"
            }
            val cleanBaseName = desiredDisplayName
                .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                .replace(Regex("\\s+"), " ")
                .trim()
                .take(90)
                .ifEmpty { "media_${System.currentTimeMillis()}" }

            val finalFileName = if (cleanBaseName.endsWith(".$extension", ignoreCase = true)) {
                cleanBaseName
            } else {
                "$cleanBaseName.$extension"
            }

            val mimeType = resolveMimeType(extension, isAudioOnly)
            val safeSubfolder = subfolder.replace(Regex("[^a-zA-Z0-9_\\- ]"), "").trim()
            val relativeFolder = if (isAudioOnly) {
                if (safeSubfolder.isEmpty()) Environment.DIRECTORY_MUSIC
                else "${Environment.DIRECTORY_MUSIC}/$safeSubfolder"
            } else {
                if (safeSubfolder.isEmpty()) Environment.DIRECTORY_MOVIES
                else "${Environment.DIRECTORY_MOVIES}/$safeSubfolder"
            }

            val collectionUri = if (isAudioOnly) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, finalFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.SIZE, sourceFile.length())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, relativeFolder)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val insertedUri = resolver.insert(collectionUri, values)
                ?: throw IllegalStateException("Failed to create MediaStore entry in $relativeFolder")

            try {
                resolver.openOutputStream(insertedUri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream, bufferSize = 64 * 1024)
                    }
                } ?: throw IllegalStateException("Could not open MediaStore output stream.")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val completeValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    resolver.update(insertedUri, completeValues, null, null)
                }
            } catch (e: Exception) {
                runCatching { resolver.delete(insertedUri, null, null) }
                throw e
            }

            val finalSize = sourceFile.length()
            runCatching { sourceFile.delete() }

            PublishedMediaResult(
                uri = insertedUri,
                displayName = finalFileName,
                sizeBytes = finalSize
            )
        }
    }

    fun deleteFromMediaStore(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return runCatching {
            val uri = Uri.parse(uriString)
            context.contentResolver.delete(uri, null, null) > 0
        }.getOrDefault(false)
    }

    fun buildPlayIntent(uriString: String, isAudioOnly: Boolean): Intent {
        val uri = Uri.parse(uriString)
        val mime = if (isAudioOnly) "audio/*" else "video/*"
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildShareIntent(uriString: String, title: String, isAudioOnly: Boolean): Intent {
        val uri = Uri.parse(uriString)
        val mime = if (isAudioOnly) "audio/*" else "video/*"
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(sendIntent, "Share $title").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    private fun resolveMimeType(ext: String, isAudioOnly: Boolean): String {
        return when (ext.lowercase()) {
            "mp4", "m4v" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> if (isAudioOnly) "audio/webm" else "video/webm"
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "opus", "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            else -> if (isAudioOnly) "audio/mp4" else "video/mp4"
        }
    }
}
