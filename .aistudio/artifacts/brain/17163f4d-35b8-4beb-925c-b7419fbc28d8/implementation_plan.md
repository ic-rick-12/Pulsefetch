# PulseFetch — Personal Media Extractor & Browser Downloader

PulseFetch is a privacy-first, true-black AMOLED video and audio downloader built for Android 17 (vivo X200). It combines instant link extraction (`youtubedl-android` / `yt-dlp` + bundled `FFmpeg`), a multi-tab built-in browser with dual page-level and network-level media stream detection, a WorkManager foreground download queue with range-request pause/resume, and zero-permission MediaStore saving directly to `Movies/` and `Music/`.

## User Review & Critical Decisions

> [!IMPORTANT]
> Your Phase 1 preferences have been locked into the specification below. Review the architecture and design choices and click **Proceed** to start building.

- **Confirmed Decision 1 (Unified Release Scope)**: We will build the complete application—including the paste/share link extractor, download queue, media history, extractor updater, and the multi-tab built-in browser with real-time stream detection—together in v1.
- **Confirmed Decision 2 (Browser Search Engine)**: The built-in browser's smart address bar will route search queries through **DuckDuckGo** (`https://duckduckgo.com/?q=...`) for privacy-respecting browsing.
- **Confirmed Decision 3 (AMOLED Visual Identity)**: The UI will use a **True Black (`#000000`) AMOLED** surface palette paired with **Electric Cyan (`#00E5FF`)** and **Deep Teal (`#00BFA5`)** accents for high-contrast legibility and minimal battery draw on the vivo X200 display.

---

## 1. Overview & Core Concept

- **What It Does**:
  - Lets you paste a link, auto-detects copied URLs from the clipboard on focus, or receives shared URLs directly from YouTube, Instagram, Chrome, and other apps via the Android Share Sheet (`ACTION_SEND`).
  - Queries `youtubedl-android` (`yt-dlp`) to display the video thumbnail, title, duration, uploader, and selectable formats (video resolutions from 360p up to 4K, or audio-only MP3/M4A).
  - Provides a built-in multi-tab WebView browser (supporting 3+ standard tabs plus Private Tabs) that detects downloadable media via both **Page-Level Extraction** (`yt-dlp` metadata probe) and **Network-Level Interception** (`shouldInterceptRequest` inspecting `.mp4`, `.webm`, `.m3u8`, `.mpd`, `video/*`, and `audio/*` while filtering out tiny tracking pixels).
  - Enforces strict compliance: detects DRM-protected streams (Widevine, encrypted `EXT-X-KEY`, PlayReady), paywalls, or unauthenticated private videos and stops immediately with a clear explanation rather than attempting any bypass.
  - Executes downloads reliably in the background via **WorkManager** promoted to a ` DATA_SYNC` **Foreground Service** with live notification progress, supporting pause, resume, cancel, and retry across both `yt-dlp`/`FFmpeg` jobs and direct OkHttp range-request downloads.
  - Saves completed videos to `Movies/PulseFetch` and audio tracks to `Music/PulseFetch` using Android `MediaStore` without requesting legacy storage permissions.
- **Target Audience / Persona**: A power user on a modern flagship Android device (vivo X200, Android 17) who wants a fast, ad-free, tracker-free personal media archiver with full control over extractor updates and format selection.
- **Key Value**: Combines the extraction power of `yt-dlp` + `FFmpeg` (with one-tap on-device extractor updates) and network stream sniffing into a single cohesive Material 3 AMOLED interface.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **First-Launch Legal & Responsibility Notice**:
   - On initial launch, a non-dismissible modal dialog presents the required notice: *"Only download content you own, have permission to download, or that is public domain / Creative Commons. You are responsible for following each site's terms and local law."*
   - Once acknowledged, the acceptance state is persisted in DataStore Preferences so it only appears once (and remains accessible in Settings).
