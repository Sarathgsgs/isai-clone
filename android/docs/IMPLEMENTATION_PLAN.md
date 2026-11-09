# Isai: Complete Implementation Plan (Core v1 + Feature Add-ons Round 1 & Round 2)

**Isai** is an offline local-music player for Android. Its Now Playing screen is a spinning vinyl record with a working tonearm. You can drag the tonearm to seek or to lift the needle and stop playback.

---

## 0. Locked Decisions

| Topic | Decision |
|---|---|
| App name | **Isai** (launcher label, splash, About, package `com.isai.player`) |
| Themes | **Dark and light**, with a Settings option: System / Dark / Light |
| Min SDK | **API 26 (Android 8.0)**. Target and compile SDK: latest stable. |
| Disc style | **Album art covers the entire disc**, with a grooves overlay, a static sheen and a tiny spindle hole |
| Tonearm | **Fully interactive**: drag onto the record to seek, drag off the record to pause or stop, tap the headshell to toggle play/pause |
| Music source | A user-selected folder (Storage Access Framework), scanned recursively |
| Network | **STRICTLY ZERO INTERNET PERMISSION**. No ads, no tracking, completely offline. |
| Out of v1 Core | Lyrics, Remixes, Podcasts, "My Wave" recommendations, comment bubbles and online features |

---

## 1. Feature Scope

### Core Library (v1)
- Pick a folder, scan it recursively, and persist access across restarts.
- Read metadata (title, artist, album, year, track number, duration) and embedded album art.
- Rescan manually or incrementally, and handle deleted files.

### Core Playback (v1)
- Play, pause, next, previous, seek, shuffle, repeat (off / all / one).
- Queue management: play next, add to queue, reorder, clear.
- Gapless-friendly ExoPlayer setup.
- Audio focus, pause on headphone unplug, and Bluetooth and headset buttons.
- Foreground service with media notification and lock-screen controls.
- Restore the last queue and position on launch.

### Core Screens & Extras (v1)
- Onboarding (folder picker)
- Home (Played Recently, Shuffle All, mini player)
- Library (Songs / Albums / Artists / Playlists / Folders)
- Album and Artist detail
- Search
- Favorites
- Playlist detail
- **Now Playing (vinyl and tonearm)**
- Queue sheet
- Settings
- Favorites, playlists, and a sleep timer (15/30/45/60 min, end of track).
- Dynamic accent color from album art (Palette).
- Dark and light themes. Play history feeding "Played Recently".

### Add-on Round 1 (v1.1.0)
- **Prompt O: Waveform Seekbar** (PCM decode via MediaCodec, 200 buckets, 95th-percentile normalization, Room caching, sync with tonearm, BARS/LINE/CLASSIC styles).
- **Prompt P: Offline Synced Lyrics** (Sidecar `.lrc`/`.txt`, embedded ID3/Vorbis/MP4 tags, manual file import, time sync, tap-to-seek, auto-scroll, offset adjuster).
- **Prompt Q: Glance Home-screen Widget** (Responsive Small/Medium/Large layouts, pre-rendered vinyl/tonearm bitmaps, background playback controls via MediaController, deep links).
- **Prompt R: QA & Release v1.1.0**.

### Add-on Round 2 (v1.2.0)
- **Prompt S: Equalizer & Audio Effects** (`android.media.audiofx.Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer`, dynamic device band detection, curve canvas UI, presets, clipping prevention).
- **Prompt T: Android Auto & Car/BT Browsing** (`MediaLibraryService`, browse tree, voice search with Google Assistant, car custom actions, `ArtworkProvider` content provider).
- **Prompt U: M3U/M3U8 Playlist Import & Export** (Relative path normalization, multi-stage fallback matcher, export with `.m3u8` UTF-8 / `.m3u` legacy, share sheet).
- **Prompt V: QA & Release v1.2.0**.

---

## 2. Tech Stack

| Concern | Choice |
|---|---|
| Language / UI | Kotlin, Jetpack Compose, Material 3 |
| Navigation | Navigation Compose (type-safe routes) |
| Playback | **AndroidX Media3**: ExoPlayer, `MediaSessionService` / `MediaLibraryService`, `MediaController` |
| Storage access | SAF `ACTION_OPEN_DOCUMENT_TREE` with persisted URI permission |
| Scanning | `DocumentsContract` child-document queries (faster than `DocumentFile`) |
| Metadata | `MediaMetadataRetriever` (embedded art cached as downsized JPEG) |
| Database | Room (with schema migration & FTS4 search) |
| Settings | DataStore (Preferences) |
| DI | Hilt |
| Images / color | Coil and Palette |
| Async | Coroutines and Flow |
| Widgets | Jetpack Glance (`glance-appwidget`, `glance-material3`) |
| Audio Effects | `android.media.audiofx` |
| Testing | JUnit5/4, Turbine, MockK, Compose UI Test, Macrobenchmark and Baseline Profile |
| Build | Gradle Kotlin DSL and a version catalog (`libs.versions.toml`) |

