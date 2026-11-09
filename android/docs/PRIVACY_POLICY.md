# Privacy Policy for Isai Music Player

**Last Updated:** November 6, 2026  
**Effective Date:** November 6, 2026

## 1. Introduction

**Isai** ("we", "our", or "the app") is a standalone, local offline audio player designed for Android. We believe privacy is a fundamental human right. Isai was built from the ground up under a strict **Zero-Network Architecture**: the application does not have internet access, does not collect any user data, does not include analytics or advertising SDKs, and will never transmit your data anywhere.

---

## 2. Zero Internet Permission Guarantee

Isai does **NOT** declare or request the `android.permission.INTERNET` permission in its Android Manifest. 

Because the operating system does not grant network access to the application, it is technically impossible for Isai to:
- Connect to any remote server or API.
- Transmit telemetry, analytics, or crash logs.
- Download ads, trackers, or third-party scripts.
- Upload your music files, metadata, listening habits, or device identifiers.

---

## 3. Storage and File Access (Storage Access Framework)

Isai requires access to audio files stored on your device to build your local music library and play audio.

- **Scoped Directory Access**: Isai uses Android's modern **Storage Access Framework (SAF)**. When you first launch the app, you explicitly choose which music directory Isai is allowed to read.
- **Read-Only Audio Processing**: Isai only reads audio metadata (tags, album art, duration) and audio streams from the directory you select.
- **No File Modifications**: Isai does not alter, rename, delete, or upload your physical audio files.
- **Revocable Permission**: You can revoke folder access at any time through Android System Settings or by changing the music folder in Isai Settings.

---

## 4. On-Device Data Storage

All data generated while using Isai remains exclusively on your physical device in private application sandboxes:

1. **Music Library Index**: Cached metadata (song titles, artists, album names, durations, track numbers) is stored in a local SQLite database (powered by Android Room).
2. **Playlists & Favorites**: Playlists you create and songs you favorite are stored locally on your device.
3. **Playback History**: Recent play timestamps and counts (used strictly for the on-device "Recently Played" home carousel) are stored locally.
4. **Artwork, Waveforms & Lyrics**: Downsampled album artwork, hardware-extracted audio waveform peaks, and parsed offline lyrics are stored entirely in the application's private internal storage and local SQLite database for rapid on-device rendering.
5. **Preferences**: Settings such as theme (Dark, Light, System), dynamic accent colors, tonearm needle-lift behavior, waveform seekbar style, and sleep timer choices are stored in local Jetpack DataStore Preferences.

All local data is automatically purged if you clear the app's storage in Android Settings or uninstall the app.

---

## 5. Third-Party Libraries and SDKs

Isai utilizes strictly offline, open-source Android Jetpack and community libraries:
- **AndroidX Media3 (ExoPlayer)**: For local audio decoding and playback sessions.
- **AndroidX Room & DataStore**: For local on-device database and preference storage.
- **AndroidX Jetpack Compose**: For user interface rendering.
- **Google Dagger Hilt**: For dependency injection.
- **Coil**: For loading local image files and embedded album art.
- **AndroidX Palette**: For on-device color extraction from album art.

**No proprietary analytics, advertisement networks, or remote tracking frameworks (e.g., Firebase, Google Analytics, Facebook SDK) are included in Isai.**

---

## 6. Children's Privacy

Isai does not collect, store, or transmit personal information from anyone, including children under the age of 13.

---

## 7. Changes to This Privacy Policy

Because Isai is completely offline and does not collect data, our data practices do not change. If any future architectural modifications occur, this document will be updated and released with version notes.

---

## 8. Open Source & Verification

Isai is committed to full transparency. Users and developers are encouraged to inspect the application package (APK/AAB) or examine the Android Manifest to independently verify the complete absence of network permissions.

For questions regarding this policy, contact the development team at `support@isai.player`.