2. **Home / Link Extractor Flow**:
   - **Auto-Paste & Share Target**: Opening the app checks the clipboard when the user taps the quick-paste banner, or automatically populates the URL input when launched from the system Share Sheet.
   - **Format Picker Sheet**: After tapping **Analyze Link**, a shimmer loading card transitions into a rich media preview card showing the 16:9 thumbnail, duration badge, channel/uploader, and segmented tabs for **Video Formats** (with resolution, codec, FPS, and estimated file size) and **Audio Only** (MP3 / M4A bitrates).
   - **DRM / Private Video Guard**: If `yt-dlp` reports DRM protection, login requirement, or a private video, an amber/crimson diagnostic banner clearly explains why the media cannot be downloaded.
3. **Built-In Browser & Stream Sniffer Flow**:
   - **Top Bar & Tab Switcher**: Features a smart URL/DuckDuckGo search bar, SSL security indicator, back/forward/refresh controls, a tab counter pill (pre-initialized with 3 ready tabs and support for adding standard or **Private Tabs**), and a page load progress bar.
   - **Dual Media Detection & Floating Action Badge**:
     - As pages load, network requests are inspected for media MIME types/extensions (`> 256 KB` threshold to ignore tracking pixels) and the page URL is probed asynchronously with `yt-dlp`.
     - A glowing Electric Cyan floating action button displays the live count of detected videos on the current page.
   - **Detected Media Bottom Sheet**: Tapping the floating badge opens a bottom sheet listing each detected stream (distinguishing between `yt-dlp` page extractions, HLS `.m3u8` / DASH `.mpd` manifests merged via `FFmpeg`, `blob:` URLs automatically routed through page-level `yt-dlp`, and direct `.mp4`/`.webm` files downloaded via OkHttp range requests).
4. **Download Queue & History Flow**:
   - **Queue Screen**: Displays active, paused, queued, and failed tasks with animated progress bars, download speed, ETA, and inline **Pause / Resume**, **Cancel**, and **Retry** buttons.
   - **History Screen**: Lists completed downloads stored in `MediaStore` with thumbnail previews, file size, format badge (`MP4`, `MKV`, `MP3`, `M4A`), and one-tap actions to **Play** (via system media intent), **Share** (`Intent.ACTION_SEND` with content URI), or **Delete** (removing both the history record and `MediaStore` entry).
5. **Settings & Extractor Maintenance Flow**:
   - **Update Extractor Button**: Runs `YoutubeDL.getInstance().updateYoutubeDL(...)` on a background coroutine with a live status indicator showing the current `yt-dlp` version and update channel (Stable / Nightly).
   - **Download Preferences**: Configure default preferred quality (`Best Available`, `1080p`, `720p`, `480p`, `Audio Only`), **Wi-Fi Only Downloads** constraint (enforced by WorkManager `NetworkType.UNMETERED`), custom subfolder name under `Movies/` and `Music/`, and a **Clear Browsing Data** action that wipes WebView cookies, cache, storage, and non-private browsing history.

### Visual Identity & Theme

- **Aesthetic Direction**: Precision Utilitarian AMOLED Dark — engineered for OLED power efficiency, zero eye strain in dark environments, and high-density technical telemetry (codec chips, bitrate readouts, live byte-rate progress).
- **Color Palette & Mood**:
  - **Background (`background` / `surface`)**: Pure OLED Black `#000000`
  - **Elevated Cards (`surfaceContainer` / `surfaceVariant`)**: Carbon Obsidian `#0D1117` with subtle `#1E293B` 1dp borders
  - **Primary Accent (`primary`)**: Electric Cyan `#00E5FF` (onPrimary `#00262B`)
  - **Secondary Accent (`secondary`)**: Deep Teal `#00BFA5`
  - **Tertiary / Stream Badge (`tertiary`)**: Neon Mint `#64FFDA`
  - **Error / DRM Blocked (`error`)**: Vivid Coral Red `#FF5252`
- **Typography & Hierarchy**:
  - **Display & Headings**: `Space Grotesk` (bundled locally in `res/font/` via `font-util`) for sharp, technical headers and tab titles.
  - **Body & UI Controls**: `Plus Jakarta Sans` for clean, legible titles and descriptions.
  - **Technical Telemetry (Sizes, Speeds, Codecs, URLs)**: `JetBrains Mono` for tabular numeric alignment on download speeds, durations, and format tags.