---

## 3. Architecture

MVVM with unidirectional data flow. **The player state is the single source of truth** for the UI, including the vinyl, tonearm, waveform, lyrics, and widgets.

```
Compose UI ─► ViewModel (StateFlow<UiState>) ─► Repository ─► Room / Scanner / DataStore
     ▲                                              │
     └────────── PlayerController (StateFlow<PlayerState>) ◄── MediaController ◄── PlaybackService (ExoPlayer + MediaLibrarySession)
```

### Data Model Summary
- `TrackEntity(id: Long PK [FNV-1a 64-bit hash of URI], uri UNIQUE, title, titleSort, artist, albumArtist, album, year, trackNo, discNo, durationMs, mimeType, sizeBytes, lastModified, artPath, folderPath, dateAdded, isMissing, relativePath?, relativePathNorm?, lyricsUri?, hasLyrics)`
- `PlaylistEntity(id PK, name, createdAt, updatedAt)`
- `PlaylistTrackEntity(playlistId, trackId, position, addedAt)`
- `FavoriteEntity(trackId PK, addedAt)`
- `PlayHistoryEntity(id PK, trackId, playedAt)`
- `QueueItemEntity(position PK, trackId)`
- `WaveformEntity(trackId PK FK cascade, bucketCount, peaks BLOB, version, createdAt)`
- `LyricsEntity(trackId PK FK cascade, source, type, content, offsetMs, updatedAt)`

---

## 4. Design System Tokens

| Token | Dark | Light |
|---|---|---|
| background | `#0B0B0E` | `#F3F4FF` (soft lavender) |
| surface / card | `#16161B` | `#FFFFFF` |
| surfaceVariant | `#22222A` | `#E8EAFB` |
| onBackground | `#F5F5F7` | `#0F1535` (navy) |
| onSurfaceVariant | `#9A9AA5` | `#7C82A3` |
| accent (default) | `#FF8A3D` orange | `#5B7CFA` blue |
| Now Playing background | blurred, darkened art plus scrim | pale gradient tinted by the Palette color |

---

## 5. Vinyl and Tonearm Detailed Specification

### Layers (Bottom to Top)
1. **Background:** Blurred, darkened album art (`RenderEffect` blur on API 31+, pre-blurred bitmap on older; pale gradient in light theme).
2. **Disc shadow / glow:** Static glow/shadow under disc.
3. **Rotating disc layer** (only layer that rotates):
   - Full-bleed circular album art covering the entire disc.
   - Grooves overlay: 48 concentric rings (0.96R down to 0.20R), stroke ~0.6dp, alternating black/white pseudo-random alpha (0.03-0.10), 3 track gap bands.
   - Rim: dark outer ring (0.965R to R) with 0.5dp edge line.
   - Inner ring: 0.12R to 0.20R.
   - Spindle hole: dark circle r = 0.03R with metallic ring.
4. **Static sheen layer:** Fixed sweepGradient wedge highlights (20°–50° & 200°–230°) with faint radial vignette.
5. **Tonearm layer:** Soft shadow offset & blurred by lift, arm drawing.
6. **Interaction overlay:** 56dp+ hit area around headshell.

### Disc Rotation Physics
- 33⅓ RPM ≈ 200°/s.
- `withFrameNanos { dt -> angle = (angle + 200f * spinSpeed.value * dt) % 360 }` with dt clamped <= 50ms.
- Applied in `Modifier.graphicsLayer { rotationZ = angle }` to prevent recomposition.
- Spin-up: ~500ms (`FastOutLinearInEasing`). Spin-down: ~900ms (`LinearOutSlowInEasing`).
- Angle never resets on pause, only resets on track change.
- Spins only when `isPlaying && !isScrubbing && armLowered`.

### Tonearm Geometry (`TonearmGeometry`)
- Pivot $P = C + (0.95R, -1.35R)$, Length $L = 1.5R$.
- $\alpha_{\text{outer}} \approx 90^\circ$, $\alpha_{\text{inner}} \approx 124^\circ$, $\alpha_{\text{rest}} = \alpha_{\text{outer}} - 28^\circ \approx 62^\circ$.
- $S(\alpha) = P + L \cdot (\cos \alpha, \sin \alpha)$.
- `trackAngle(progress) = lerp(alphaOuter, alphaInner, progress)`.
- `progressFromAngle(alpha) = (alpha - alphaOuter) / (alphaInner - alphaOuter)`.
- `isOnRecord(alpha) = alpha >= alphaOuter - 2f && alpha <= alphaInner + 2f`.

