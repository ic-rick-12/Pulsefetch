package com.example

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.LegalNoticeDialog
import com.example.ui.components.PulseFetchRootBackdrop
import com.example.ui.components.PulseFetchSplashScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QueueScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalPulseFetchTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PulseFetchViewModel
import kotlinx.coroutines.flow.collectLatest

enum class MainDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    EXTRACTOR("extractor", "Extractor", Icons.Filled.Link, Icons.Outlined.Link),
    QUEUE("queue", "Queue", Icons.Filled.CloudDownload, Icons.Outlined.CloudDownload),
    HISTORY("history", "History", Icons.Filled.History, Icons.Outlined.History),
    SETTINGS("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {

    private val pulseViewModel: PulseFetchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        consumeShareIntentIfNeeded(intent)

        setContent {
            PulseFetchRoot(
                pulseViewModel = pulseViewModel
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeShareIntentIfNeeded(intent)
    }

    private fun consumeShareIntentIfNeeded(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                pulseViewModel.handleIncomingSharedText(sharedText)
                intent.removeExtra(Intent.EXTRA_TEXT)
            }
        }
    }
}

@Composable
fun PulseFetchRoot(
    pulseViewModel: PulseFetchViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val preferences by pulseViewModel.preferencesState.collectAsStateWithLifecycle()
    val hasLoadedPrefs by pulseViewModel.hasLoadedPrefs.collectAsStateWithLifecycle()
    val urlInput by pulseViewModel.urlInput.collectAsStateWithLifecycle()
    val clipboardUrl by pulseViewModel.clipboardCandidateUrl.collectAsStateWithLifecycle()
    val extractionState by pulseViewModel.extractionState.collectAsStateWithLifecycle()
    val queueTasks by pulseViewModel.queueTasks.collectAsStateWithLifecycle()
    val historyTasks by pulseViewModel.historyTasks.collectAsStateWithLifecycle()
    val extractorUpdateState by pulseViewModel.extractorUpdateState.collectAsStateWithLifecycle()

    var currentDestination by rememberSaveable { mutableStateOf(MainDestination.EXTRACTOR) }
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Foreground worker continues regardless; notification shown if granted */ }

    val requestNotificationPermissionIfNeeded = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Inspect clipboard when app resumes to auto-detect copied links
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                runCatching {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clipText = cm?.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()
                    pulseViewModel.onClipboardTextInspected(clipText)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        pulseViewModel.snackbarEvents.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    MyApplicationTheme(preferences = preferences) {
        val themeConfig = LocalPulseFetchTheme.current

        // First-launch responsibility & legal notice dialog
        if (hasLoadedPrefs && !preferences.hasAcceptedLegalNotice && !showSplash) {
            LegalNoticeDialog(
                onAccept = { pulseViewModel.acceptLegalNotice() }
            )
        }

        PulseFetchRootBackdrop(
            modifier = Modifier.fillMaxSize()
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                bottomBar = {
                    AdaptiveBottomNavigationBar(
                        currentDestination = currentDestination,
                        queueCount = queueTasks.size,
                        historyCount = historyTasks.size,
                        isLiquidGlass = themeConfig.isLiquidGlass,
                        surfaceAlpha = themeConfig.glassIntensity.surfaceAlpha,
                        borderAlpha = themeConfig.glassIntensity.borderHighlightAlpha,
                        isDarkTheme = !themeConfig.isLightTheme,
                        onSelectDestination = { dest -> currentDestination = dest }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AnimatedContent(
                        targetState = currentDestination,
                        transitionSpec = {
                            (fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                scaleIn(initialScale = 0.985f, animationSpec = tween(220, easing = FastOutSlowInEasing)))
                                .togetherWith(fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing)))
                        },
                        label = "main_nav_transition"
                    ) { destination ->
                        when (destination) {
                            MainDestination.EXTRACTOR -> {
                                HomeScreen(
                                    urlInput = urlInput,
                                    clipboardCandidateUrl = clipboardUrl,
                                    extractionState = extractionState,
                                    ytDlpVersion = preferences.ytDlpVersion,
                                    onUrlChange = pulseViewModel::updateUrlInput,
                                    onClearUrl = pulseViewModel::clearUrlInput,
                                    onAnalyzeClick = { pulseViewModel.analyzeUrl() },
                                    onApplyClipboardUrl = pulseViewModel::applyClipboardUrlAndAnalyze,
                                    onDismissClipboardUrl = pulseViewModel::dismissClipboardSuggestion,
                                    onSelectFormat = pulseViewModel::selectFormat,
                                    onStartDownload = {
                                        requestNotificationPermissionIfNeeded()
                                        pulseViewModel.startSelectedDownload {
                                            currentDestination = MainDestination.QUEUE
                                        }
                                    },
                                    onUpdateExtractorClick = {
                                        currentDestination = MainDestination.SETTINGS
                                        pulseViewModel.updateYtDlpExtractor(false)
                                    }
                                )
                            }

                            MainDestination.QUEUE -> {
                                QueueScreen(
                                    tasks = queueTasks,
                                    onPause = pulseViewModel::pauseDownload,
                                    onResume = pulseViewModel::resumeDownload,
                                    onCancel = pulseViewModel::cancelDownload,
                                    onRetry = pulseViewModel::retryDownload,
                                    onRemove = { task -> pulseViewModel.deleteTask(task, false) },
                                    onClearFailedAndCancelled = pulseViewModel::clearFinishedQueueItems,
                                    onBackToExtractor = { currentDestination = MainDestination.EXTRACTOR }
                                )
                            }

                            MainDestination.HISTORY -> {
                                HistoryScreen(
                                    historyTasks = historyTasks,
                                    mediaStorePublisher = pulseViewModel.mediaStorePublisher,
                                    customSubfolder = preferences.customSubfolder,
                                    onDeleteTask = pulseViewModel::deleteTask,
                                    onBackToExtractor = { currentDestination = MainDestination.EXTRACTOR }
                                )
                            }

                            MainDestination.SETTINGS -> {
                                SettingsScreen(
                                    preferences = preferences,
                                    extractorUpdateState = extractorUpdateState,
                                    onSelectDefaultQuality = pulseViewModel::setDefaultQuality,
                                    onToggleWifiOnly = pulseViewModel::setWifiOnly,
                                    onSaveSubfolder = pulseViewModel::setCustomSubfolder,
                                    onUpdateExtractor = pulseViewModel::updateYtDlpExtractor,
                                    onSelectAppearanceMode = pulseViewModel::setAppearanceMode,
                                    onSelectGlassIntensity = pulseViewModel::setGlassIntensity,
                                    onSelectAccentPreset = pulseViewModel::setAccentPreset,
                                    onApplyCustomAccentHex = pulseViewModel::setCustomAccentHex,
                                    onSelectBackgroundStyle = pulseViewModel::setBackgroundStyle,
                                    onApplyCustomBackgroundHex = pulseViewModel::setCustomBackgroundHex,
                                    onSelectThemePreference = pulseViewModel::setThemePreference,
                                    onResetAppearance = pulseViewModel::resetAppearanceSettings,
                                    onResetAllSettings = pulseViewModel::resetAllSettings,
                                    onRestartApp = {
                                        pulseViewModel.restartAppSession {
                                            currentDestination = MainDestination.EXTRACTOR
                                            showSplash = true
                                        }
                                    },
                                    onBackToExtractor = { currentDestination = MainDestination.EXTRACTOR }
                                )
                            }
                        }
                    }
                }
            }

            // Official PulseFetch Splash Screen with Rick branding
            AnimatedVisibility(
                visible = showSplash,
                enter = fadeIn(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(360))
            ) {
                PulseFetchSplashScreen(
                    onFinished = { showSplash = false }
                )
            }
        }
    }
}