- **Interactive Feedback & Motion**:
  - Spring-animated badge scaling when new media streams are detected in the browser.
  - Smooth `AnimatedVisibility` transitions between URL input, extraction progress, and format selection cards.
  - Full edge-to-edge layout (`enableEdgeToEdge()`) with proper `WindowInsets.statusBars` and `WindowInsets.navigationBars` handling so nothing clips under the vivo X200 punch-hole camera or gesture bar.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Dual-Engine Download Pipeline (`youtubedl-android` + `FFmpeg` vs. `OkHttp` Range Downloader)**
  - *Chosen Approach*: Route site extractions, `blob:` fallbacks, and adaptive manifests (`.m3u8` HLS / `.mpd` DASH) through `youtubedl-android` with bundled `FFmpeg` (`--continue` flag enabled for resumable partials), while routing direct single-file URLs (`.mp4`, `.webm`, `.m4a`, `.mp3`) captured by the browser network interceptor through an `OkHttp` client using `Range: bytes=<offset>-` headers.
  - *Why*: `yt-dlp` + `FFmpeg` is essential for muxing separate video/audio streams and adaptive HLS/DASH segments, whereas `OkHttp` range requests offer instant, low-overhead byte-level pause/resume for direct media files discovered in the browser.
  - *Alternatives Considered*: Using Android's legacy `DownloadManager` was rejected because it cannot mux HLS/DASH streams, cannot run `yt-dlp` extractors, and offers limited control over custom headers/cookies.
- **Decision 2: Scoped Storage via `MediaStore` (Zero Storage Permissions)**
  - *Chosen Approach*: Download active files into an app-private staging directory (`context.externalCacheDir/downloads/`) so `yt-dlp`, `FFmpeg`, and `OkHttp` can freely seek, append, and mux partial files, then atomically publish the finished file into `MediaStore.Video.Media` (`Movies/PulseFetch`) or `MediaStore.Audio.Media` (`Music/PulseFetch`) using `IS_PENDING = 1 -> 0`.
  - *Why*: Complies with Android 10–17 Scoped Storage rules without requiring `MANAGE_EXTERNAL_STORAGE` or `WRITE_EXTERNAL_STORAGE`, while still allowing `FFmpeg` direct filesystem access during muxing.
- **Decision 3: Strict DRM & Access Control Compliance**
  - *Chosen Approach*: Inspect both `yt-dlp` error output (e.g., `DRM protected`, `Private video`, `Sign in to confirm`, `members-only`) and HLS/DASH manifests (checking for `SAMPLE-AES`, `KEYFORMAT="urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed"` Widevine, or `<ContentProtection>` tags in `.mpd`) prior to queuing.
  - *Why*: Ensures the app strictly honors the rule never to bypass DRM, paywalls, or unauthenticated logins, giving the user an immediate, transparent explanation instead of a cryptic failed download.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        UI LAYER (Jetpack Compose + M3)                       │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐  ┌────────────┐  │
│  │  HomeScreen    │  │ BrowserScreen  │  │ Queue & History│  │  Settings  │  │
│  │ (Paste/Share + │  │ (Multi-Tab     │  │   Screens      │  │   Screen   │  │
│  │ Format Picker) │  │ WebView + FAB) │  │ (Pause/Resume/ │  │ (Extractor │  │
│  └───────┬────────┘  └───────┬────────┘  │  Play/Share)   │  │  Updater)  │  │
│          │                   │           └───────┬────────┘  └─────┬──────┘  │
└──────────┼───────────────────┼───────────────────┼─────────────────┼─────────┘
           │                   │                   │                 │
           ▼                   ▼                   ▼                 ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                     VIEWMODEL & STATE MANAGEMENT LAYER                       │
│  ┌─────────────────────────┐  ┌──────────────────────┐  ┌─────────────────┐  │
│  │   DownloaderViewModel   │  │   BrowserViewModel   │  │SettingsViewModel│  │
│  │ (Extraction, Queue Ops, │  │ (3+ Tabs, Private    │  │ (yt-dlp Update, │  │
│  │  History, Share Intent) │  │  Mode, Sniffed List) │  │  Prefs, Clear)  │  │
│  └────────────┬────────────┘  └──────────┬───────────┘  └────────┬────────┘  │
└───────────────┼──────────────────────────┼───────────────────────┼───────────┘
                │                          │                       │
                ▼                          ▼                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                      DOMAIN & EXECUTION ENGINE LAYER                         │
