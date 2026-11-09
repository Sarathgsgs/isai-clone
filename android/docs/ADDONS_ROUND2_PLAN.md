# Isai: Feature Add-ons — Round 2 (Equalizer, Android Auto, M3U)

## Run order
| Order | Prompt | Depends on | Effort |
|---|---|---|---|
| 1 | **S: Equalizer and audio effects** | G (playback), K (settings) | 3-4 days |
| 2 | **T: Android Auto / car and Bluetooth browsing** | G, E, I, J | 4-5 days |
| 3 | **U: M3U playlist import/export** | E, F, J | 2-3 days |
| 4 | **V: QA and release v1.2.0** | S, T, U | 1-2 days |

---

## Prompt S: Equalizer and audio effects

### Design notes:
- Uses Android system effects (`Equalizer`, `BassBoost`, `Virtualizer`, `LoudnessEnhancer`) attached to ExoPlayer's audio session. No DSP library needed.
- UI: glowing curve drawn over band sliders in accent color.

```
[Paste shared context block + round-2 rules]

TASK: Add an equalizer with presets, custom bands, bass boost, virtualizer and a preamp/loudness
control. Effects live in PlaybackService and survive track changes, process death and audio-session changes.

1) EFFECTS ENGINE (playback/audio)
- AudioEffectsManager (owned by PlaybackService, interface exposed via PlayerController):
  * Attach to exoPlayer.audioSessionId. Re-create on onAudioSessionIdChanged.
  * Effects: Equalizer, BassBoost, Virtualizer, LoudnessEnhancer (preamp/gain).
  * Expose EffectCapabilities based on device detection. Do NOT assume 5 bands or ±15 dB.
  * Preamp range -12 dB to +12 dB with "Prevent clipping" auto-compensation toggle.
  * Presets: Flat, Bass Boost, Bass Reducer, Treble Boost, Vocal, Rock, Pop, Jazz, Classical,
    Electronic, Hip-Hop, Loud, Acoustic, Late Night (normalized log-frequency curves resampled to device bands).
  * System EQ interop: broadcast session actions, option to launch system panel.

2) EQUALIZER SCREEN (ui/equalizer)
- EqCurveView Canvas with Catmull-Rom smooth curve, draggable band handles, haptic tick at 0 dB.
- Vertical band sliders aligned with curve points (0.5 dB steps).
- Bass boost, Virtualizer, Preamp sliders, clipping warning chip.

3) INTEGRATION & TESTS
- Apply before audio starts (no pops). Sleep timer fades volume, not EQ gain.
- Unit tests: log-frequency resampling (5 to 10 bands), clipping compensation math, custom preset CRUD.
```

---

## Prompt T: Android Auto and car/Bluetooth browsing

### Design notes:
- Android Auto uses a templated car UI (browse tree + now-playing + voice search). Tonearm & disc stay on phone.
- Migrates `PlaybackService` to `MediaLibraryService`.

```
[Paste shared context block + round-2 rules]

TASK: Make Isai a media app for Android Auto (and generic MediaBrowser clients such as Bluetooth car
displays and Google Assistant), with a browsable library, voice search, playback resumption and custom actions.

1) SERVICE MIGRATION
- PlaybackService extends MediaLibraryService with MediaLibrarySession.Callback.
- Manifest: mediaPlayback type, MediaLibraryService and MediaBrowserService intent filters, automotive_app_desc.xml.
- Controller verification: trusted clients (Android Auto, System UI, Assistant) get full features, untrusted get read-only.

2) BROWSE TREE (playback/library/BrowseTree)
- Root nodes (4-6 max): Recently played (20 tracks), Favorites (max 200), Playlists, Albums (GRID), Artists (GRID), Songs (LIST with Shuffle All).
- Paged loading (default 50 items) off main thread.

3) ARTWORK FOR CARS
- ArtworkProvider content:// provider serving cached 512px JPEGs, path traversal rejection guard.

4) VOICE AND SEARCH
- Google Assistant voice intents ("Play <song/artist/album> on Isai").
- Search query resolution (artist -> all songs, album -> in order, empty -> shuffle all).

5) CAR CUSTOM ACTIONS & DHU TESTING
- Car screen custom layout: Shuffle, Repeat, Favorite.
- Desktop Head Unit (DHU) test documentation in docs/android-auto.md.
```

---

## Prompt U: M3U playlist import and export

### Design notes:
- SAF provides document URIs; imports are matched by relative path first, then filename, then tags.

```
[Paste shared context block + round-2 rules]

TASK: Let users export playlists (and favorites and the queue) to .m3u/.m3u8 files, and import
M3U/M3U8 files into new or existing playlists, with robust matching to the scanned library.

1) DATA PREPARATION
- TrackEntity.relativePath and relativePathNorm columns with Room migration.
- PathNormalizer: separator normalization (\ to /), URL decoding, Windows drive stripping.

2) PARSER (data/m3u/M3uParser)
- .m3u8 (UTF-8) and .m3u (UTF-8 / ISO-8859-1 fallback).
- #EXTM3U, #EXTINF, #PLAYLIST tags, stream line reading (max 5 MB / 50k entries).

3) MATCHER (data/m3u/TrackMatcher)
- Multi-stage matching:
  1. Exact relativePathNorm match
  2. Suffix match (trailing path segments)
  3. Filename match with EXTINF duration disambiguation (±2 s)
  4. Tag match (artist/title/duration ±3 s)

4) IMPORT & EXPORT UX
- Import preview screen with match breakdown (matched, ambiguous, not found), manual "Find" picker, merge/create options.
- Export writer (.m3u8 UTF-8 / .m3u legacy), relative path style, FileProvider share action.
- Round-trip compatibility tests with foobar2000, VLC, MusicBee, Poweramp fixtures.
```

---

## Prompt V: Round-2 QA and release (v1.2.0)

```
[Paste shared context block + round-2 rules]

TASK: Integrate, regression-test and ship Equalizer, Android Auto and M3U support as Isai v1.2.0.

- Service migration regression: verify MediaLibraryService doesn't break notification, lock screen, widgets, or tonearm sync.
- Cross-feature matrix: EQ active in car, Android Auto favorite updates phone/widget, M3U imported playlist in car browse tree.
- DB migration v1.1.0 -> v1.2.0.
- Macrobenchmarks, TalkBack, no INTERNET permission verification.
- Bump v1.2.0, tag, signed AAB.
```

