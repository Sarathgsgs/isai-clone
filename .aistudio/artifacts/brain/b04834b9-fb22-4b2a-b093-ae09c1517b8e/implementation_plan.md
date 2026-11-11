# Download Folder Access, Multi-Directory Library, Tonearm Touch Resume & Playcursor Scrubbing

This implementation delivers full support for selecting folders or individual tracks, managing multiple music directories simultaneously, making the turntable tonearm toggle between pause and resume upon touch, and eliminating playcursor drag stuttering with unified pointer capture and local scrubbing state.

## User Decisions & Implemented Workflow

> [!IMPORTANT]
> **Consolidated 2-Option Selector Implemented:**
> - The Add Music modal renders **strictly two options**:
>   1. **Select Folder**: Uses `<input webkitdirectory />`. Allows choosing any folder on device (including Downloads, Music albums, etc.). When selected, it automatically reads the folder name, adds the tracks, switches to the folder view, and immediately starts playback on the turntable.
>   2. **Select Separate Music Tracks**: Uses `<input type="file" multiple accept="audio/*" />` to pick one or more individual audio files from anywhere on the device.
> - No separate "Downloads" option or "System" badge is shown. All folders are treated uniformly and can be removed via the trash button.
> - **Tonearm Touch Behavior**: Touching or tapping anywhere on the tonearm toggles playback: pausing if playing, and resuming if paused.
> - **Playcursor Scrubbing**: Uses pointer capture (`setPointerCapture`) and decoupled scrub state so rapid dragging never stutters or gets stuck.

---

## 1. Overview & Core Concept