│  ┌─────────────────────────┐  ┌──────────────────────┐  ┌─────────────────┐  │
│  │   YtDlpExtractorEngine  │  │ WebMediaSnifferEngine│  │ WorkManager +   │  │
│  │ (VideoInfo Probe, DRM   │  │ (shouldInterceptReq, │  │ Foreground      │  │
│  │  Check, Format Mapping, │  │  Content-Type/Size   │  │ DownloadWorker  │  │
│  │  Binary Self-Update)    │  │  Filter, Blob Route) │  │ (yt-dlp/OkHttp) │  │
│  └────────────┬────────────┘  └──────────┬───────────┘  └────────┬────────┘  │
└───────────────┼──────────────────────────┼───────────────────────┼───────────┘
                │                          │                       │
                ▼                          ▼                       ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│                        PERSISTENCE & SYSTEM SERVICES                         │
│  ┌─────────────────────────┐  ┌──────────────────────┐  ┌─────────────────┐  │
│  │     Room Database       │  │ DataStore Preferences│  │   MediaStore    │  │
│  │ (DownloadTaskEntity:    │  │ (First-Run Notice,   │  │ Publisher       │  │
│  │  Queue & History State) │  │  Default Quality,    │  │ (Movies/ &      │  │
│  │                         │  │  Wi-Fi Only, Folder) │  │  Music/ Export) │  │
│  └─────────────────────────┘  └──────────────────────┘  └─────────────────┘  │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & State

1. **`DownloadTaskEntity` (Room Table `download_tasks`)**:
   - `id`: `String` (UUID primary key, matched with WorkManager request tag)
   - `sourceUrl`: `String` (Original page or direct stream URL)
   - `pageUrl`: `String?` (Originating web page URL for referer/cookies context)
   - `title`: `String`
   - `thumbnailUrl`: `String?`
   - `durationSeconds`: `Long`
   - `extractorType`: `ExtractorType` (`YTDLP_FORMAT`, `YTDLP_HLS_DASH`, `OKHTTP_DIRECT`)
   - `formatId`: `String` (e.g., `bestvideo+bestaudio`, `137+140`, `mp3-192`, or `direct`)
   - `formatLabel`: `String` (e.g., `1080p MP4`, `Audio M4A 128k`, `HLS Stream`)
   - `isAudioOnly`: `Boolean`
   - `status`: `DownloadStatus` (`QUEUED`, `DOWNLOADING`, `PAUSED`, `COMPLETED`, `FAILED`, `CANCELLED`)
   - `progressPercent`: `Int` (`0..100`)
   - `downloadedBytes`: `Long`
   - `totalBytes`: `Long`
   - `speedBytesPerSec`: `Long`
   - `etaSeconds`: `Long`
   - `tempFilePath`: `String?` (Staging path in cache directory for resume support)
   - `mediaStoreUri`: `String?` (Final `content://media/external/...` URI once completed)
   - `errorMessage`: `String?` (Human-readable failure or DRM message)
   - `createdAt`: `Long`
   - `completedAt`: `Long?`
2. **`BrowserTabState` (In-Memory + Session State in `BrowserViewModel`)**:
   - `tabId`: `String`
   - `url`: `String`
   - `title`: `String`
   - `isPrivate`: `Boolean` (Disables cookies, DOM storage, cache, and history saving when `true`)
   - `isLoading`: `Boolean`
   - `progress`: `Int`
   - `detectedMedia`: `List<DetectedMediaItem>` (Combines page-level `yt-dlp` result and network-sniffed `.mp4`/`.webm`/`.m3u8`/`.mpd` streams with deduplication)
