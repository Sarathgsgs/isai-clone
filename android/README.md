# Isai (இசை) — Offline Local Music Player for Android

**Isai** is an offline Android music player crafted with Jetpack Compose, Material 3, and AndroidX Media3. Its signature feature is an interactive Now Playing screen showcasing a rotating vinyl disc and a working tonearm that responds to gestures for seeking and needle drops.

---

## Architecture Overview

```
Compose UI (Material 3)
     │
     ▼
ViewModel (StateFlow<UiState>) ──► Repository ──► Room DB / DocumentsContract Scanner / DataStore
     ▲                                   │
     └────────── PlayerController ◄──────┴── MediaController ◄── PlaybackService (ExoPlayer + MediaSession)
```

- **Unidirectional Data Flow**: Strict separation of concerns with immutable state emissions.
- **Single Source of Truth**: `PlayerState` exposed via `PlayerController` dictates all playback and vinyl/tonearm states across the UI.
- **Strictly Offline**: Zero `INTERNET` permission in AndroidManifest.

---

## Tech Stack
- **Language / UI**: Kotlin 2.0, Jetpack Compose, Material 3
- **Audio Engine**: AndroidX Media3 (ExoPlayer 1.4+, `MediaSessionService` / `MediaLibraryService`)
- **Storage / Scanning**: Storage Access Framework (`ACTION_OPEN_DOCUMENT_TREE`) + `DocumentsContract` queries
- **Persistence**: Room 2.6+ with schema migrations, DataStore Preferences
- **Dependency Injection**: Hilt 2.52+
- **Image & Color**: Coil 2.7+, AndroidX Palette
- **Widgets**: Jetpack Glance (Material 3)
- **Static Analysis**: Detekt

---

## Building and Running

### Prerequisites
- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17
- Android SDK 35 (minSdk: 26)

### CLI Commands
```bash
# Run static analysis
./gradlew detekt

# Run unit tests
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```