@Composable
private fun AdaptiveBottomNavigationBar(
    currentDestination: MainDestination,
    queueCount: Int,
    historyCount: Int,
    isLiquidGlass: Boolean,
    surfaceAlpha: Float,
    borderAlpha: Float,
    isDarkTheme: Boolean,
    onSelectDestination: (MainDestination) -> Unit
) {
    if (isLiquidGlass) {
        val glassShape = RoundedCornerShape(26.dp)
        val glassContainer = if (isDarkTheme) {
            MaterialTheme.colorScheme.surface.copy(alpha = (surfaceAlpha + 0.14f).coerceAtMost(0.92f))
        } else {
            Color.White.copy(alpha = (surfaceAlpha + 0.14f).coerceAtMost(0.94f))
        }
        val specularBorder = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = borderAlpha),
                MaterialTheme.colorScheme.primary.copy(alpha = borderAlpha * 0.65f),
                Color.White.copy(alpha = borderAlpha * 0.25f)
            )
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .clip(glassShape),
            shape = glassShape,
            color = glassContainer,
            border = BorderStroke(1.dp, specularBorder),
            tonalElevation = 0.dp
        ) {
            NavigationBarContent(
                currentDestination = currentDestination,
                queueCount = queueCount,
                historyCount = historyCount,
                containerColor = Color.Transparent,
                onSelectDestination = onSelectDestination
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
            )
            NavigationBarContent(
                currentDestination = currentDestination,
                queueCount = queueCount,
                historyCount = historyCount,
                containerColor = MaterialTheme.colorScheme.background,
                onSelectDestination = onSelectDestination
            )
        }
    }
}

@Composable
private fun NavigationBarContent(
    currentDestination: MainDestination,
    queueCount: Int,
    historyCount: Int,
    containerColor: Color,
    onSelectDestination: (MainDestination) -> Unit
) {
    NavigationBar(
        containerColor = containerColor,
        tonalElevation = 0.dp
    ) {
        MainDestination.entries.forEach { dest ->
            val isSelected = currentDestination == dest
            val badgeCount = when (dest) {
                MainDestination.QUEUE -> queueCount
                MainDestination.HISTORY -> historyCount
                else -> 0
            }

            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectDestination(dest) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (badgeCount > 0) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ) {
                                    Text(
                                        text = badgeCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSelected) dest.selectedIcon else dest.unselectedIcon,
                            contentDescription = dest.label
                        )
                    }
                },
                label = {
                    Text(
                        text = dest.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("nav_tab_${dest.route}")
            )
        }
    }
}