3. **`UserPreferences` (Jetpack DataStore)**:
   - `hasAcceptedLegalNotice`: `Boolean`
   - `defaultQuality`: `DefaultQuality` (`BEST`, `P1080`, `P720`, `P480`, `AUDIO_MP3`, `AUDIO_M4A`)
   - `wifiOnly`: `Boolean`
   - `customSubfolder`: `String` (default `"PulseFetch"`)
   - `ytDlpVersion`: `String` (last known version timestamp)

### Interactive Component & State Mapping

- **First-Launch Compliance Gate**: `MainActivity` observes `UserPreferences.hasAcceptedLegalNotice`. Until accepted, `LegalDisclaimerDialog` blocks interaction; clicking **I Understand & Agree** persists the flag in DataStore.
- **Clipboard & Share Intent Handling**:
  - `MainActivity` declares an `<intent-filter>` for `android.intent.action.SEND` (`text/plain`). Shared text is parsed via regex to extract the first `http://` or `https://` URL, switches navigation to the Home tab, populates the URL bar, and immediately triggers `DownloaderViewModel.analyzeUrl(url)`.
  - On the Home screen, a clipboard detection chip inspects `ClipboardManager.primaryClip` when the screen gains focus and offers a one-tap **Paste & Analyze** pill if a valid URL is present.
- **Link Analysis & Format Selection**:
  - `YtDlpExtractorEngine.extractInfo(url)` invokes `YoutubeDL.getInstance().getInfo(request)` on `Dispatchers.IO`.
  - Parses available formats into clean video quality tiers (`2160p`, `1440p`, `1080p`, `720p`, `480p`, `360p`) and audio extraction presets (`MP3 320kbps`, `M4A Best`), automatically pairing video-only DASH formats with best audio via `+bestaudio` so `FFmpeg` merges them seamlessly.
  - Checks for DRM flags (`drm`, `is_drm`) or authentication errors and transitions UI state to `ExtractionState.ProtectedError` or `ExtractionState.NetworkError` with actionable messages.
- **WorkManager Foreground Queue & Pause/Resume**:
  - Enqueuing a download inserts a `DownloadTaskEntity` (`QUEUED`) and schedules a `OneTimeWorkRequestBuilder<MediaDownloadWorker>()` with `Constraints.Builder().setRequiredNetworkType(if (wifiOnly) UNMETERED else CONNECTED)`.
  - `MediaDownloadWorker` calls `setForeground(ForegroundInfo(..., FOREGROUND_SERVICE_TYPE_DATA_SYNC))` and updates a persistent notification with progress and a Cancel action.
  - **Pause**: Cancels the active `YoutubeDL.getInstance().destroyProcessById(taskId)` or closes the `OkHttp` stream while preserving the `.part` / staging file on disk and setting status to `PAUSED`.
  - **Resume**: Re-enqueues the worker with the same `taskId`; `yt-dlp` resumes from the `.part` file via `--continue` and `OkHttp` sends `Range: bytes=<existingLength>-` to append to the staging file.
- **Built-In Browser Dual Detection**:
  - **Network-Level (`shouldInterceptRequest`)**: Inspects request URLs and response `Content-Type` headers (via lightweight `HEAD` probe when ambiguous) for `.mp4`, `.webm`, `.m3u8`, `.mpd`, `video/*`, and `audio/*`. Ignores segments smaller than `256 KB`, `.ts` micro-segments when a parent `.m3u8` is already captured, and tracking pixels. Checks `.m3u8` / `.mpd` manifests for `EXT-X-KEY` encryption or Widevine UUIDs and marks them `isProtected = true`.
  - **Page-Level (`onPageFinished`)**: Debounces page URL changes and runs a background `yt-dlp --dump-json --no-playlist` check. Also provides a JavaScript bridge / blob detector so any `<video src="blob:...">` element on the page automatically falls back to passing the page URL to `yt-dlp`.
  - **Private Tabs & Clear Data**: Private tabs configure a dedicated ephemeral state (`setSaveFormData(false)`, third-party cookies disabled, history recording suppressed, cache cleared on tab close). The Settings **Clear Browsing Data** button invokes `WebStorage.getInstance().deleteAllData()`, `CookieManager.getInstance().removeAllCookies(null)`, and clears cached WebView files.
