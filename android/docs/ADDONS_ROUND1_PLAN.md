# Isai: Feature Add-ons — Round 1 (Waveform Seekbar, Lyrics, Widget)

These continue the numbering from the main plan. Run them after Prompt H at the earliest (they need the real Now Playing screen). Running them after N is safest.

## Run order
| Order | Prompt | Depends on | Effort |
|---|---|---|---|
| 1 | **O: Waveform seekbar** | E (Room), G (controller), H (Now Playing) | 3–4 days |
| 2 | **P: Lyrics** | E, F (scanner), G, H | 4–5 days |
| 3 | **Q: Home-screen widget** | G, H, K (settings) | 3–4 days |
| 4 | **R: Add-on QA and release update** | O, P, Q | 1–2 days |

---

## Prompt O: Waveform seekbar

### Design notes from mockups:
- Mockups 1 and 2 show a thin progress line over a faint waveform silhouette.
- Mockup 3 (the light theme) shows a bar-style waveform that fades from cyan to purple.
- Isai supports both looks as a style option, plus the classic line as a fallback.

```
[Paste shared context block + add-on rules]

TASK: Replace the plain seek bar on Now Playing with a real waveform seekbar generated from
each track's audio. Waveforms are computed on-device, cached, and kept in sync with the tonearm.

1) WAVEFORM GENERATION (data/waveform)
- WaveformExtractor.extract(trackUri, bucketCount = 200): ByteArray? (one amplitude byte, 0..255, per bucket)
  * Use MediaExtractor + MediaCodec to decode to PCM (16-bit). Select the first audio track.
  * Downmix to mono by averaging channels. For each bucket compute the peak of |sample| (or RMS,
    and pick one with a note on why in the plan). Buckets are defined by TIME (duration / bucketCount),
    not by sample count, so the result is correct for any sample rate.
  * Normalize: scale by the 95th percentile (not the absolute max) so one loud spike doesn't flatten
    the rest, then apply a mild gamma (about 0.7) so quiet parts stay visible. Clamp to 0..255.
  * Performance: for long files use a decode budget. If the duration is over 12 minutes, decode every Nth
    frame region or skip ahead using seekTo and decode short windows (document the approach). Reuse
    ByteBuffers, and release codec and extractor in finally blocks. Cancel promptly on coroutine
    cancellation (check isActive between buffers).
  * Run on a bounded dispatcher (1 worker, limitedParallelism(1)) so it never competes with playback.
  * If duration is unknown, decoding fails or the format is unsupported, return null.
- WaveformRepository:
  * Room table WaveformEntity(trackId PK FK cascade, bucketCount, peaks BLOB, version, createdAt).
    Bump the DB version with a migration and commit the schema JSON. A WAVEFORM_VERSION constant lets
    us invalidate the cache if the algorithm changes.
  * observeWaveform(trackId): Flow<WaveformState> = Loading | Ready(peaks) | Unavailable.
  * Strategy: when a track becomes current, generate if missing (high priority). Pre-generate the next
    track in the queue (low priority, only when playing and not in battery saver). A "Generate for all
    songs" optional background job (WorkManager, charging and idle constraints) is OFF by default and
    appears in Settings.
  * Never regenerate for the same track in parallel (a per-track mutex or in-flight map).
  * When tracks are purged or marked missing, the cascade removes the waveform. Also delete the rows
    when lastModified changes (the file was edited).
- Fallback while loading or unavailable: a deterministic pseudo-waveform seeded from the track id (smooth
  noise, clearly "calm", at 35% alpha) that crossfades to the real waveform over 300 ms when ready.
  It must never look broken or jump.

2) WAVEFORM SEEKBAR COMPONENT (ui/components/WaveformSeekBar.kt)
@Composable fun WaveformSeekBar(
    peaks: ByteArray?,              // null = use the fallback
    progress: () -> Float,          // lambda read in the draw phase (no recomposition on ticks)
    bufferedProgress: () -> Float,  // optional faint buffered marker
    style: SeekBarStyle,            // WAVEFORM_BARS, WAVEFORM_LINE (silhouette like mockups 1 and 2), CLASSIC
    colors: SeekBarColors,          // played, unplayed, thumb, gradient pair for the bars style
    enabled: Boolean,
    onScrubStart: () -> Unit,
    onScrubPreview: (Float) -> Unit,
    onScrubEnd: (commit: Boolean, progress: Float) -> Unit,
    modifier: Modifier = Modifier)

- Drawing (drawWithCache, with a path or bar rects built once per size and peaks change):
  * BARS: rounded bars (width about 3dp, gap about 2dp). The bar count is derived from the width and
    resampled from the 200 stored buckets (max-pool when downsampling, linear interpolation when upsampling).
    Played bars use the accent, and unplayed bars use onSurfaceVariant at 35% alpha. Optional gradient
    across the width as in mockup 3. Height is min 12% .. max 100% of the component height (about 56dp),
    centered vertically and mirrored around the center line.
  * LINE (mockups 1 and 2): a smooth mirrored silhouette Path (quadratic smoothing) at 25% alpha with a
    2dp progress line and played-region clip in the accent.
  * CLASSIC: a plain track and a thumb (the fallback and the reduced-motion choice).
  * Played versus unplayed is a clipRect on the progress x. The bar passing the playhead gets a small
    scale-up "pop" (cheap, optional). A thumb or playhead line (2dp) is always visible, and it grows on touch.
- Interaction:
  * Tap and drag horizontally to scrub, with the same callbacks as the tonearm (beginScrub, previewScrub,
    endScrub on the PlayerController), so the tonearm moves while the user drags the waveform, and the
    waveform moves while the user drags the arm. They must never fight: during any scrub only the active
    control owns the preview value (a shared scrub owner in NowPlayingViewModel: NONE, ARM, BAR).
  * While dragging, show a floating time bubble above the playhead (mm:ss), and haptic ticks at about every 5%
    (honor the haptics setting).
  * Horizontal drags must not trigger the disc swipe or system back gestures (use a
    system-gesture-exclusion rect for the bar's horizontal area if needed). Provide a vertical-slop rule: a drag
    that goes mostly vertical is canceled and not committed.
  * Long-press and drag is not required. A single tap seeks directly (with the arm animating to the new groove).
- Accessibility: slider semantics (progressBarRangeInfo, a stateDescription "1:23 of 3:37"),
  custom actions "Forward 10 seconds" and "Back 10 seconds", plus an optional TalkBack-friendly fallback to
  CLASSIC if the user has TalkBack on (setting default: waveform, but the semantics are always present).
- Reduced motion: no pop and no crossfade, just a swap.

3) INTEGRATION
- NowPlayingViewModel exposes waveform: StateFlow<WaveformState> for the current track (switchMap on track id).
- Replace the Prompt H seek bar with WaveformSeekBar in the same position. Keep the elapsed and remaining labels.
- SeekBarStyle setting (Waveform bars / Waveform line / Classic) in SettingsRepository and in Settings > Appearance,
  with a live preview tile. The default for dark is the LINE style (matches mockups 1 and 2), and for light it is BARS
  (matches mockup 3), but the user choice overrides it once set.
- The mini player keeps its hairline progress (no waveform there).
- Zero-duration or unknown-duration tracks: the bar is disabled, with the playhead at 0 and no NaN in any math.
- Settings > Library: "Waveform cache" with its size, "Clear waveform cache", and the optional "Pre-generate waveforms
  while charging".

4) TESTS
- Unit: bucketing by time (a synthetic PCM sine and a step signal give the expected peaks), percentile
  normalization, resampling (max-pool and interpolation), progress to x and x to progress mapping with clamping, the
  fallback determinism.
- Extractor on a short bundled test asset in the formats mp3, m4a and flac (Robolectric may not decode, so
  make these instrumented), plus a corrupt file returns null without a crash.
- Repository: a cache hit avoids decoding, version invalidation, cascade delete, and no duplicate in-flight generation.
- Compose tests: a drag calls scrubStart, preview and end in order. A tap seeks. A disabled bar ignores input.
  Scrub ownership: dragging the bar moves the arm, and dragging the arm moves the bar.

ACCEPTANCE
- A waveform appears within about 2 s for a typical 4-minute MP3 on a mid-range device. Playback does not stutter while generating.
- Next-track waveforms are usually ready before the track starts.
- Dragging the waveform moves the tonearm in real time, and the final seek lands within 1% of the touch position.
- Zero recompositions of the waveform composable on position ticks, and 60 fps.
- All three styles look right in dark and light themes, and previews exist for each.
- The DB migration is verified from the previous schema version with real data.
```