### Tonearm Canvas Shape Drawing Steps
- Local space: 1 AU = `armLengthPx / 100`. Pivot at $(0,0)$, arm points $+Y$ ($\alpha_{\text{ref}} = 90^\circ$), Stylus tip $T = (0, 100)$.
- Transform: `translate(pivot) -> rotate(angle - 90) -> scale(AU)`.
- 11 Parts in draw order:
  1. **Shadow:** silhouette in black, offset $(2 \to 10) \times \text{lift}$, `BlurMaskFilter` radius $3 \to 8 \times \text{lift}$, alpha $0.25 \to 0.12$.
  2. **Counterweight rod:** line $(0,0) \to (0,-10)$, stroke 2, `metalMid`.
  3. **Counterweight:** roundRect $x \in [-6, 6], y \in [-30, -10]$, corner 3, vertical gradient, 3 groove lines at $y = -26, -22, -18$.
  4. **Pivot base:** circle $r = 10$ radial gradient, inner ring $r = 6.5$, cap $r = 3.5$, highlight dot at $(-1.2, -1.2)$.
  5. **Arm tube:** cubic path $M(0,6)\ C(0,38)\ (0,62)\ (-3,82)$, 3 stacked strokes (w3.4 `metalDark`, w2.2 `metalMid`, w0.8 `metalLight` shifted $x -0.5$).
  6. **Bearing collar:** path trimmed $t = 0.08..0.14$, stroke 4.6 `metalDark` with 1.0 `metalLight` line.
  7. **Tube end cap:** circle $r = 2.4$ at $E = (-3, 82)$.
  8. **Headshell:** rotated at $E$ by $\text{atan2}(T - E) - 90^\circ \approx -9.5^\circ$, roundRect $10 \times 13$, corner 2, matte black, 1 AU accent stripe.
  9. **Finger lift:** open path $(5,4) \to (12,6) \to (13,1)$, stroke 1.1, `metalMid`.
  10. **Cartridge:** roundRect $7 \times 6$ at $y \in [11, 17]$, corner 1.2, front accent line.
  11. **Stylus:** line $(0,17) \to (0,18.2)$ landing at $T = (0, 100)$, `metalLight`.
- Colors: `metalLight` (`#F2F4F8`), `metalMid` (`#A9AFBA`), `metalDark` (`#4A4E57`), `headshell` (`#17171B`), `cartridge` (`#2A2A30`), theme accent.

---

## 6. Complete Execution Sequence (Ordered Prompts)

### Core v1 (Prompts 0 to N)
1. **Prompt 0:** Project setup (Gradle catalog, Hilt, Room, Media3, Coil, Palette, DataStore, Nav, adaptive icon, splash screen, CI)
2. **Prompt 1:** Design system & nav shell (`IsaiTheme`, tokens, reusable components, type-safe nav graph, previews)
3. **Prompt A:** Tonearm shape & geometry (`TonearmGeometry`, `Tonearm`, `TonearmColors`, `TonearmLabScreen`, tests)
4. **Prompt B:** Tonearm motion & gestures (`TonearmState`, drag-to-seek, lift-to-pause, deny shake, velocity tracking, lab screen with FakePlayer)
5. **Prompt C:** VinylDisc (`VinylDiscState`, spin physics, full-bleed art, grooves overlay, static sheen, `VinylFallbackArt`, lab screen, tests)
6. **Prompt D:** VinylStage assembly (combines disc, tonearm, ambient background, track transitions, gesture arbitration, lab screen)
7. **Prompt E:** Data layer (Room entities, DAOs with FNV-1a 64-bit URI hashes, DataStore `SettingsRepository`, domain models, repositories, tests)
8. **Prompt F:** Onboarding & scanning (SAF folder picker, recursive `DocumentsContract` traversal, parallel `MediaMetadataRetriever`, art downsampling cache, incremental diff, WorkManager)
9. **Prompt G:** Playback engine (`PlaybackService : MediaSessionService`, `PlayerController`, position ticker, `QueueStore`, `PlayHistory`, `SleepTimer`)
10. **Prompt H:** Now Playing integration (wire `VinylStage` to real `PlayerController`, seekbar, control cluster dial, queue bottom sheet, live mini player)
11. **Prompt I:** Home, Library, Detail screens, Search (recent carousel, shuffle card, tabs with fast-scroll, peeking vinyl detail hero, debounced search, track menu)
12. **Prompt J:** Favorites, playlists, queue actions (heart toggle, playlist CRUD & reordering, multi-select picker, sleep timer UI)
13. **Prompt K:** Settings & theming polish (appearance, library, playback, vinyl options, 200% font scale, WCAG AA contrast)
14. **Prompt L:** Hardening & edge cases (permission revocation, corrupt audio, zero duration guards, monkey test, LeakCanary, StrictMode)
15. **Prompt M:** Tests, performance, accessibility (unit/UI test suite, Baseline Profile, Macrobenchmark, TalkBack, semantics)
16. **Prompt N:** Release v1.0.0 (R8 rules, signed AAB, Play store metadata, screenshots, privacy policy)

