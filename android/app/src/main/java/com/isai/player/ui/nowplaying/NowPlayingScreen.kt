package com.isai.player.ui.nowplaying

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.player.R
import com.isai.player.data.repository.LiftBehavior
import com.isai.player.data.repository.LyricsState
import com.isai.player.data.repository.LyricsTextSize
import com.isai.player.data.repository.SeekBarStyle
import com.isai.player.data.repository.ThemeMode
import com.isai.player.data.repository.WaveformState
import com.isai.player.playback.PlayerState
import com.isai.player.playback.RepeatMode
import com.isai.player.ui.components.StageTrack
import com.isai.player.ui.components.SwipeDirection
import com.isai.player.ui.components.VinylStage
import com.isai.player.ui.components.WaveformSeekBar
import com.isai.player.ui.components.defaultSeekBarColors
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    onCollapseClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NowPlayingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val stageTrack by viewModel.stageTrack.collectAsStateWithLifecycle()
    val isFavorite by viewModel.isFavorite.collectAsStateWithLifecycle()
    val liftBehavior by viewModel.liftBehavior.collectAsStateWithLifecycle()
    val seekBarStyle by viewModel.seekBarStyle.collectAsStateWithLifecycle()
    val waveformState by viewModel.waveform.collectAsStateWithLifecycle()
    val isLyricsVisible by viewModel.isLyricsVisible.collectAsStateWithLifecycle()
    val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
    val lyricsTextSize by viewModel.lyricsTextSize.collectAsStateWithLifecycle()
    val scrubPreviewProgress by viewModel.scrubPreviewProgress.collectAsStateWithLifecycle()
    val lastSwipeDirection by viewModel.lastSwipeDirection.collectAsStateWithLifecycle()
    val sleepTimerRemainingMs by viewModel.sleepTimerRemainingMs.collectAsStateWithLifecycle()
    val isStopAtEndOfTrack by viewModel.isStopAtEndOfTrack.collectAsStateWithLifecycle()

    var showQueueSheet by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showOffsetDialog by remember { mutableStateOf(false) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Document Picker launcher for importing .lrc / .txt files
    val importLyricsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = stream.bufferedReader().readText()
                    viewModel.importLyrics(text)
                }
            }
        }
    }

    NowPlayingContent(
        playerState = playerState,
        stageTrack = stageTrack,
        isFavorite = isFavorite,
        isSleepTimerActive = sleepTimerRemainingMs > 0L || isStopAtEndOfTrack,
        isLyricsVisible = isLyricsVisible,
        lyricsState = lyricsState,
        lyricsTextSize = lyricsTextSize,
        liftBehavior = liftBehavior,
        seekBarStyle = seekBarStyle,
        waveformState = waveformState,
        scrubPreviewProgress = scrubPreviewProgress,
        lastSwipeDirection = lastSwipeDirection,
        onCollapseClick = onCollapseClick,
        onTogglePlayPause = { viewModel.togglePlayPause() },
        onToggleLyrics = { viewModel.toggleLyricsView() },
        onSeekToLyric = { viewModel.seekToLyricTimestamp(it) },
        onImportLyricsClick = { importLyricsLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*")) },
        onOpenOffsetDialog = { showOffsetDialog = true },
        onScrubStartArm = { viewModel.onScrubStart(ScrubOwner.ARM) },
        onScrubPreviewArm = { viewModel.onScrubPreview(ScrubOwner.ARM, it) },
        onScrubEndArm = { commit, progress -> viewModel.onScrubEnd(ScrubOwner.ARM, commit, progress) },
        onScrubStartBar = { viewModel.onScrubStart(ScrubOwner.BAR) },
        onScrubPreviewBar = { viewModel.onScrubPreview(ScrubOwner.BAR, it) },
        onScrubEndBar = { commit, progress -> viewModel.onScrubEnd(ScrubOwner.BAR, commit, progress) },
        onLiftedStop = { viewModel.onLiftedStop() },
        onSkipNext = { viewModel.skipToNext() },
        onSkipPrevious = { viewModel.skipToPrevious() },
        onToggleFavorite = { viewModel.toggleFavorite() },
        onToggleShuffle = { viewModel.toggleShuffle() },
        onCycleRepeat = { viewModel.cycleRepeatMode() },
        onOpenQueueClick = { showQueueSheet = true },
        onOpenSleepTimerClick = { showSleepTimerDialog = true },
        modifier = modifier
    )

    if (showOffsetDialog) {
        val currentOffset = (lyricsState as? LyricsState.Synced)?.offsetMs ?: 0
        LyricsOffsetDialog(
            currentOffsetMs = currentOffset,
            onOffsetChange = { viewModel.setLyricsOffset(it) },
            onDismissRequest = { showOffsetDialog = false }
        )
    }

    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingMs = sleepTimerRemainingMs,
            isStopAtEndOfTrack = isStopAtEndOfTrack,
            onStartTimer = { durationMs, stopAtEnd -> },
            onCancelTimer = { },
            onDismissRequest = { showSleepTimerDialog = false }
        )
    }

    if (showQueueSheet) {
        QueueBottomSheet(
            sheetState = queueSheetState,
            queue = playerState.queue,
            currentQueueIndex = playerState.queueIndex,
            isPlaying = playerState.isPlaying,
            onTrackClick = { index ->
                viewModel.playQueueIndex(index)
            },
            onRemoveTrack = { index ->
                viewModel.removeQueueItem(index)
            },
            onClearQueue = { },
            onDismissRequest = {
                showQueueSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingContent(
    playerState: PlayerState,
    stageTrack: StageTrack?,
    isFavorite: Boolean,
    isSleepTimerActive: Boolean = false,
    isLyricsVisible: Boolean = false,
    lyricsState: LyricsState = LyricsState.Empty,
    lyricsTextSize: LyricsTextSize = LyricsTextSize.MEDIUM,
    liftBehavior: LiftBehavior,
    seekBarStyle: SeekBarStyle,
    waveformState: WaveformState,
    scrubPreviewProgress: Float?,
    lastSwipeDirection: SwipeDirection,
    onCollapseClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleLyrics: () -> Unit,
    onSeekToLyric: (timestampMs: Long) -> Unit,
    onImportLyricsClick: () -> Unit,
    onOpenOffsetDialog: () -> Unit,
    onScrubStartArm: () -> Unit,
    onScrubPreviewArm: (Float) -> Unit,
    onScrubEndArm: (commit: Boolean, progress: Float) -> Unit,
    onScrubStartBar: () -> Unit,
    onScrubPreviewBar: (Float) -> Unit,
    onScrubEndBar: (commit: Boolean, progress: Float) -> Unit,
    onLiftedStop: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenQueueClick: () -> Unit,
    onOpenSleepTimerClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val durationMs = playerState.durationMs
    val currentProgress = scrubPreviewProgress ?: playerState.progress
    val currentPositionMs = (currentProgress * durationMs).toLong().coerceIn(0L, durationMs.coerceAtLeast(0L))
    val remainingMs = (durationMs - currentPositionMs).coerceAtLeast(0L)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.nav_screen_now_playing).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = IsaiTheme.colors.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCollapseClick) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(id = R.string.global_action_cancel),
                            tint = IsaiTheme.colors.onBackground,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                },
                actions = {
                    // Lyrics Toggle Button
                    IconButton(onClick = onToggleLyrics) {
                        Icon(
                            imageVector = Icons.Default.Subtitles,
                            contentDescription = stringResource(id = R.string.action_lyrics),
                            tint = if (isLyricsVisible) IsaiTheme.colors.accent else IsaiTheme.colors.onBackground
                        )
                    }
                    IconButton(onClick = onOpenSleepTimerClick) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = stringResource(id = R.string.sleep_timer_title),
                            tint = if (isSleepTimerActive) IsaiTheme.colors.accent else IsaiTheme.colors.onBackground
                        )
                    }
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(
                                id = if (isFavorite) R.string.track_menu_action_remove_favorite else R.string.track_menu_action_add_favorite
                            ),
                            tint = if (isFavorite) IsaiTheme.colors.accent else IsaiTheme.colors.onBackground
                        )
                    }
                    IconButton(onClick = onOpenQueueClick) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = stringResource(id = R.string.nav_tab_library),
                            tint = IsaiTheme.colors.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = IsaiTheme.colors.background,
                    titleContentColor = IsaiTheme.colors.onBackground
                )
            )
        },
        containerColor = IsaiTheme.colors.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Center Hero Area: Smooth transition between VinylStage and LyricsView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            ) {
                AnimatedContent(
                    targetState = isLyricsVisible,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
                    },
                    label = "StageOrLyrics"
                ) { showLyrics ->
                    if (showLyrics) {
                        LyricsView(
                            lyricsState = lyricsState,
                            textSize = lyricsTextSize,
                            onLineClick = onSeekToLyric,
                            onImportClick = onImportLyricsClick,
                            onOpenOffsetDialog = onOpenOffsetDialog,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        VinylStage(
                            track = stageTrack,
                            isPlaying = playerState.isPlaying,
                            progress = { currentProgress },
                            durationMs = durationMs,
                            lastSwipeDirection = lastSwipeDirection,
                            liftBehavior = liftBehavior,
                            onTogglePlay = onTogglePlayPause,
                            onScrubStart = onScrubStartArm,
                            onScrubPreview = onScrubPreviewArm,
                            onScrubEnd = onScrubEndArm,
                            onLiftedStop = onLiftedStop,
                            onSwipeNext = onSkipNext,
                            onSwipePrevious = onSkipPrevious,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 2. Track Title, Artist & Album Information
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = playerState.currentTrack?.title ?: stringResource(id = R.string.miniplayer_placeholder_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IsaiTheme.colors.onBackground,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val subtitle = if (playerState.currentTrack != null) {
                    val track = playerState.currentTrack
                    "${track.artist} · ${track.album}" + if (track.year > 0) " (${track.year})" else ""
                } else {
                    stringResource(id = R.string.miniplayer_placeholder_artist)
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IsaiTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 3. Waveform Seek Bar with Elapsed and Remaining Time
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = Formatters.formatDurationMs(currentPositionMs),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = IsaiTheme.colors.accent
                    )
                    Text(
                        text = "-${Formatters.formatDurationMs(remainingMs)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = IsaiTheme.colors.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                val peaks = (waveformState as? WaveformState.Ready)?.peaks
                val isSeekEnabled = playerState.currentTrack != null && durationMs > 0

                WaveformSeekBar(
                    peaks = peaks,
                    progress = { currentProgress },
                    style = seekBarStyle,
                    colors = defaultSeekBarColors(),
                    trackDurationMs = durationMs,
                    fallbackSeed = playerState.currentTrack?.id ?: 12345L,
                    enabled = isSeekEnabled,
                    onScrubStart = onScrubStartBar,
                    onScrubPreview = onScrubPreviewBar,
                    onScrubEnd = onScrubEndBar,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Control Cluster Dial (Shuffle, Prev, Play/Pause, Next, Repeat)
            ControlDial(
                isPlaying = playerState.isPlaying,
                shuffleEnabled = playerState.shuffleModeEnabled,
                repeatMode = playerState.repeatMode,
                onTogglePlayPause = onTogglePlayPause,
                onSkipNext = onSkipNext,
                onSkipPrevious = onSkipPrevious,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
