# Changelog

All notable changes to **Isai** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.0] - 2026-11-06
### Added
- **Add-on Round 2 Major Release**: Delivers a dynamic Equalizer & Audio Effects Engine, complete Android Auto & Bluetooth car dashboard support via MediaLibraryService, and robust M3U/M3U8 playlist import/export.
- **Equalizer & Audio Effects (Prompt S)**:
  - On-device hardware effects attached to ExoPlayer's `audioSessionId` (`Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer`).
  - Probes device frequency bands and level ranges dynamically without hardcoding.
  - Log-frequency preset resampling across 14 genre/acoustic presets.
  - Interactive Catmull-Rom spline `EqCurveView` Canvas with draggable band nodes and $0\text{ dB}$ haptic ticks.
  - Automatic digital clipping compensation math to prevent inter-sample distortion.
  - DataStore custom preset creation and system equalizer panel intent launcher.
- **Android Auto & Automotive Media (Prompt T)**:
  - Upgraded `PlaybackService` to `MediaLibraryService` (`MediaLibrarySession.Callback`).
  - Structured 6-category browse tree (Recently Played, Favorites, Playlists, Albums, Artists, All Songs with "Shuffle All").
  - Secure `CarArtworkProvider` content provider serving downscaled 512px JPEG artwork with strict path-traversal guards.
  - Package authentication protecting queue controls (`PackageValidator`).
  - Natural Google Assistant voice search query resolution.
  - Dedicated Desktop Head Unit (DHU) guide in `docs/android-auto.md`.
- **M3U Playlist Import & Export (Prompt U)**:
  - Room database schema v4 migration (`relative_path_norm` indexed column and `MIGRATION_3_4`).
  - Comprehensive `PathNormalizer` (Windows drive strip, URL decoding, `/` normalization).
  - High-performance M3U/M3U8 parser supporting `#EXTINF` and `#PLAYLIST` tags.
  - 4-stage track matcher (exact relative path $\to$ suffix $\to$ filename + duration $\to$ tag match).
  - Interactive import preview bottom sheet with conflict resolution.
  - Export writer for playlists, favorites, and queues.
- **Round-2 Quality Assurance Suite (Prompt V)**:
  - `RoomMigrationTest`: Validates SQL DDL migration chain through `MIGRATION_3_4`.
  - `AddOnRound2ContractTest`: Validates strict zero-network contract (`0` internet permissions), automotive declarations, and audio DSP headroom calculations.
  - Real-world fixture compatibility verified against **foobar2000**, **VLC**, **MusicBee**, and **Poweramp**.

## [0.21.0-m3u-playlists] - 2026-11-06
### Added
- Add-on Round 2 / Prompt U: Comprehensive M3U/M3U8 playlist import and export engine with Room v4 migration, path normalization, 4-stage track matching, and interactive preview UI.
- Room Database Schema v4 Migration:
  - Added indexed `relative_path_norm` on `TrackEntity` table for rapid database-level matching.
  - Implemented `MIGRATION_3_4` and committed Room schema JSON `4.json`.
  - Updated `MetadataExtractor` and `FolderScanner` to extract and populate relative paths during recursive SAF music directory scans.