### Add-on Round 1 (Prompts O to R — v1.1.0)
17. **Prompt O:** Waveform seekbar (MediaCodec PCM decode, 200 buckets, 95th-percentile normalization, Room `WaveformEntity`, BARS/LINE/CLASSIC styles, shared scrub owner)
18. **Prompt P:** Lyrics (Discovery order: Imported > Sidecar `.lrc`/`.txt` > Embedded tags, `LrcParser` with UTF-8/BOM/legacy detection, binary search line indexing, auto-scroll `LyricsPanel`, word highlights, offset sheet, manual import)
19. **Prompt Q:** Home-screen widget (Glance Small/Medium/Large, pre-rendered `WidgetArtRenderer` bitmaps with lowered/lifted arm, `WidgetUpdater` throttled updates, ActionCallbacks with `MediaController`, config screen)
20. **Prompt R:** Add-on QA & Release v1.1.0 (Scrub ownership integration, database migrations, TalkBack a11y, changelog, signed AAB v1.1.0)

### Add-on Round 2 (Prompts S to V — v1.2.0)
21. **Prompt S:** Equalizer and audio effects (`AudioEffectsManager` on ExoPlayer audioSessionId, `Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer`, dynamic device band detection, `EqCurveView` canvas UI, clipping auto-compensation, presets)
22. **Prompt T:** Android Auto / Car & Bluetooth browsing (Migrate service to `MediaLibraryService`, `BrowseTree` with 6 root nodes, paged queries, `ArtworkProvider` content provider, Assistant voice search, car custom layout)
23. **Prompt U:** M3U/M3U8 playlist import & export (`TrackEntity.relativePath`, `PathNormalizer`, `M3uParser`, multi-stage `TrackMatcher` [relative path > suffix > filename + duration > tags], import preview screen, `M3uWriter`)
24. **Prompt V:** Round-2 QA & Release v1.2.0 (Service migration regression, cross-feature matrix, DB migration v1.1.0 -> v1.2.0, store listing update, signed AAB v1.2.0)

---

## 7. Master Antigravity Project Rules (`.agent/rules`)

```
PROJECT: "Isai", an offline local-music player for Android with a vinyl disc and a working tonearm on the Now Playing screen.
STACK: Kotlin, Jetpack Compose, Material 3, Navigation Compose (type-safe routes), Media3 (ExoPlayer, MediaSessionService / MediaLibraryService, MediaController), Room, DataStore (Preferences), Hilt, Coil, Palette, Coroutines/Flow, Glance. Package com.isai.player. minSdk 26, target/compile latest stable.
ARCHITECTURE: MVVM with unidirectional data flow. ViewModels expose StateFlow<UiState>; one-shot events through Channel/SharedFlow. Composables are stateless where possible, with no business logic in UI. PlayerState from PlayerController is the single source of truth for playback UI (no parallel isPlaying flags in the UI).
CORE RULES:
- STRICTLY NO INTERNET PERMISSION. No network code, analytics, or ads. Completely offline.
- No hardcoded strings (strings.xml) or colors (IsaiTheme tokens). Dark and light supported everywhere.
- Per-frame animation uses graphicsLayer lambdas or draw-phase lambdas, never per-frame recomposition.
- Lazy lists use stable keys. Every screen has @Preview for dark and light.
- No new library outside the version catalog without asking.
- Every task: write a plan artifact first and wait for approval, then implement, then run build, lint and tests, then report results (with screenshots for UI) and commit with a clear message.
- Existing components (once approved): Tonearm, TonearmGeometry, TonearmState, VinylDisc, VinylDiscState, VinylStage, WaveformSeekBar, LyricsPanel, GlanceWidget.
ADD-ON & ROUND-2 RULES:
- Allowed new libraries (must justify in plan artifact): androidx.glance (appwidget, material3), and ONE tag-reading library for embedded lyrics if MediaMetadataRetriever cannot read them.
- No DSP library needed (use android.media.audiofx).
- Room schema changes need a proper migration and a committed schema JSON. No destructive migration.
- Every new setting goes into SettingsRepository and the Settings screen, with strings in strings.xml.
- All previously approved behavior (tonearm, disc, scrub ownership, notification, widget, lyrics) must keep working.
```