- **What It Does**: Enhances the Isai music player with friction-free audio directory importing (specifically overcoming the browser's Downloads folder restriction), enables organizing music across multiple separate local folders, restores natural touch-to-resume responsiveness to the turntable tonearm, and ensures buttery-smooth 60fps playcursor scrubbing.
- **Target Audience / Persona**: Music enthusiasts and offline listeners who download songs regularly to their device's Downloads directory and store albums across multiple folders on local storage.
- **Key Value**: Eliminates browser permission hurdles for downloaded audio, gives full multi-folder library organization, and ensures the analog turntable and seekbar feel immediate, tactile, and bug-free.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Importing Audio from Downloads or Any Folder**:
   - The user visits the **Folders** tab or clicks the folder icon in the top bar.
   - The Folders tab presents configured music directories (e.g. `Downloads`, `Music`, `Vinyl Rips`).
   - Clicking **"+ Add Folder"** opens a clean chooser modal offering:
     - **"Select Files from Downloads"**: Opens file picker directly targeting audio files without browser security errors.
     - **"Choose Folder Directory"**: Opens folder picker using `webkitdirectory`.
   - Imported tracks are tagged with their source folder name, and newly added directories appear immediately in the Folders list without removing existing tracks.

2. **Browsing Multi-Folder Directories**:
   - In the Folders tab, each directory card displays a folder icon, folder name, track count, total duration, and a badge.
   - Tapping a folder card opens that folder's track list with a top bar showing the folder name and a "Back to Folders" button (mirroring the Samsung One UI Albums experience).

3. **Tonearm Touch to Pause & Resume**:
   - When music is playing, touching/tapping the tonearm pauses playback and gently lifts the needle.
   - When music is paused, touching/tapping the tonearm drops the needle back into the groove and resumes playback instantly.
   - If the tonearm is parked in its rest cradle, tapping it swings the needle onto the track and begins playback.
   - Dragging the tonearm continues to seek track depth and needle drop angle.

4. **Smooth Playcursor Scrubbing**:
   - The user grabs the seekbar thumb or waveform playcursor.
   - As the finger or mouse drags horizontally, the thumb tracks 1:1 with zero lag, zero snapping, and zero jitter.
   - The timestamp tooltip updates smoothly in real time.
   - Upon release, the audio seamlessly seeks to the chosen timestamp.

### Visual Identity & Theme (Samsung One UI Hi-Fi)

- **Aesthetic**: AMOLED true black (`#000000`), subtle deep charcoal plinth (`#101014`), amber-orange accents (`#ff8a3d` / `hsl(28, 100%, 62%)`).
- **Typography**: Clean One UI sans-serif layout with monospaced timestamps (`font-mono`) and bold category headers.
- **Touch Target Ergonomics**: Tonearm hit area expanded across the entire arm tube and cartridge with minimum 44px touch targets for mobile thumb usability.
- **Seekbar Scrubber**: High-contrast white accent thumb with amber glow during active scrubbing, with `touch-none` preventing mobile browser scroll interference.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Overcoming the Browser "Downloads" Folder Security Block
- **Chosen Approach**: Dual import strategy using standard `<input type="file" multiple accept="audio/*">` for Downloads/files and `<input type="file" webkitdirectory directory multiple>` for folders, combined with clear in-app guidance.
- **Why**: The W3C File System Access API (`window.showDirectoryPicker`) deliberately forbids web apps from selecting user Downloads directories on Chromium (`SecurityError`). An `<input type="file">` does not suffer from this restriction and allows selecting files inside Downloads.
- **Alternatives Considered**: Forcing only `showDirectoryPicker()`, which crashes on Downloads; or requiring server uploads, which violates the 100% offline local privacy design.

### Decision 2: Multi-Directory State Architecture
- **Chosen Approach**: Store a persistent registry of `MusicFolder` records (`{ id, name, path, trackCount, addedAt }`) and associate each track with a `folderId` / `folderPath`.
- **Why**: Allows users to accumulate music from multiple distinct folders (e.g. `Downloads` + `SD Card` + `External Drive`) incrementally rather than resetting the library on each import.
- **Alternatives Considered**: Overwriting tracks on every new folder selection (the previous limitation).

### Decision 3: Tonearm Touch State & Click Discrimination
- **Chosen Approach**: Measure pointer travel distance between pointerdown and pointerup. If travel < 6px, treat as a tap/click and execute `togglePlayResume()`; if travel >= 6px, treat as a manual needle position drag.
- **Why**: Resolves the bug where `isDragging` was set to true on `pointerdown`, permanently blocking the `onClick` handler from firing and preventing resume.

### Decision 4: Playcursor Scrubbing Decoupling
- **Chosen Approach**: Maintain local `isScrubbing` and `scrubbingTime` state in `WaveformSeekBar` and `NowPlayingView`. While pointer is down, ignore external `currentTime` ticks from `AudioEngine`'s `timeupdate` and capture all pointer movement via `setPointerCapture`.
- **Why**: Prevents incoming audio time updates from fighting the user's drag gesture, eliminating cursor freezing and snap-back.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        App State & Library                             │
│                                                                        │
│   • tracks: Track[] (each with folderId, folderPath, uri, etc.)        │
│   • folders: MusicFolder[] (Downloads, Music, Album Folders)           │
│   • activeTab: 'folders' -> folder view or directory drill-down        │
└───────────────────▲────────────────────────────────▲───────────────────┘
                    │                                │
    ┌───────────────┴───────────────┐  ┌─────────────┴─────────────┐
    │     Directory / File Input    │  │     Interactive Stage     │
    │                               │  │                           │
    │ • Multi-file input (Downloads)│  │ • TurntableStage          │
    │ • webkitdirectory input       │  │ • Tonearm (Tap -> Resume) │
    │ • Folder tagging & merge      │  │ • VinylDisc (Scratch)     │
    └───────────────────────────────┘  └─────────────┬─────────────┘
                                                     │
                                       ┌─────────────▼─────────────┐
                                       │    Unified Seekbar Hub    │
                                       │                           │
                                       │ • Decoupled scrub state   │
                                       │ • setPointerCapture       │
                                       │ • AudioEngine.seek()      │
                                       └───────────────────────────┘
```

### Component & File Mapping

1. **`src/types/index.ts`**:
   - Add `MusicFolder` interface:
     ```typescript
     export interface MusicFolder {
       id: string;
       name: string;
       path?: string;
       trackCount: number;
       addedAt: number;
     }
     ```
   - Ensure `Track.folderId` and `Track.folderPath` are fully typed.

2. **`src/App.tsx`**:
   - Maintain `folders: MusicFolder[]` state and `selectedFolderId: string | null`.
   - Add dual file inputs:
     - `fileInputRef`: `<input type="file" multiple accept="audio/*" />` for Downloads/batch files.
     - `folderInputRef`: `<input type="file" webkitdirectory directory multiple />` for whole directories.
   - Enhance `handleProcessAudioFiles` to tag each track with source folder name and update/create corresponding `MusicFolder` records.
   - In `Folders` tab:
     - When no folder is selected: render list of folder cards with track counts, plus an **"+ Add Folder / Downloads"** button.
     - When a folder is clicked: render only tracks belonging to that folder, with a back button to return to folder list.

3. **`src/components/turntable/Tonearm.tsx` & `TurntableStage.tsx`**:
   - Expand tonearm interactive hit area across the arm tube (`pointer-events-auto`) and headshell.
   - Add `startPointerPos` tracking and pointer capture.
   - In `handlePointerUp`: if `totalMovedDistance < 6px`, fire `onTogglePlay()` regardless of previous drag state.
   - In `TurntableStage`: ensure that if tonearm is at rest cradle (`progress === 0 && !isPlaying`), tapping the tonearm lifts it onto the record and triggers `onTogglePlay()` to start playing immediately.

4. **`src/components/player/WaveformSeekBar.tsx` & `NowPlayingView.tsx`**:
   - Add `setPointerCapture` to container on `pointerdown` and `releasePointerCapture` on `pointerup`.
   - Maintain `localScrubTime: number | null` state while scrubbing.
   - Render the thumb and progress bar from `localScrubTime ?? currentTime`.
   - Commit seek to audio engine on pointer move (throttled) and on pointer up.
   - Add `touch-none` styling to prevent touch scroll cancellation.

---

## 5. Verification & Testing Steps

1. **Download Folder Selection Test**:
   - Trigger file/folder import and select files directly from Downloads folder.
   - Confirm audio tracks load into the library and are automatically categorized under a "Downloads" folder card.
2. **Multi-Folder Accumulation Test**:
   - Add a first folder, verify tracks appear.
   - Add a second folder, verify tracks from both folders coexist without overwriting.
   - In Folders tab, verify navigating into Folder 1 and Folder 2 shows only respective tracks.
3. **Tonearm Touch-to-Resume Test**:
   - Play audio -> Tap tonearm -> Verify playback pauses.
   - While paused -> Tap tonearm -> Verify playback immediately resumes.
   - While stopped at rest cradle -> Tap tonearm -> Verify tonearm swings to record and playback starts.
   - Drag tonearm needle across record -> Verify smooth seeking.
4. **Playcursor Scrubbing Test**:
   - Grab seekbar playcursor and drag rapidly back and forth across 0% to 100%.
   - Move pointer above and below the seekbar container while dragging.
   - Verify playcursor follows finger/mouse smoothly without freezing, snapping back, or getting stuck.
5. **Compilation & Linting**:
   - Execute `compile_applet` and `lint_applet` to confirm zero TypeScript or React compilation errors.

---

## 6. Unified Web-to-APK Production Packaging

### Cause of UI Discrepancy in APK
- AI Studio live preview runs the React + Tailwind + Vite application (`src/App.tsx`) with Samsung One UI tabs, sorting, Tamil demo tracks, realistic turntable plinth, tonearm physics, and lyrics.
- In previous builds, the GitHub Actions workflow built a separate, unfinished native Kotlin Jetpack Compose UI (`HomeScreen.kt`) that had an empty Room database ("Your Library: 0 Songs") and bottom tabs ("Home | Library | Favorites | Search").

### Implemented Fix
1. **Full-Fidelity WebView Host (`MainActivity.kt`)**:
   - `MainActivity.kt` now hosts the compiled React application directly in a hardware-accelerated, full-screen WebView with AMOLED `#121212` background.
   - Built assets (`dist/`) are staged into `android/app/src/main/assets/dist/`.
   - `WebViewClient.shouldInterceptRequest` handles `https://isai.local/` directly from APK assets, enabling 100% offline playback, standard ES Modules (`<script type="module">`), Web Audio API, and persistent local storage.
   - Native file chooser (`onShowFileChooser`) connects to Android's system audio file picker for uploading personal tracks.
   - Physical/gesture back button support via `window.__isaiGoBack` closes open sheets, modals, and the Now Playing view natively before exiting.
2. **Automated GitHub Actions Pipeline (`.github/workflows/build-apk.yml`)**:
   - Node.js 22 builds the exact web app bundle (`npm run build`).
   - Copies bundle to `android/app/src/main/assets/dist/`.
   - Gradle compiles and releases `app-debug.apk` directly matching the live preview.
3. **Verification**:
   - Web application builds cleanly (`compile_applet` & `lint_applet` passing with 0 errors).
   - Project archive bundled at `/public/assets/aistudio/Isai-complete-app.zip`.

---

## 7. Call Recording & Custom Track Prefix Exclusion

### Feature Overview
When scanning local storage directories (e.g. phone storage or Downloads), call recordings and voice memos often clutter the music library (e.g., `Call@...`, `Call recording...`, `REC_...`). An interactive configuration now enables users to exclude any track whose title or filename begins with specified prefixes.

### Implementation Details
1. **Configurable Exclusion State**:
   - Added `excludePrefixesEnabled: boolean` and `excludePrefixes: string[]` (pre-filled with `['Call']` as a default) to `PlayerSettings`.
   - Persisted across launches via `localStorage`.
2. **Settings Modal (`SettingsModal.tsx`)**:
   - Dedicated **"Exclude Call Recordings & Prefixes"** section with an instant ON/OFF toggle switch.
   - Text input allowing users to enter custom prefixes (`Call`, `REC_`, `Voice`, etc.) with an **Add** button and Enter key support.
   - Active filter tag chips with 1-click removal (`×`).
   - Quick preset buttons (`+ Call`, `+ REC`, `+ Recording`, `+ Voice`, `+ AUD-`).
   - Live count of avoided tracks with an expandable "View excluded tracks" drawer displaying titles and source paths.
3. **Library Integration**:
   - `isTrackExcluded(track)` checks if `title` or `filename` starts with any configured prefix (case-insensitive).
   - `filteredTracks`, `albums`, `artists`, `foldersWithCounts`, and auto-play queues dynamically avoid all excluded tracks.
   - Samsung Music subheader displays a subtle indicator when tracks are excluded (e.g., `🛡️ X excluded`) with a link to manage them.
   - **Folders Tab & Add Music Modal**: Include status banners and quick-access "Change" buttons.

---

## 8. Android SAF Folder Picker & Status Bar Inset Fixes

### 1. Android SAF Folder Selection ("USE THIS FOLDER")
- **Problem**: In previous builds, tapping "Select Folder" on Android opened a single-file document chooser rather than the folder picker. The system "USE THIS FOLDER" button was missing, forcing the user to pick only one song.
- **Solution**:
  - Implemented `ActivityResultContracts.OpenDocumentTree()` in `MainActivity.kt`.
  - Added `IsaiAndroid.openFolderPicker()` and `IsaiAndroid.openFilePicker()` via a native `@JavascriptInterface` bridge.
  - When the user taps **Select Folder**, Android launches `ACTION_OPEN_DOCUMENT_TREE`, displaying the native Android folder browser with the wide **"USE THIS FOLDER"** button at the bottom.
  - Recursively scans audio files within the selected directory tree, extracts metadata, filters out call recordings, and passes the entire directory to the player.
  - Added `content_stream` proxy handler in `MainActivity.kt` to stream audio directly from content URIs.

### 2. Status Bar & Notification Overlap
- **Problem**: Edge-to-edge mode pushed the app header directly beneath the phone's clock, SIM network, and battery icons.
- **Solution**:
  - Added `ViewCompat.setOnApplyWindowInsetsListener(isaiWebView)` in `MainActivity.kt` to dynamically pad the WebView content down by the exact status bar height (`insets.getInsets(Type.statusBars()).top`).
  - Added safe area viewport tags and increased top header padding (`pt-6 pb-2`) in `App.tsx` and `NowPlayingView.tsx`.

### 3. Build Hardening
- Preserved `package-lock.json` with `--legacy-peer-deps`.
- Hardened `.github/workflows/build-apk.yml` with error-tolerant fallbacks, Node.js 22, and `workflow_dispatch` release triggers.
