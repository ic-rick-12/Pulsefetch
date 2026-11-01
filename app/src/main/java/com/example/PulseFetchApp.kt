package com.example

import android.app.Application
import com.example.domain.extractor.YtDlpExtractorEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PulseFetchApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Pre-initialize youtubedl-android and FFmpeg asynchronously on app startup
        appScope.launch {
            YtDlpExtractorEngine(this@PulseFetchApp).ensureInitialized()
        }
    }
}
