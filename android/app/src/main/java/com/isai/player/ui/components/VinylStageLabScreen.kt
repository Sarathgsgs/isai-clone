package com.isai.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsPaused
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.player.R
import com.isai.player.data.repository.LiftBehavior
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.CardShape
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VinylStageLabScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isDarkTheme by remember { mutableStateOf(true) }
    var isReducedMotion by remember { mutableStateOf(false) }

    // Sample playlist of 5 tracks
    val sampleTracks = remember {
        listOf(
            StageTrack("1", "Sundari Kannal Oru Sethi", "Ilaiyaraaja", "Thalapathi", 1991, null),
            StageTrack("2", "Chinna Chinna Aasai", "A. R. Rahman", "Roja", 1992, null),
            StageTrack("3", "Nilaave Vaa", "Ilaiyaraaja", "Mouna Ragam", 1986, null),
            StageTrack("4", "Uyire Uyire", "A. R. Rahman", "Bombay", 1995, null),
            StageTrack("5", "Ennavale Adi Ennavale", "A. R. Rahman", "Kadhalan", 1994, null)
        )
    }

    var currentTrackIndex by remember { mutableIntStateOf(0) }
    var swipeDirection by remember { mutableStateOf(SwipeDirection.NONE) }

    // FakePlayer
    val fakePlayer = remember { FakePlayer(scope) }
    val isPlaying by fakePlayer.isPlaying.collectAsStateWithLifecycle()
    val positionMs by fakePlayer.positionMs.collectAsStateWithLifecycle()

    var scrubPreviewProgress by remember { mutableStateOf<Float?>(null) }
    val currentProgress = scrubPreviewProgress ?: (positionMs.toFloat() / fakePlayer.durationMs.toFloat())

    // Event log
    val eventLogs = remember { mutableStateListOf<String>() }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    fun logEvent(event: String) {
        val timestamp = timeFormatter.format(Date())
        eventLogs.add(0, "[$timestamp] $event")
        if (eventLogs.size > 20) {
            eventLogs.removeAt(eventLogs.lastIndex)
        }
    }

    // Previous track logic: if position > 3s, restart; else previous track
    fun handlePrevious() {
        if (positionMs > 3000L) {
            fakePlayer.seekTo(0L)
            logEvent("handlePrevious -> position > 3s, restarting track from 0:00")
        } else {
            swipeDirection = SwipeDirection.PREVIOUS
            currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else sampleTracks.lastIndex
            fakePlayer.seekTo(0L)
            logEvent("handlePrevious -> changed to track: ${sampleTracks[currentTrackIndex].title}")
        }
    }

    fun handleNext() {
        swipeDirection = SwipeDirection.NEXT
        currentTrackIndex = (currentTrackIndex + 1) % sampleTracks.size
        fakePlayer.seekTo(0L)
        logEvent("handleNext -> changed to track: ${sampleTracks[currentTrackIndex].title}")
    }

    // FPS counter
    var currentFps by remember { mutableIntStateOf(60) }
    var frameCount by remember { mutableIntStateOf(0) }
    var lastFpsTimestamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { nowNanos ->
                frameCount++
                if (lastFpsTimestamp == 0L) {
                    lastFpsTimestamp = nowNanos
                } else {
                    val elapsed = nowNanos - lastFpsTimestamp
                    if (elapsed >= 1_000_000_000L) {
                        currentFps = frameCount
                        frameCount = 0
                        lastFpsTimestamp = nowNanos
                    }
                }
            }
        }
    }

    val currentTrack = sampleTracks.getOrNull(currentTrackIndex)

    IsaiTheme(themeMode = if (isDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Prompt D: Vinyl Stage Lab") },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(id = R.string.global_action_cancel),
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
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Vinyl Stage Container (340dp height)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    VinylStage(
                        track = currentTrack,
                        isPlaying = isPlaying,
                        progress = { currentProgress },
                        durationMs = fakePlayer.durationMs,
                        lastSwipeDirection = swipeDirection,
                        liftBehavior = fakePlayer.liftBehavior,
                        onTogglePlay = {
                            fakePlayer.togglePlayPause()
                            logEvent("VinylStage: onTapToggle -> isPlaying=${!isPlaying}")
                        },
                        onScrubStart = {
                            logEvent("VinylStage: onScrubStart")
                        },
                        onScrubPreview = {
                            scrubPreviewProgress = it
                        },
                        onScrubEnd = { commit, progressVal ->
                            scrubPreviewProgress = null
                            if (commit) {
                                fakePlayer.seekToProgress(progressVal)
                                fakePlayer.play()
                                val time = Formatters.formatDurationMs((progressVal * fakePlayer.durationMs).toLong())
                                logEvent("VinylStage: onScrubEnd -> seek to $time and resumed")
                            }
                        },
                        onLiftedStop = {
                            scrubPreviewProgress = null
                            fakePlayer.handleLiftedStop()
                            logEvent("VinylStage: onLiftedStop (${fakePlayer.liftBehavior})")
                        },
                        onSwipeNext = {
                            handleNext()
                        },
                        onSwipePrevious = {
                            handlePrevious()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Controls and Readouts below stage
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    // Track Title and Artist
                    Text(
                        text = currentTrack?.title ?: "No Track",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IsaiTheme.colors.onBackground
                    )
                    Text(
                        text = "${currentTrack?.artist} · ${currentTrack?.album} (${currentTrack?.year})",
                        style = MaterialTheme.typography.labelMedium,
                        color = IsaiTheme.colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Seek Bar with elapsed / remaining times (tapping seek bar moves tonearm)
                    val displayedPositionMs = (currentProgress * fakePlayer.durationMs).toLong()
                    val remainingMs = (fakePlayer.durationMs - displayedPositionMs).coerceAtLeast(0L)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = Formatters.formatDurationMs(displayedPositionMs),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = IsaiTheme.colors.accent
                        )
                        Text(
                            text = "-${Formatters.formatDurationMs(remainingMs)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = IsaiTheme.colors.onSurfaceVariant
                        )
                    }

                    Slider(
                        value = currentProgress,
                        onValueChange = { preview ->
                            scrubPreviewProgress = preview
                        },
                        onValueChangeFinished = {
                            scrubPreviewProgress?.let { finalProg ->
                                fakePlayer.seekToProgress(finalProg)
                                logEvent("Seek bar scrub finished -> ${Formatters.formatDurationMs((finalProg * fakePlayer.durationMs).toLong())}")
                            }
                            scrubPreviewProgress = null
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = IsaiTheme.colors.accent,
                            activeTrackColor = IsaiTheme.colors.accent
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Player Controls Cluster
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { handlePrevious() }) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous",
                                tint = IsaiTheme.colors.onBackground,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Button(
                            onClick = { fakePlayer.togglePlayPause() },
                            colors = ButtonDefaults.buttonColors(containerColor = IsaiTheme.colors.accent),
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(onClick = { handleNext() }) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next",
                                tint = IsaiTheme.colors.onBackground,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // External Notification Pause Simulator
                        IconButton(onClick = {
                            fakePlayer.pause()
                            logEvent("SIMULATION: External notification pause received")
                        }) {
                            Icon(
                                imageVector = Icons.Default.NotificationsPaused,
                                contentDescription = "Simulate Notification Pause",
                                tint = IsaiTheme.colors.accent,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Stats & Toggles
                    Card(
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Diagnostics",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = IsaiTheme.colors.onBackground
                                )
                                Text(
                                    text = "$currentFps FPS",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentFps >= 55) IsaiTheme.colors.accent else MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Track: ${currentTrackIndex + 1}/${sampleTracks.size} | Lift: ${fakePlayer.liftBehavior}",
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Lift Behavior & Theme Switches
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lift Needle Behavior",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IsaiTheme.colors.onBackground
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (fakePlayer.liftBehavior == LiftBehavior.STOP) "STOP (reset 0:00)" else "PAUSE",
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.accent,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Switch(
                                checked = fakePlayer.liftBehavior == LiftBehavior.STOP,
                                onCheckedChange = { isStop ->
                                    fakePlayer.liftBehavior = if (isStop) LiftBehavior.STOP else LiftBehavior.PAUSE
                                    logEvent("Setting: Lift Behavior -> ${fakePlayer.liftBehavior}")
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dark Theme",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IsaiTheme.colors.onBackground
                        )
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { isDarkTheme = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Event Log
                    Text(
                        text = "Stage Event Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IsaiTheme.colors.onBackground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (eventLogs.isEmpty()) {
                                Text(
                                    text = "Interact with the vinyl disc, tonearm or swipe horizontally to log events...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = IsaiTheme.colors.onSurfaceVariant
                                )
                            } else {
                                eventLogs.forEach { log ->
                                    Text(
                                        text = log,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = if (log.contains("SIMULATION")) IsaiTheme.colors.accent else IsaiTheme.colors.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "VinylStageLabScreen - Prompt D Preview")
@Composable
private fun VinylStageLabScreenPreview() {
    VinylStageLabScreen(onBackClick = {})
}