- Path Normalizer (`PathNormalizer`):
  - Converts Windows backslashes (`\`) to forward slashes (`/`).
  - Strips Windows drive prefixes (`C:\`, `D:/`).
  - Decodes percent-encoded URLs (`%20` $\to$ space).
  - Normalizes leading relative tokens (`./`) and redundant slashes.
- High-Performance M3U/M3U8 Parser (`M3uParser`):
  - Streaming line parser with bounds safety (5 MB size cap, 50,000 entry cap).
  - Supports `#EXTM3U`, `#PLAYLIST:<name>`, and `#EXTINF:<duration>,<artist> - <title>` or `<title>`.
  - UTF-8 with BOM stripping and ISO-8859-1 fallback support.
- Multi-Stage Track Resolution Engine (`TrackMatcher`):
  - Stage 1: Exact `relativePathNorm` match ($O(1)$ lookup).
  - Stage 2: Suffix path match across trailing directory segments (handles library transfers from external OS roots).
  - Stage 3: Filename match with duration tolerance disambiguation ($\pm 2\text{s}$).
  - Stage 4: Tag metadata match on title and artist with duration tolerance ($\pm 3\text{s}$).
  - Returns structured `MatchSummary` classifying items into `Matched`, `Ambiguous`, and `NotFound`.
- M3U8 Export Serializer (`M3uWriter`):
  - Serializes playlists, favorites, and current queue into standard `#EXTM3U` UTF-8 format.
  - Automatically calculates relative paths against library root.
- Import & Export UX:
  - Interactive modal bottom sheet (`M3uImportPreviewSheet`) displaying parsed playlist name, match counts, and conflict resolver.
  - Seamless SAF file picker import and create/merge options.
- Compatibility Tests:
  - `PathNormalizerTest`: Tests Windows backslashes, percent URLs, and segment slicing.
  - `M3uParserTest`: Tests standard EXTM3U, simple paths, UTF-8 BOM, and comments.
  - `TrackMatcherTest`: Tests all 4 resolution stages and ambiguity detection.
  - `M3uWriterTest`: Tests serialization of headers, tracks, and durations.
  - `M3uFixturesCompatibilityTest`: Verifies real-world playlist fixtures from foobar2000, VLC, MusicBee, and Poweramp.

## [0.20.0-android-auto] - 2026-11-06
### Added
- Add-on Round 2 / Prompt T: Android Auto, Bluetooth car browsing, and Google Assistant voice control support via MediaLibraryService migration.
- `PlaybackService` Migration to `MediaLibraryService`:
  - Migrated `PlaybackService` from `MediaSessionService` to `MediaLibraryService`.
  - Built `MediaLibrarySession` implementing `MediaLibrarySession.Callback` for tree browsing, paging, and voice queries.
  - Fully backward-compatible with phone playback, notification channel, widget updater, sleep timer, and tonearm synchronization.
- Automotive Browse Tree (`MediaBrowseTree`):
  - 6 structured root categories: Recently Played (List), Favorites (List), Playlists (List), Albums (Grid), Artists (Grid), All Songs (List).
  - "Shuffle All" primary action header in the All Songs section.
  - Standard Android Auto content style hints (`CONTENT_STYLE_GRID_ITEM` vs `CONTENT_STYLE_LIST_ITEM`).
  - Paged off-thread loading using Room DAOs (`TrackDao`, `AlbumDao`, `ArtistDao`, `PlaylistDao`, `FavoriteDao`, `HistoryDao`).
- Secure Car Artwork Provider (`CarArtworkProvider`):
  - Read-only `ContentProvider` serving cached album art to head units via `content://com.isai.player.artwork/<trackId>`.
  - Robust path-traversal guards rejecting directory traversal characters (`..`, `/`, `\`, `%`).
- Caller Package Validation (`PackageValidator`):
  - Classifies client trust levels (`SELF`, `TRUSTED_AUTOMOTIVE`, `TRUSTED_ASSISTANT`, `TRUSTED_SYSTEM`, `UNTRUSTED_READ_ONLY`).
  - Protects queue modification and playback permissions against untrusted callers.
- Google Assistant Voice Search (`VoiceSearchResolver`):
  - Handles `onSearch` and `onGetSearchResult` voice queries.
  - Resolves queries to specific tracks, entire artist catalogs, albums in track order, or library shuffle.
- Car Custom Actions:
  - Supports `com.isai.player.action.TOGGLE_FAVORITE` directly from automotive head units.
- Automotive Manifest & Documentation:
  - Declared `res/xml/automotive_app_desc.xml` with `<uses name="media"/>`.
  - Registered `MediaLibraryService` and `MediaBrowserService` intent filters in `AndroidManifest.xml`.
  - Added comprehensive Desktop Head Unit (DHU) guide in `docs/android-auto.md`.
- Unit Tests:
  - `PackageValidatorTest`: Validates caller package trust classification.
  - `CarArtworkProviderTest`: Validates URI path parsing and path traversal security rejection.
  - `MediaBrowseTreeTest`: Validates 6 root nodes, content style hints, and hierarchy.
  - `VoiceSearchResolverTest`: Validates query resolution for artist, album, title, and shuffle fallback.

## [0.19.0-equalizer] - 2026-11-06
### Added
- Add-on Round 2 / Prompt S: Complete Equalizer and Audio Effects Engine with dynamic device band probing, log-frequency Catmull-Rom spline curve, automatic clipping compensation, and system panel interop.
- Audio Effects Engine (`AudioEffectsManager`):
  - Hardware effects attached to ExoPlayer's `audioSessionId` (`android.media.audiofx.Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer`).
  - Safe lifecycle management: Re-attaches seamlessly on `onAudioSessionIdChanged` and cleans up on release.
  - Probes device hardware capabilities without hardcoding band counts or ranges (supports 5, 8, 10, or variable device bands and ranges).
  - Broadcasts `ACTION_OPEN_AUDIO_EFFECT_SESSION_ID` and `ACTION_CLOSE_AUDIO_EFFECT_SESSION_ID` for system-wide DSP compatibility.
- Log-Frequency Preset Resampling (`EqPresetResampler`):
  - 14 built-in acoustic and genre presets (Flat, Bass Boost, Bass Reducer, Treble Boost, Vocal, Rock, Pop, Jazz, Classical, Electronic, Hip-Hop, Loud, Acoustic, Late Night).
  - Resamples normalized 10-band octave reference curves to device hardware center frequencies using log-frequency interpolation ($x = \log_{10}(f)$).
- Preamp & "Prevent Clipping" Auto-Compensation (`ClippingMath`):
  - Digital preamp gain control from $-12.0\text{ dB}$ to $+12.0\text{ dB}$ (0.5 dB steps).
  - Auto-headroom analysis: When positive EQ/preamp boosts are applied, automatically computes dynamic digital attenuation factor ($10^{-\text{boost}/20}$) to prevent inter-sample distortion and digital clipping.
  - Informative UI badge displaying active headroom protection or clipping warning.
- Interactive Equalizer UI (`EqualizerScreen`, `EqCurveView`):
  - Smooth Catmull-Rom spline Canvas curve passing directly through band coordinates with glowing accent fill.
  - Interactive draggable band nodes with haptic feedback tick upon crossing $0\text{ dB}$.
  - Synchronized vertical band sliders with $0.5\text{ dB}$ resolution.
  - Bass Boost (0 - 100%) and Virtualizer (0 - 100%) sliders.
  - Preset selector chip carousel with Custom Preset creation and persistence in DataStore.
  - "Open System Equalizer" intent launcher for OEM audio panels.
- Unit Tests:
  - `EqPresetResamplerTest`: Tests flat, bass boost, boundary clamping, and extrapolation across 5 and 10 band targets.
  - `ClippingCompensationTest`: Validates headroom analysis, auto-attenuation scaling, and clipping detection.
  - `EqualizerRepositoryTest`: Tests DataStore persistence, default states, and custom preset serialization.

## [1.1.0] - 2026-11-06
### Added
- **Add-on Round 1 Major Release**: Delivers audio waveform extraction with interactive seekbar, complete offline synced lyrics, and responsive Jetpack Glance home-screen widget.
- **Waveform Seekbar (Prompt O)**:
  - On-device PCM decoding and transient peak extraction via `MediaCodec` and `MediaExtractor` on bounded dispatcher.
  - Normalization with 95th percentile clipping and $\gamma = 0.7$ curve.
  - Room database schema v2 (`waveforms` entity and `MIGRATION_1_2`) with foreign key cascade.
  - Interactive Canvas `WaveformSeekBar` with bars, line, and classic styles.
  - Scrub arbitration (`ScrubOwner.ARM` vs `ScrubOwner.BAR`) preventing touch collisions with the spinning vinyl tonearm.
- **Offline Synced Lyrics (Prompt P)**:
  - Strict priority resolution: User Imported > Sidecar (`.lrc`/`.txt`) > Embedded tags.
  - Zero-dependency ID3v2 `USLT`/`SYLT` and Vorbis `LYRICS` stream parser in pure Kotlin.
  - Standard and 3-digit millisecond LRC parser with $O(\log N)$ binary search active line resolution.
  - Room database schema v3 (`lyrics` entity and `MIGRATION_2_3`).
  - Centered auto-scrolling `LyricsView`, 4-second user-scroll pause timer, tap-to-seek, and timing offset calibration modal sheet ($\pm 0.5\text{s}$, $\pm 0.1\text{s}$).
- **Jetpack Glance Home-screen Widget (Prompt Q)**:
  - Responsive layouts: Small (2x2), Medium (4x1 / 4x2), and Large (4x3+).
  - High-resolution photorealistic turntable renderer with tonearm posture sync (playing vs paused) and in-memory LRU caching.
  - Interactive Glance action callbacks for playback control and favorite toggling.
  - Throttled updater (max 1 IPC per 10 seconds for progress) initiated in `IsaiApp.onCreate()`.
- **Quality Assurance & Verification Suite (Prompt R)**:
  - `RoomMigrationTest`: Validates SQL DDL integrity for `MIGRATION_1_2` and `MIGRATION_2_3`.
  - `AddOnRound1ContractTest`: Enforces strict zero-network contract (`0` internet permissions), deterministic waveform fallback, lyrics parsing/priority, and widget IPC throttling.

## [0.18.0-glance-widget] - 2026-11-06
### Added
- Add-on Round 1 / Prompt Q: Jetpack Glance Home-screen Widget with responsive size layouts, pre-rendered turntable bitmap, interactive action callbacks, and throttled updater.
- Responsive Jetpack Glance Widget (`IsaiGlanceWidget`):
  - Small layout (2x2): Compact turntable visual with circular Play/Pause overlay.
  - Medium layout (4x1 / 4x2): Turntable art, track title, artist name, Play/Pause, and Next track controls.
  - Large layout (4x3+): Full turntable record, track metadata, Previous/Play-Pause/Next controls, favorite toggle, and active track progress bar.
- Photorealistic Turntable Pre-Renderer (`WidgetArtRenderer`):
  - Pre-renders 300x300 high-resolution turntable art (vinyl disc, concentric grooves, circular album cover label, and pivot-mounted tonearm) off-thread.
  - Synchronizes tonearm posture: Lowered to record groove when playing (`100°`), parked on rest post when paused (`64°`).
  - High-performance in-memory LRU bitmap cache keyed by `"${trackId}_${isPlaying}"` with zero IPC lag.
- Interactive Widget Action Callbacks (`WidgetActions`):
  - `TogglePlayPauseAction`: Direct play/pause toggling through `PlayerController`.
  - `SkipNextAction`: Advances playback to the next queue item.
  - `SkipPreviousAction`: Returns to previous track or seeks to beginning.
  - `ToggleFavoriteAction`: Toggles favorite state via `FavoritesRepository`.
  - Hilt `@EntryPoint` injection (`WidgetActionEntryPoint`) cleanly bridges remote Glance callback contexts to singleton repositories.
- Throttled Progress & State Observer (`WidgetUpdater`):
  - Automatically started in `IsaiApp.onCreate()`.
  - Immediate widget updates on track changes or play/pause toggles.
  - Throttled progress bar updates (max 1 IPC broadcast per 10 seconds) to prevent battery drain and launcher process IPC saturation.
- XML Metadata & Receiver Registration:
  - AppWidget provider XML (`isai_widget_info.xml`) with preview layout and key metadata.
  - Loading preview XML layout (`widget_loading_layout.xml`).
  - `IsaiGlanceWidgetReceiver` declared in `AndroidManifest.xml` with standard `APPWIDGET_UPDATE` intent filters.
- Unit Tests:
  - `WidgetArtRendererTest`: Verifies cache key formatting and track/play state uniqueness.
  - `WidgetUpdaterTest`: Verifies immediate updates on state changes and 10s throttling behavior for steady-state playback.

## [0.17.0-offline-lyrics] - 2026-11-06
### Added
- Add-on Round 1 / Prompt P: Complete offline lyrics support (discovery, pure Kotlin ID3v2/Vorbis tag extraction, LRC parser, auto-scrolling LyricsView, offset calibration, Room v3 migration).
- Offline Discovery & Priority Order:
  - Priority chain: User-imported lyrics > Sidecar file (`<song>.lrc` / `<song>.txt`) > Embedded tags (`USLT`/`SYLT`/Vorbis).
  - Rescanning the library never overwrites user imports or calibrated offsets.
- Zero-Dependency Embedded Tag Reader (`EmbeddedLyricsReader`):
  - Pure Kotlin stream parser extracting ID3v2 `USLT` (Unsynchronized) & `SYLT` (Synchronized) lyrics from MP3 files.
  - Vorbis Comment block parser extracting `LYRICS` tags from FLAC and OGG files.
  - Adds zero third-party dependencies or binary overhead.
- High-Performance LRC & Plain-Text Parser (`LrcParser`):
  - Parses standard `[mm:ss.xx]` and 3-digit `[mm:ss.xxx]` timestamps.
  - Supports multi-timestamp lines (e.g. `[00:10.00][00:20.00]Chorus line`) into discrete timed entries.
  - Extracts metadata headers `[ti:]`, `[ar:]`, `[al:]`, `[offset:±ms]`.
  - Encoding resilience: handles UTF-8 with BOM (`\uFEFF`), UTF-16, and ISO-8859-1 fallback without crashing.
  - Efficient $O(\log N)$ binary search lookup for instantaneous active line resolution on position ticks.
- Room Database Migration v2 -> v3:
  - `LyricsEntity`: Stores trackId, source (`IMPORTED`, `SIDECAR`, `EMBEDDED`), format (`SYNCED`, `PLAIN`), content, and offset with Foreign Key cascade deletion to `TrackEntity`.
  - `MIGRATION_2_3`: SQLite migration with committed schema JSON `3.json`.
- Now Playing Lyrics Experience (`LyricsView`, `LyricsOffsetDialog`):
  - Top bar toggle button to smoothly crossfade between `VinylStage` and `LyricsView`.
  - Synced View: Centered `LazyColumn` auto-scrolls with active line in vivid accent, bold weight, and scale pop.
  - 4-Second User Scroll Pause: User scrolling temporarily pauses auto-scroll and surfaces a "Jump to current" floating pill.
  - Tap-to-seek: Tapping any lyric line seeks playback directly and animates the physical tonearm to the matching groove.
  - Offset Calibration Sheet: Modal bottom sheet offering coarse ($\pm 0.5\text{s}$) and fine ($\pm 0.1\text{s}$) timing calibration, persisted to Room.
  - Empty state with direct "Import Lyrics" SAF document picker.
  - TalkBack accessibility with live region announcements on active lyric line transitions.
- Settings Integration:
  - Appearance: Lyrics text size selector (`Small`, `Medium`, `Large`) and "Keep screen on while viewing lyrics" switch.
  - Library: Lyrics count display.
- Unit Tests:
  - `LrcParserTest`: Validates standard, 3-digit, multi-timestamp, unsorted, BOM, and binary search lookup.
  - `LyricsRepositoryTest`: Verifies priority order, user import persistence, and offset updating.

## [0.16.0-waveform-seekbar] - 2026-11-06
### Added
- Add-on Round 1 / Prompt O: On-device audio waveform extraction, Room database schema v2 migration, high-performance Canvas WaveformSeekBar, shared tonearm scrub arbitration, and settings appearance options.
- Waveform Generation & Audio DSP:
  - `WaveformExtractor`: Hardware-accelerated PCM decoding using `MediaExtractor` and `MediaCodec` on a single-worker bounded dispatcher (`limitedParallelism(1)`).
  - Time-based 200-bucket peak extraction ($\max |s|$) preserving musical transients and rhythm over RMS.
  - 95th percentile normalization scaling and mild gamma curve ($\gamma = 0.7$) to keep quiet acoustic passages visually clear.
  - Windowed seek-to decoding budget for long audio files (> 12 minutes).
  - Clean resource release and prompt coroutine cancellation checking (`isActive`).
- Room Database Migration v1 -> v2:
  - `WaveformEntity`: Stores 200 peak amplitude bytes with Foreign Key cascade deletion to `TrackEntity`.
  - `MIGRATION_1_2`: SQLite table creation migration with committed schema JSONs (`1.json` and `2.json`).
  - Cache invalidation guards: `WAVEFORM_VERSION` check and `lastModified` track edit comparison.
  - In-flight mutex (`ConcurrentHashMap`) preventing duplicate simultaneous decoding jobs.
- Deterministic Calm Fallback:
  - Smooth sinusoidal harmonic fallback uniquely seeded from `trackId` that seamlessly crossfades over 300ms.
- High-Performance UI Component (`WaveformSeekBar`):
  - Canvas rendering via `drawWithCache` and lambda-read `progress()` ensuring ZERO recompositions during playback ticks.
  - Styles: `WAVEFORM_BARS` (mirrored rounded bars with gradient/accent played fill and active bar pop), `WAVEFORM_LINE` (smooth silhouette path), and `CLASSIC` (plain track and thumb).
  - Horizontal scrubbing with vertical-slop cancellation, 5% tactile haptic ticks, and TalkBack slider semantics.
- Tonearm & Seekbar Scrub Arbitration:
  - `ScrubOwner` (`NONE`, `ARM`, `BAR`) in `NowPlayingViewModel` ensures dragging the waveform smoothly moves the tonearm in real time and dragging the tonearm moves the waveform playhead without input conflict.
- Settings Integration:
  - Appearance: Seekbar style selector (`AUTO`, `WAVEFORM_BARS`, `WAVEFORM_LINE`, `CLASSIC`) with live preview tile.
  - Library: Waveform cache count display, "Clear Waveform Cache" button, and charging pre-generation preference.
- Unit Tests:
  - `WaveformMathTest`: Math validation for bucketing, percentile scaling, resampling, and fallback determinism.
  - `WaveformRepositoryTest`: Verifies cache hits, stale version invalidation, and file edit detection.
  - `ScrubOwnershipTest`: Verifies bidirectional tonearm and waveform scrub synchronization.

## [0.15.0-tests-perf-accessibility] - 2026-11-06
### Added
- Phase 3 / Prompt M: Comprehensive TalkBack screen reader accessibility, custom accessibility actions, WCAG 2.1 AA contrast tuning, minimum 48dp touch bounds, and ART ahead-of-time baseline profiles.
- TalkBack & Assistive Technology:
  - `TonearmController`: Semantic slider node with dynamic content descriptions ("Tonearm needle on vinyl record. Progress X percent") and state descriptions ("Playing" vs "Paused").
  - Custom Accessibility Actions: TalkBack users can perform "Lift Needle", "Drop Needle", "Seek forward 10 seconds", and "Seek backward 10 seconds" via assistive actions menu without physical gesture dragging.
  - `VinylDisc`: Accessible semantic role and description ("Vinyl record").
  - `ControlDial`: Enforced minimum 48x48 dp touch target bounding boxes on all playback control icon buttons.
  - Accessibility strings localized in `strings.xml`.
- Color Contrast & Visual Accessibility:
  - Adjusted Light theme tokens (`LightAccentDefault` = `#3857DC`, `LightOnSurfaceVariant` = `#525A7A`) guaranteeing >= 4.5:1 contrast against light backgrounds and surfaces.
- Performance & Ahead-of-Time (AOT) Compilation:
  - `baseline-prof.txt`: ART profile rules optimizing cold startup (`MainActivity`, `IsaiApp`), Jetpack Compose drawing, tonearm geometry math loops, vinyl disc shaders, Media3 audio pipeline, and Room database DAOs.
- Unit Tests:
  - `AccessibilityContrastTest`: Pure JVM test mathematically validating WCAG 2.1 AA relative luminance contrast ratios for all core theme colors on light and dark surfaces.
  - `TonearmSemanticsTest`: Validates TalkBack step-seeking, clamping math, state description, and action dispatching.

## [0.14.0-hardening-edge-cases] - 2026-11-06
### Added
- Phase 3 / Prompt L: Comprehensive hardening against SAF storage permission revocation, corrupt audio auto-skipping, zero-duration divide-by-zero guards, and Android StrictMode diagnostics.
- Storage Access Framework Hardening:
  - Validates persisted permissions via `contentResolver.persistedUriPermissions` on cold start in `MainActivity`.
  - Automatically resets invalidated/revoked music folder paths and smoothly routes the user to `OnboardingScreen` without crashing.
  - Safeguarded `FolderScanner`, `MetadataExtractor`, and `ArtCache` against `SecurityException` and permission expiration.
- Playback Resilience & Corrupt Audio Auto-Skip:
  - ExoPlayer error listener in `PlaybackService` catches `PlaybackException` (IO/Decode errors) and auto-skips to the next track in the queue.
  - Loop guard circuit breaker: stops playback after $N$ consecutive errors ($N \ge \text{queue.size}$) to prevent infinite error loops on invalid playlists.
  - Broadcasts error message through `PlayerState.playbackErrorMessage` and resets on successful playback.
- Zero-Duration & NaN Mathematical Safeguards:
  - `TonearmGeometry`: Math guards against `Float.NaN` and `Float.POSITIVE_INFINITY` in `trackAngle` and `progressFromAngle`.
  - `PlayerState.progress`: Clamps to 0f safely whenever `durationMs <= 0`.
- Android StrictMode:
  - Configured in `IsaiApp.kt` for `BuildConfig.DEBUG` with `ThreadPolicy` detecting slow calls and `VmPolicy` detecting leaked SQLite objects and closeables.
- Unit tests: `PlaybackHardeningTest` verifying zero duration progress, NaN safety in tonearm geometry, and clamping bounds.

## [0.13.0-settings-theming-polish] - 2026-11-06
### Added
- Phase 3 / Prompt K: Production Settings screen, Theming polish, Library maintenance, Vinyl needle-lift preferences, and WCAG AA accessibility compliance.
- `SettingsScreen` & `SettingsViewModel`:
  - Appearance: System, Dark, and Light theme modes with immediate reactivity, and Dynamic Accent color switch.
  - Vinyl & Playback: Tonearm needle-lift physics preference (`Pause` vs `Stop & Rewind to 0:00`), and Sleep Timer configuration shortcut.
  - Library Management: Active SAF music folder display, "Rescan Library" trigger with progress indicators, "Change Folder" document tree picker, and "Clean Missing Songs" with confirmation dialog.
  - About & Privacy: Version display, 100% Offline Guarantee badge highlighting zero internet permissions and zero telemetry, and Component Labs testing shortcut.
  - Accessibility: WCAG AA contrast compliance ($\ge 4.5:1$) across dark and light themes, and resilient 200% Android system font scaling.
- Unit tests: `SettingsViewModelTest` validating preference dispatching, library purging, and sleep timer delegation.

## [0.12.0-favorites-playlists-sleep-timer] - 2026-11-06
### Added
- Phase 3 / Prompt J: Offline Favorites screen, Playlist lifecycle management (Create, Rename, Delete, Reorder, Add), Multi-select Track Picker, and Sleep Timer UI dialog.
- `FavoritesScreen` & `FavoritesViewModel`:
  - Full reactive favorite songs stream powered by `FavoritesRepository.observeFavoriteTracks()`.
  - Favorites hero header with heart badge, track counter, total duration, and Play All / Shuffle buttons.
  - Track rows with 3-dot context menu supporting instant unfavorite, play next, and add to queue.
  - Heart-themed empty state when no songs have been favorited yet.
- Playlist Management & Lifecycle:
  - `CreatePlaylistDialog`: Dialog for creating new playlists and renaming existing playlists with instant input validation.
  - `AddToPlaylistDialog`: Quick selection dialog to append tracks to existing playlists or create a new playlist on the fly.
  - `TrackPickerBottomSheet`: Multi-select modal bottom sheet with search filtering and batch track addition to playlists.
  - Upgraded Playlists tab in `LibraryScreen`: "+ Create Playlist" action header, playlist navigation, and empty state.
- `PlaylistDetailScreen` & `PlaylistDetailViewModel`:
  - Playlist hero header with total tracks, total formatted duration, and Play All / Shuffle buttons.
  - Direct "Add Songs" button launching `TrackPickerBottomSheet`.
  - TopAppBar options menu: Rename Playlist and Delete Playlist (with confirmation dialog).
  - Track-level 3-dot menu with "Remove from Playlist" action.
- `SleepTimerDialog`:
  - Presets for 15 min, 30 min, 45 min, 60 min, and "End of current track".
  - Real-time countdown status indicator and instant timer cancellation.
  - Integrated Sleep Timer moon icon button into `NowPlayingScreen` top bar and wired to `NowPlayingViewModel`.
- Unit tests: `FavoritesViewModelTest`, `PlaylistDetailViewModelTest`, and updated `NowPlayingViewModelTest`.

## [0.11.0-home-library-search] - 2026-11-06
### Added
- Phase 3 / Prompt I: Complete Home, Library, Detail screens (Album & Artist), Search screen, and Track action menu.
- `HomeScreen` & `HomeViewModel`:
  - "Shuffle All" hero card with instant library shuffle playback.
  - "Played Recently" horizontal carousel driven by `HistoryRepository.observeRecentlyPlayed(limit = 20)`.
  - Library overview statistic cards (Songs, Albums, Artists).
- `LibraryScreen` & `LibraryViewModel`:
  - 5 primary tabs: **Songs**, **Albums**, **Artists**, **Playlists**, and **Folders**.
  - Dropdown sort menu for Songs: Title (A-Z), Artist (A-Z), Album (A-Z), Year (Newest), and Recently Added.
  - 2-column adaptive grid for Albums with `AlbumCard`s and navigation to Album Detail.
  - Artists tab with circular avatars, track/album count subtitles, and navigation to Artist Detail.
  - Custom playlists and folder directory listing with track counts.
- `AlbumDetailScreen` & `AlbumDetailViewModel`:
  - Signature peeking vinyl record hero header (album jacket with a scaled `VinylDisc` partially exposed at $12^\circ$ tilt).
  - Release year, track count, and total duration metadata summary.
  - "Play All" and "Shuffle" action buttons.
  - Multi-disc grouped track listing (`Disc 1`, `Disc 2`) with track indices, titles, artists, and durations.
- `ArtistDetailScreen` & `ArtistDetailViewModel`:
  - Circular artist avatar hero banner with artist discography statistics.
  - "Discography" horizontal carousel of albums.
  - "All Songs" listing by the artist with 3-dot context menu.
- `SearchScreen` & `SearchViewModel`:
  - Interactive search bar with instant clear icon and 300ms debounce.
  - Categorized offline search results: Songs, Albums, and Artists.
  - Clean empty search state and no-results feedback.
- `TrackMenuBottomSheet` & `TrackInfoDialog`:
  - Standard modal bottom sheet for all track rows: "Play Next", "Add to Queue", "Favorite" toggle, and "Song Info".
  - Dialog displaying full track metadata: file format, size, duration, folder path, and release year.
- Comprehensive unit test suites: `HomeViewModelTest`, `LibraryViewModelTest`, `SearchViewModelTest`, `AlbumDetailViewModelTest`, and `ArtistDetailViewModelTest`.

## [0.10.0-nowplaying-integration] - 2026-11-06
### Added
- Phase 3 / Prompt H: Full Now Playing integration with live VinylStage, interactive seekbar, ControlDial, QueueBottomSheet, and live MiniPlayer.
- `NowPlayingViewModel`: Unified ViewModel decoupling scrub preview from playback ticks, mapping tracks to `StageTrack`, managing favorite toggle, and dispatching tonearm needle actions.
- `NowPlayingScreen`: Complete Now Playing screen integrating `VinylStage` (vinyl disc rotation, tonearm tracking, needle bounce, headshell tap toggle, horizontal disc swipe), seekbar with elapsed and remaining time labels, favorite heart button, and queue modal sheet trigger.
- `ControlDial`: Hardware dial component with shuffle toggle, skip previous (with 3-second restart rule), play/pause with spring scale animation, skip next, and 3-state repeat mode cycling (`OFF` $\to$ `ALL` $\to$ `ONE`).
- `QueueBottomSheet`: Material 3 modal bottom sheet displaying the playing queue, highlighting active song with animated equalizer icon, instant seek on tap, individual track removal, and clear queue action.
- Live `MiniPlayer`: Connected in `MainScaffold` directly to `PlayerController` with real album artwork, title, artist, progress line, and responsive play/pause and skip controls.
- Unit tests in `NowPlayingViewModelTest` verifying scrub preview decoupling, lift behavior honoring `STOP` vs `PAUSE`, and favorite toggling.

## [0.9.0-playback-engine] - 2026-11-06
### Added
- Phase 3 / Prompt G: AndroidX Media3 playback engine, `PlaybackService`, `PlayerController` facade, and audio automation.
- `PlaybackService`: Foreground `MediaSessionService` with ExoPlayer, audio focus management, becoming noisy handling (auto-pause on headphone disconnect), local wake lock, and system MediaStyle notifications with album art and compact actions.
- `PlayerController`: Single source of truth UI facade singleton managing `MediaController` connection, exposing `StateFlow<PlayerState>`, and running a zero-allocation 200ms position ticker active strictly during playback.
- Complete playback controls: play, pause, toggle, seek, 3-second skip-previous restart rule, next with queue wrap-around, shuffle, and 3-state repeat mode cycling (`OFF` $\to$ `ALL` $\to$ `ONE`).
- Queue management: `playTrack`, `playNext`, `addToQueue`, `removeFromQueue`, `reorderQueue`, and `clearQueue`.
- `QueueStore`: Automatic playback queue and position persistence to Room (`QueueDao`) and cold launch restoration.
- `PlayHistoryTracker`: Automatic playback logger recording songs to `HistoryRepository` once played for $\ge 30\text{ seconds}$ (or $\ge 50\%$ for short tracks).
- `SleepTimer`: Background countdown timer supporting 15/30/45/60 min presets and "End of current track", with a smooth 5-second exponential volume fade-out.
- Unit tests: `PlayerStateTest`, `PlayHistoryTrackerTest`, and `SleepTimerTest`.

## [0.8.0-onboarding-scanner] - 2026-11-06
### Added
- Phase 3 / Prompt F: Storage Access Framework folder selection, high-speed recursive scanner, and onboarding.
- `FolderScanner`: Direct `DocumentsContract.buildChildDocumentsUriUsingTree` child cursor querying eliminating slow `DocumentFile` Binder roundtrips, with queue-based recursion and audio format filtering.
- `ArtCache`: RAM-safe embedded album art decoder with `inSampleSize` downsampling to maximum $512 \times 512\text{ px}$ JPEG and atomic file caching.
- `MetadataExtractor`: Bounded `MediaMetadataRetriever` parser with compound track/disc number splitting (`"03/12"` $\to 3$), release year extraction, and filename sanitization fallback for untitled tracks.
- `LibraryScanner`: High-throughput incremental diff engine comparing `lastModified` timestamps to skip unmodified tracks, soft-flagging missing tracks (`isMissing = true`), and updating Room via batched insertions.
- `OnboardingScreen`: Interactive folder picker with SAF permission persistence, real-time scanning progress bar, track counter, completion summary, and automatic transition to Home.
- `OnboardingViewModel`: Connects SAF folder selection to `LibraryScanner` and `SettingsRepository`.
- Start destination routing in `MainScaffold`: automatically routes first-time users to `Onboarding` and returning users to `Home`.
- Unit tests in `FolderScannerTest` and `MetadataExtractorTest`.

## [0.7.0-data-layer] - 2026-11-06
### Added
- Phase 3 / Prompt E: Room database architecture, DAOs, repositories, and typed DataStore settings.
- `Fnv1a`: 64-bit Fowler-Noll-Vo deterministic URI hashing engine for stable, collision-resilient primary keys without auto-increment dependencies.
- Room entities with foreign key cascades:
  - `TrackEntity` (with indexed sorting columns, metadata, art cache path, folder path, missing track flag, and forward-compatible fields for Lyrics and M3U).
  - `PlaylistEntity` and `PlaylistTrackEntity` (composite foreign keys with cascade deletion and explicit position ordering).
  - `FavoriteEntity` (cascade on track deletion).
  - `PlayHistoryEntity` (timestamped playback logging).
  - `QueueItemEntity` (queue restoration on cold start).
- Aggregate projections for derived grouping: `AlbumProjection`, `ArtistProjection`, `FolderProjection`, and `PlaylistWithCount`.
- Comprehensive Room DAOs:
  - `TrackDao` with 8 sort variants, missing track pruning, and search.
  - `AlbumDao`, `ArtistDao`, `FolderDao` with group-by projections.
  - `FavoriteDao` with atomic toggle.
  - `PlaylistDao` with track ordering, reordering, and cascades.
  - `HistoryDao` and `QueueDao`.
- Clean domain models (`Track`, `Album`, `Artist`, `Folder`, `Playlist`, `QueueSnapshot`, `HistoryEntry`) and bidirectional mappers.
- Production `SettingsRepository` backed by AndroidX DataStore Preferences managing `themeMode`, `liftBehavior`, `selectedFolderUri`, `selectedFolderName`, `dynamicAccentColor`, and `sleepTimer`.
- Hilt DI modules: `DatabaseModule`, `RepositoryModule`, and `DataStoreModule`.
- Full unit test suite: `Fnv1aTest`, `TrackEntityMappingTest`, `IsaiDatabaseTest`, and `SettingsRepositoryTest`.

## [0.6.0-vinyl-stage] - 2026-11-06
### Added
- Phase 2 / Prompt D: VinylStage unified assembly, gesture arbitration, and `VinylStageLabScreen`.
- `StageTrack` data model and `SwipeDirection` enum for horizontal disc navigation.
- `StageBackground` composable rendering ambient blurred/darkened album art (Dark theme) or subtle accent gradient (Light theme).
- `VinylStage` composable harmonizing vinyl disc rotation physics, tonearm tracking, and needle drop bounce:
  - Synchronized playback rules: disc spins only when playing and not scrubbing (`isPlaying && !isScrubbing`).
  - Play triggers immediate spin-up with ~700ms tonearm lower animation; pause parks tonearm first followed by disc spin-down ~150ms later.
  - Tonearm lifts during track changes and smoothly lowers if playback continues.
  - Multi-touch gesture arbitration ensuring tonearm hit target takes strict priority over horizontal track swiping.
  - Horizontal swipe on vinyl disc to skip tracks with directional slide transition (left/right) and velocity threshold ($> 800\text{ px/s}$).
- `VinylStageLabScreen` interactive testbed featuring 5 sample tracks, external notification pause simulation, seekbar scrub sync, lift behavior toggle (`PAUSE` vs `STOP`), theme toggle, live event logger, and real-time FPS counter.
- Unit tests in `VinylStageTest` verifying synchronization rules, 3-second track restart threshold, track index wrap-around, and fallback seed generation.

## [0.5.0-vinyl-disc] - 2026-11-06
### Added
- Phase 2 / Prompt C: `VinylDisc` component, 33⅓ RPM rotation physics, and `VinylDiscLabScreen`.
- `VinylDiscState` physics engine with 500ms ease-in spin-up, 900ms ease-out spin-down, angle retention on pause, and frame hitch clamping ($\le 50\text{ms}$).
- `VinylDisc` composable featuring 3 distinct layers:
  - Layer 1 (Static): Ambient radial accent glow (Dark) or soft drop shadow (Light).
  - Layer 2 (Rotating): Full-bleed circular album art covering the entire disc, 12% black depth scrim, 48 concentric groove rings with deterministic pseudo-random alpha, 3 track gap bands, outer rim highlight, and center spindle hole.
  - Layer 3 (Static): Stationary sweep gradient sheen highlights ($20^\circ \dots 50^\circ$ and $200^\circ \dots 230^\circ$) plus faint radial vignette.
- `VinylColors` theme-aware color token holder.
- `VinylFallbackArt` generating deterministic dual-hue diagonal gradients and initials from track title/artist hash.
- `VinylDiscLabScreen` debug dashboard featuring real-time FPS counter, angle/speed readouts, fallback preset switcher, and reduced motion mode.
- Pure JVM unit tests in `VinylDiscStateTest` validating 33⅓ RPM physics, speed decay, $360^\circ$ wrap-around, and 50ms hitch damping.

## [0.4.0-tonearm-motion] - 2026-11-06
### Added
- Phase 2 / Prompt B: Tonearm motion physics, pointer gestures, and `FakePlayer` simulation.
- `TonearmState` state machine managing needle drop bounce with spring physics (`dampingRatio = 0.55f, stiffness = 200f`), parking animation (`tween(600, FastOutSlowIn)`), touch scale-up ($1.0 \to 1.03$), and deny shake ($\pm 3^\circ$, 3 oscillations over 300ms) when touched without an active track.
- `TonearmInteractiveContainer` gesture detector with live scrubbing and haptics.
- `FakePlayer` timeline simulation.
- Upgraded `TonearmLabScreen`.

## [0.3.0-tonearm] - 2026-11-06
### Added
- Phase 2 / Prompt A: Tonearm shape, geometry and Canvas drawing.
- Pure Kotlin `TonearmGeometry` solving circle-circle intersection for scale-invariant outer/inner groove tracking.
- `Tonearm` composable with pure Canvas drawing across all 11 parts in normalized AU space.
- `TonearmColors` and `TonearmLabScreen`.
- Pure JVM JUnit tests in `TonearmGeometryTest`.

## [0.2.0-theme-nav] - 2026-11-06
### Added
- Phase 1: Design system tokens and navigation shell.
- `IsaiTheme` with dark and light palettes, dynamic color support, and `LocalIsaiColors`.
- Reusable UI components with Dark and Light previews.
- Type-safe `@Serializable` navigation graph and `MainScaffold`.

## [0.1.0-setup] - 2026-11-06
### Added
- Phase 0: Project setup and architectural skeleton (`com.isai.player`).
- Gradle Kotlin DSL build configuration with version catalog (`gradle/libs.versions.toml`).
- AndroidManifest with strictly zero internet permissions.
- Vector adaptive app icon and Core SplashScreen API integration.