---

## Prompt P: Lyrics

### Design decisions:
- Lyrics gets its own toggle inside the Now Playing screen (switching between VinylStage and LyricsPanel with a 300ms crossfade and slide).
- Lyrics are offline only: embedded tags, `.lrc` files next to the song, and manual import. Strictly zero network access.

```
[Paste shared context block + add-on rules]

TASK: Add lyrics support: discovery (embedded and sidecar .lrc), parsing (synced and plain), a
Now Playing lyrics view with auto-scroll and tap-to-seek, manual import, and offset adjustment.

1) LYRICS SOURCES AND DISCOVERY (data/lyrics)
Priority order per track:
  a. User-imported lyrics (stored by Isai, takes priority because the user chose it)
  b. Sidecar file: same folder, same base name, extension .lrc (case-insensitive), also .txt as plain text.
     Found through the SAF tree (DocumentsContract child query by display name inside the track's parent
     directory). During the scan (Prompt F's scanner) record the sidecar URI if present (add nullable
     lyricsUri and hasLyrics columns to TrackEntity with a migration), and a cheap check on rescan.
  c. Embedded lyrics in the tags: ID3v2 USLT (unsynced) and SYLT (synced) for MP3, the "LYRICS" or "UNSYNCEDLYRICS" Vorbis
     comments for FLAC and OGG, and the ©lyr atom for M4A.
     * First check whether MediaMetadataRetriever exposes it (it generally doesn't). If not, the plan artifact
       must evaluate lightweight options (a small Kotlin ID3 or tag reader versus a JNI TagLib wrapper), their APK size
       impact and license, and ask me to approve ONE before adding it.
     * Read embedded lyrics lazily when a track is played (not during the scan, to keep scans fast), then cache.
  d. None: show the empty state.
- Storage: Room LyricsEntity(trackId PK FK cascade, source: IMPORTED|SIDECAR|EMBEDDED, type: SYNCED|PLAIN,
  content TEXT, offsetMs INT default 0, updatedAt).
- Re-check a sidecar when the track's lastModified or the sidecar's lastModified changes.
- LyricsRepository.observeLyrics(trackId): Flow<LyricsState> = Loading | Available(Lyrics) | None | Error.

2) PARSING (data/lyrics/LrcParser, pure Kotlin, heavily unit tested)
- Standard LRC: [mm:ss.xx], [mm:ss.xxx], [m:ss], [mm:ss] timestamps. Multiple timestamps expand to multiple entries.
- Metadata tags: [ti:], [ar:], [al:], [by:], [offset:+/-ms], [length:].
- Enhanced LRC (word-level) <mm:ss.xx>word: parse into words with times. Set hasWordSync = true.
- Empty lines are kept as instrumental gaps ("♪" interlude if gap > 8 s).
- Encoding: UTF-8 (with/without BOM), UTF-16 (BOM), fallback to ISO-8859-1.
- Detect SYNCED vs PLAIN by presence of at least 3 valid timestamps.
- Binary search activeLineIndex(lines, positionMs + offsetMs).

3) LYRICS VIEW (ui/nowplaying/lyrics)
- Entry points: toggle pill switches stage area between VinylStage and LyricsPanel with 300ms crossfade/slide.
- Compact 72dp spinning vinyl thumbnail at top-left with title/artist next to it.
- SYNCED: LazyColumn of lines. Active line large in accent/onBackground, distance-based alpha dimming.
  Auto-scroll keeps active line at ~35% viewport height.
- Word highlight sweep inside active line (draw phase lambda).
- User scroll pauses auto-scroll, shows "Jump to current" chip. Resumes after 4 s inactivity.
- Tap a line seeks via PlayerController; long press copies line text.
- Text sizing: Small / Medium / Large setting. RTL and CJK support.

4) IMPORT AND EDIT
- "Import lyrics" via OpenDocument for .lrc and .txt.
- Sync offset bottom sheet (-/+ 100ms, -/+ 500ms, range ±10 s, live preview).

5) SETTINGS & TESTS
- Settings: text size, toggle visibility, word highlight, keep screen on, stats ("N songs with lyrics").
- Acceptance: synced lyrics scroll smoothly, line tap seeks with tonearm moving in sync, zero full-list recompositions.
```

