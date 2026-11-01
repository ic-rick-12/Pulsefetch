package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DownloadTaskEntity
import com.example.data.local.PulseFetchDatabase
import com.example.data.model.AccentPreset
import com.example.data.model.AppearanceMode
import com.example.data.model.BackgroundStyle
import com.example.data.model.ColorHexUtils
import com.example.data.model.DefaultQuality
import com.example.data.model.DetectedMediaItem
import com.example.data.model.ExtractedFormat
import com.example.data.model.ExtractedMediaInfo
import com.example.data.model.GlassIntensity
import com.example.data.model.SearchEngine
import com.example.data.model.ThemePreference
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DownloadRepository
import com.example.domain.extractor.ExtractorUpdateResult
import com.example.domain.extractor.YtDlpExtractorEngine
import com.example.domain.storage.MediaStorePublisher
import com.example.util.FormatUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ExtractionUiState {
    data object Idle : ExtractionUiState()
    data class Analyzing(val url: String) : ExtractionUiState()
    data class Ready(
        val mediaInfo: ExtractedMediaInfo,
        val selectedFormat: ExtractedFormat
    ) : ExtractionUiState()
    data class ProtectedBlocked(val url: String, val message: String) : ExtractionUiState()
    data class Error(val url: String, val message: String) : ExtractionUiState()
}

sealed class ExtractorUpdateUiState {
    data object Idle : ExtractorUpdateUiState()
    data object Updating : ExtractorUpdateUiState()
    data class Completed(val message: String, val isError: Boolean = false) : ExtractorUpdateUiState()
}

class PulseFetchViewModel(application: Application) : AndroidViewModel(application) {

    private val database = PulseFetchDatabase.getInstance(application)
    val prefsRepo = UserPreferencesRepository(application)
    val ytDlpEngine = YtDlpExtractorEngine(application)
    val mediaStorePublisher = MediaStorePublisher(application)
    val downloadRepository = DownloadRepository(
        context = application,
        dao = database.downloadTaskDao(),
        prefsRepo = prefsRepo,
        ytDlpEngine = ytDlpEngine,
        mediaStorePublisher = mediaStorePublisher
    )

