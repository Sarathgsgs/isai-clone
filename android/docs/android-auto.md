# Android Auto & Automotive Media Guide — Isai

This guide provides instructions for configuring, testing, and debugging **Isai** on **Android Auto** head units and the official Google **Desktop Head Unit (DHU)** simulator.

---

## 1. Architecture Overview

Isai implements AndroidX Media3's **`MediaLibraryService`** (`com.isai.player.playback.PlaybackService`), enabling automotive dashboards, steering wheel controls, and Google Assistant voice commands while maintaining strict 100% offline privacy.

### Core Automotive Components:
- **`MediaLibrarySession`**: Replaces the standard `MediaSession`, providing a hierarchical media browse tree and search callbacks.
- **`MediaBrowseTree`**: Exposes 6 root nodes:
  - **Recently Played**: Last 20 tracks (List format)
  - **Favorites**: Starred tracks (List format)
  - **Playlists**: Custom user playlists (List format)
  - **Albums**: Grid format with album cover artwork
  - **Artists**: Grid format with discography
  - **All Songs**: List format with a primary "Shuffle All" action
- **`CarArtworkProvider`**: Secure `ContentProvider` (`content://com.isai.player.artwork/<trackId>`) serving cached 512px JPEG artwork with strict path-traversal guards.
- **`PackageValidator`**: Restricts sensitive queue modification to trusted callers (e.g. `com.google.android.projection.gearhead`, Google Assistant, System UI).
- **`VoiceSearchResolver`**: Maps natural voice queries ("Play Abbey Road on Isai", "Play The Beatles on Isai") into targeted track queues or shuffle playback.

---

## 2. Desktop Head Unit (DHU) Testing Setup

The Desktop Head Unit (DHU) emulates an Android Auto infotainment head unit on your development machine.

### Prerequisites:
1. **Android SDK**: Install the **Android Auto Desktop Head Unit emulator** package via Android Studio SDK Manager (`SDK Tools > Android Auto Desktop Head Unit emulator`).
   - Located at: `<Android-SDK>/extras/google/auto/desktop-head-unit`
2. **Android Device**: A physical phone or emulator running Android 8.0+ (API 26+) with the **Android Auto** app installed.

### Step-by-Step Setup:
1. **Enable Android Auto Developer Mode on Device**:
   - Open Android System Settings $\to$ Apps $\to$ Android Auto (or open Android Auto app).
   - Scroll to **Version** and tap it 10 times until "Developer mode enabled" appears.
   - Tap the three-dot menu $\to$ **Developer settings**.
   - Enable **"Unknown sources"** (allows testing debug builds of Isai).
   - In the three-dot menu, select **"Start head unit server"**.

2. **Connect Device and Forward Port**:
   - Connect your phone via USB with USB Debugging enabled.
   - Run in your terminal:
     ```bash
     adb forward tcp:5277 tcp:5277
     ```

3. **Launch Desktop Head Unit (DHU)**:
   - On Windows:
     ```powershell
     cd "$env:LOCALAPPDATA\Android\Sdk\extras\google\auto"
     .\desktop-head-unit.exe
     ```
   - On macOS / Linux:
     ```bash
     cd ~/Library/Android/sdk/extras/google/auto
     ./desktop-head-unit
     ```

---

## 3. Verification Checklist

| Scenario | Expected Behavior |
|---|---|
| **App Discovery** | Isai icon appears on the Android Auto app launcher screen. |
| **Media Tree Browsing** | Selecting Isai displays 6 tabs/nodes: Recently Played, Favorites, Playlists, Albums, Artists, Songs. |
| **Grid vs List Rendering** | Albums and Artists display as an image grid; Songs and Playlists display as a list. |
| **Playback** | Tapping any track starts playback immediately and updates now-playing metadata on the car screen. |
| **Artwork Delivery** | High-resolution album artwork appears on the head unit background and mini-card via `CarArtworkProvider`. |
| **Shuffle All** | Tapping "Shuffle All" in the Songs section randomizes library playback. |
| **Steering Wheel Controls** | Next, Previous, and Play/Pause hardware controls function seamlessly. |
| **Voice Command** | "Hey Google, play <song> on Isai" resolves and begins playing without phone interaction. |
| **Audio Focus** | Navigational voice prompts (e.g., Google Maps) duck or pause Isai audio and resume smoothly. |