---

## Prompt Q: Home-screen widget

### Design notes:
- Built with Jetpack Glance (`GlanceAppWidget` with `SizeMode.Responsive`).
- RemoteViews cannot run continuous Canvas animation, so pre-render Bitmaps off-screen with lowered/lifted arm.

```
[Paste shared context block + add-on rules]

TASK: Build Isai home-screen widgets with Glance: small, medium and large, with playback controls
that work even when the app is not running.

1) WIDGET SIZES AND LAYOUTS (ui/widget)
- SMALL (2x1 / ~110x60 dp): circular vinyl thumbnail on left, play/pause and next on right.
- MEDIUM (4x2 / ~250x110 dp): vinyl thumbnail with small tonearm on left; title, artist, prev, play/pause, next, progress line on right.
- LARGE (4x4 / ~250x250 dp): big vinyl disc with tonearm, title/artist, progress bar with elapsed/total, full controls (shuffle, prev, play/pause, next, repeat, heart).
- Empty state: Isai logo, "Tap to open Isai".

2) VINYL AND TONEARM RENDERING FOR WIDGETS
- WidgetArtRenderer.render(art, sizePx, armState: LOWERED|LIFTED, theme): Bitmap
  * Circle-cropped art with ~24 groove rings and rim.
  * Tonearm drawn at lowered angle (~alphaOuter) or lifted angle (alphaRest).
  * Binder limit guard (downscale or pass file-backed content URI via FileProvider if > 500 KB).

3) STATE AND UPDATES (playback/widget)
- WidgetStateStore: small DataStore/Glance state.
- WidgetUpdater (singleton in PlaybackService): throttled updates (max 1 per 500ms).
- Progress updates only on events, seek, and every 15 s while playing IF widget placed and screen ON.
- Zero per-second ticking on widgets (preserves battery).

4) ACTIONS
- Glance ActionCallback classes (PlayPauseAction, NextAction, PreviousAction, ToggleFavoriteAction, etc.) connecting via MediaController.
- Starts PlaybackService if killed. Resumes persisted queue without autoplay on play button.
- Tapping disc/title opens MainActivity on NowPlaying.

5) CONFIGURATION & ACCEPTANCE
- Optional config activity: style (System/Dark/Light/Dynamic), opacity slider, toggles.
- Survives reboot, app update, and process death.
```

---

## Prompt R: Add-on QA and release update (v1.1.0)

```
[Paste shared context block + add-on rules]

TASK: Integrate, regression test and ship the add-ons as Isai v1.1.0.

- Scrub ownership matrix: tonearm drag, waveform drag, lyrics line tap, notification seek, widget actions stay in sync.
- DB migrations tested with real v1.0.0 file.
- Performance benchmarks: cold start, list scroll, Now Playing at 60 fps, APK size delta check.
- Version bump: v1.1.0. Signed AAB, updated CHANGELOG.
```