    val preferencesState: StateFlow<UserPreferences> = prefsRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences(hasAcceptedLegalNotice = true)
    )

    private val _hasLoadedPrefs = MutableStateFlow(false)
    val hasLoadedPrefs: StateFlow<Boolean> = _hasLoadedPrefs.asStateFlow()

    val queueTasks: StateFlow<List<DownloadTaskEntity>> = downloadRepository.queueTasksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val historyTasks: StateFlow<List<DownloadTaskEntity>> = downloadRepository.historyTasksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _clipboardCandidateUrl = MutableStateFlow<String?>(null)
    val clipboardCandidateUrl: StateFlow<String?> = _clipboardCandidateUrl.asStateFlow()

    private val _extractionState = MutableStateFlow<ExtractionUiState>(ExtractionUiState.Idle)
    val extractionState: StateFlow<ExtractionUiState> = _extractionState.asStateFlow()

    private val _extractorUpdateState = MutableStateFlow<ExtractorUpdateUiState>(ExtractorUpdateUiState.Idle)
    val extractorUpdateState: StateFlow<ExtractorUpdateUiState> = _extractorUpdateState.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    private var lastDismissedClipboardUrl: String? = null

    init {
        viewModelScope.launch {
            prefsRepo.getSnapshot()
            _hasLoadedPrefs.value = true
            val ver = ytDlpEngine.getVersionString()
            if (ver.isNotBlank()) {
                prefsRepo.setYtDlpVersion(ver)
            }
        }
    }

    fun updateUrlInput(value: String) {
        _urlInput.value = value
    }

    fun clearUrlInput() {
        _urlInput.value = ""
        _extractionState.value = ExtractionUiState.Idle
    }

    fun onClipboardTextInspected(rawClipText: String?) {
        val detectedUrl = FormatUtils.extractFirstUrl(rawClipText)
        if (detectedUrl != null &&
            detectedUrl != lastDismissedClipboardUrl &&
            detectedUrl != _urlInput.value.trim()
        ) {
            _clipboardCandidateUrl.value = detectedUrl
        } else if (detectedUrl == null) {
            _clipboardCandidateUrl.value = null
        }
    }

    fun dismissClipboardSuggestion() {
        lastDismissedClipboardUrl = _clipboardCandidateUrl.value
        _clipboardCandidateUrl.value = null
    }

    fun applyClipboardUrlAndAnalyze() {
        val url = _clipboardCandidateUrl.value ?: return
        _clipboardCandidateUrl.value = null
        _urlInput.value = url
        analyzeUrl(url)
    }

    fun handleIncomingSharedText(sharedText: String?) {
        val url = FormatUtils.extractFirstUrl(sharedText)
        if (url != null) {
            _urlInput.value = url
            analyzeUrl(url)
        } else if (!sharedText.isNullOrBlank()) {
            _snackbarEvents.tryEmit("No valid web link found in shared text.")
        }
    }

    fun analyzeUrl(overrideUrl: String? = null) {
        val raw = (overrideUrl ?: _urlInput.value).trim()
        val url = FormatUtils.extractFirstUrl(raw) ?: raw
        if (url.isBlank() || (!url.startsWith("http://", true) && !url.startsWith("https://", true))) {
            _extractionState.value = ExtractionUiState.Error(
                url = raw,
                message = "Please enter a valid http:// or https:// video or audio link."
            )
            return
        }

        _urlInput.value = url
        _extractionState.value = ExtractionUiState.Analyzing(url)

        viewModelScope.launch {
            val result = ytDlpEngine.extractMediaInfo(url = url, cookiesHeader = null)
            result.fold(
                onSuccess = { info ->
                    if (info.isProtected) {
                        _extractionState.value = ExtractionUiState.ProtectedBlocked(
                            url = url,
                            message = info.protectionReason ?: "This video is protected and can't be downloaded"
                        )
                    } else {
                        val preferred = selectDefaultFormat(info, preferencesState.value.defaultQuality)
                        _extractionState.value = ExtractionUiState.Ready(
                            mediaInfo = info,
                            selectedFormat = preferred
                        )
                    }
                },
                onFailure = { err ->
                    _extractionState.value = ExtractionUiState.Error(
                        url = url,
                        message = err.localizedMessage ?: "Unsupported link or network failure."
                    )
                }
            )
        }
    }

    fun selectFormat(format: ExtractedFormat) {
        val current = _extractionState.value
        if (current is ExtractionUiState.Ready) {
            _extractionState.value = current.copy(selectedFormat = format)
        }
    }

    fun startSelectedDownload(onQueuedNavigateToQueue: () -> Unit) {
        val current = _extractionState.value as? ExtractionUiState.Ready ?: return
        viewModelScope.launch {
            downloadRepository.enqueueExtractedFormat(
                mediaInfo = current.mediaInfo,
                format = current.selectedFormat
            )
            _snackbarEvents.tryEmit("Added \"${current.mediaInfo.title.take(36)}\" to download queue")
            onQueuedNavigateToQueue()
        }
    }

    fun enqueueBrowserDetectedItem(item: DetectedMediaItem, onQueued: () -> Unit = {}) {
        if (item.isProtected) {
            _snackbarEvents.tryEmit("This video is protected and can't be downloaded")
            return
        }
        viewModelScope.launch {
            downloadRepository.enqueueBrowserStream(item)
            _snackbarEvents.tryEmit("Added \"${item.title.take(36)}\" to download queue")
            onQueued()
        }
    }

    fun pauseDownload(taskId: String) {
        viewModelScope.launch { downloadRepository.pauseTask(taskId) }
    }

    fun resumeDownload(taskId: String) {
        viewModelScope.launch { downloadRepository.resumeTask(taskId) }
    }

    fun retryDownload(taskId: String) {
        viewModelScope.launch { downloadRepository.retryTask(taskId) }
    }

    fun cancelDownload(taskId: String) {
        viewModelScope.launch { downloadRepository.cancelTask(taskId) }
    }

    fun deleteTask(task: DownloadTaskEntity, deleteFileFromDevice: Boolean) {
        viewModelScope.launch {
            downloadRepository.removeTask(task, deleteFileFromDevice)
            _snackbarEvents.tryEmit(
                if (deleteFileFromDevice) "Deleted from history and MediaStore"
                else "Removed from history"
            )
        }
    }

    fun clearFinishedQueueItems() {
        viewModelScope.launch {
            downloadRepository.clearFailedAndCancelled()
        }
    }

    fun acceptLegalNotice() {
        viewModelScope.launch {
            prefsRepo.acceptLegalNotice()
        }
    }

    fun setDefaultQuality(quality: DefaultQuality) {
        viewModelScope.launch {
            prefsRepo.setDefaultQuality(quality)
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.setWifiOnly(enabled)
        }
    }

    fun setCustomSubfolder(subfolder: String) {
        viewModelScope.launch {
            prefsRepo.setCustomSubfolder(subfolder)
            _snackbarEvents.tryEmit("Download folder updated to $subfolder")
        }
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        viewModelScope.launch {
            prefsRepo.setAppearanceMode(mode)
        }
    }

    fun setAccentPreset(preset: AccentPreset) {
        viewModelScope.launch {
            prefsRepo.setAccentPreset(preset)
        }
    }

    fun setCustomAccentHex(hexInput: String) {
        val valid = ColorHexUtils.normalizeHexOrNull(hexInput)
        if (valid == null) {
            _snackbarEvents.tryEmit("Invalid HEX color. Use 6-digit format like #00E5FF")
            return
        }
        viewModelScope.launch {
            prefsRepo.setCustomAccentHex(valid)
            _snackbarEvents.tryEmit("Applied custom accent $valid")
        }
    }

    fun setGlassIntensity(intensity: GlassIntensity) {
        viewModelScope.launch {
            prefsRepo.setGlassIntensity(intensity)
        }
    }

    fun setBackgroundStyle(style: BackgroundStyle) {
        viewModelScope.launch {
            prefsRepo.setBackgroundStyle(style)
        }
    }

    fun setCustomBackgroundHex(hexInput: String) {
        val valid = ColorHexUtils.normalizeHexOrNull(hexInput)
        if (valid == null) {
            _snackbarEvents.tryEmit("Invalid HEX color. Use 6-digit format like #061018")
            return
        }
        viewModelScope.launch {
            prefsRepo.setCustomBackgroundHex(valid)
            _snackbarEvents.tryEmit("Applied custom background $valid")
        }
    }

    fun setThemePreference(themePreference: ThemePreference) {
        viewModelScope.launch {
            prefsRepo.setThemePreference(themePreference)
        }
    }

    fun setSearchEngine(searchEngine: SearchEngine) {
        viewModelScope.launch {
            prefsRepo.setSearchEngine(searchEngine)
        }
    }

    fun resetAppearanceSettings() {
        viewModelScope.launch {
            prefsRepo.resetAppearanceSettings()
            _snackbarEvents.tryEmit("Appearance reset to AMOLED White/Silver defaults")
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            prefsRepo.resetAllSettings()
            _snackbarEvents.tryEmit("All settings restored to factory defaults")
        }
    }

    fun restartAppSession(onRestartComplete: () -> Unit = {}) {
        _urlInput.value = ""
        _clipboardCandidateUrl.value = null
        _extractionState.value = ExtractionUiState.Idle
        _extractorUpdateState.value = ExtractorUpdateUiState.Idle
        viewModelScope.launch {
            ytDlpEngine.ensureInitialized()
            val ver = ytDlpEngine.getVersionString()
            if (ver.isNotBlank()) {
                prefsRepo.setYtDlpVersion(ver)
            }
            onRestartComplete()
            _snackbarEvents.tryEmit("PulseFetch restarted & extractor engine refreshed")
        }
    }

    fun updateYtDlpExtractor(useNightly: Boolean = false) {
        if (_extractorUpdateState.value is ExtractorUpdateUiState.Updating) return
        _extractorUpdateState.value = ExtractorUpdateUiState.Updating
        viewModelScope.launch {
            when (val res = ytDlpEngine.updateExtractor(useNightly)) {
                is ExtractorUpdateResult.Updated -> {
                    prefsRepo.setYtDlpVersion(res.version)
                    _extractorUpdateState.value = ExtractorUpdateUiState.Completed(
                        message = "Updated yt-dlp to ${res.version}",
                        isError = false
                    )
                }
                is ExtractorUpdateResult.AlreadyUpToDate -> {
                    prefsRepo.setYtDlpVersion(res.version)
                    _extractorUpdateState.value = ExtractorUpdateUiState.Completed(
                        message = "Extractor is already up to date (${res.version})",
                        isError = false
                    )
                }
                is ExtractorUpdateResult.Error -> {
                    _extractorUpdateState.value = ExtractorUpdateUiState.Completed(
                        message = res.message,
                        isError = true
                    )
                }
            }
        }
    }

    fun clearBrowsingData(onCleared: () -> Unit = {}) {
        onCleared()
        _snackbarEvents.tryEmit("Cleared all browser session data, cache, and site history")
    }

    private fun selectDefaultFormat(
        info: ExtractedMediaInfo,
        defaultQuality: DefaultQuality
    ): ExtractedFormat {
        return when (defaultQuality) {
            DefaultQuality.AUDIO_MP3 -> {
                info.audioFormats.firstOrNull { it.ext.equals("mp3", ignoreCase = true) }
                    ?: info.audioFormats.first()
            }
            DefaultQuality.AUDIO_M4A -> {
                info.audioFormats.firstOrNull { it.ext.equals("m4a", ignoreCase = true) }
                    ?: info.audioFormats.first()
            }
            DefaultQuality.BEST -> info.videoFormats.firstOrNull() ?: info.audioFormats.first()
            DefaultQuality.P1080 -> {
                info.videoFormats.firstOrNull { it.height in 900..1150 }
                    ?: info.videoFormats.firstOrNull { it.height <= 1080 }
                    ?: info.videoFormats.first()
            }
            DefaultQuality.P720 -> {
                info.videoFormats.firstOrNull { it.height in 650..780 }
                    ?: info.videoFormats.firstOrNull { it.height <= 720 }
                    ?: info.videoFormats.first()
            }
            DefaultQuality.P480 -> {
                info.videoFormats.firstOrNull { it.height in 400..540 }
                    ?: info.videoFormats.firstOrNull { it.height <= 480 }
                    ?: info.videoFormats.last()
            }
        }
    }
}
