package com.isai.player.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import com.isai.player.data.repository.LiftBehavior
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.player.R
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.CardShape
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.util.Formatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TonearmLabScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isDarkTheme by remember { mutableStateOf(true) }
    var showDebugOverlay by remember { mutableStateOf(true) }

    // FakePlayer instance
    val fakePlayer = remember { FakePlayer(scope) }
    val isPlaying by fakePlayer.isPlaying.collectAsStateWithLifecycle()
    val positionMs by fakePlayer.positionMs.collectAsStateWithLifecycle()
    val hasTrack by fakePlayer.hasTrack.collectAsStateWithLifecycle()

    // Standalone stage measurements (360 x 360 dp)
    val density = LocalDensity.current
    val stageSizePx = with(density) { 360.dp.toPx() }
    val discCenter = Offset(stageSizePx * 0.45f, stageSizePx * 0.55f)
    val discRadius = stageSizePx * 0.36f

    val geometry = remember(discCenter, discRadius) {
        TonearmGeometry.forDisc(discCenter, discRadius)
    }

    val tonearmState = rememberTonearmState(geometry, scope)
    val tonearmColors = if (isDarkTheme) TonearmColors.dark() else TonearmColors.light()

    // Real-time scrubbing preview state
    var scrubPreviewProgress by remember { mutableStateOf<Float?>(null) }
    val currentProgress = scrubPreviewProgress ?: (positionMs.toFloat() / fakePlayer.durationMs.toFloat())

    // Event Log
    val eventLogs = remember { mutableStateListOf<String>() }
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    fun logEvent(event: String) {
        val timestamp = timeFormatter.format(Date())
        eventLogs.add(0, "[$timestamp] $event")
        if (eventLogs.size > 20) {
            eventLogs.removeAt(eventLogs.lastIndex)
        }
    }

    // React to playback state changes
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            tonearmState.animateLower()
            logEvent("Player: PLAY -> Tonearm lowering with bounce")
        } else {
            tonearmState.animatePark()
            logEvent("Player: PAUSE -> Tonearm lifting to park")
        }
    }

    IsaiTheme(themeMode = if (isDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Prompt B: Motion & Gestures") },
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
                // 360 dp square interactive testing stage
                Box(
                    modifier = Modifier
                        .size(360.dp)
                        .background(IsaiTheme.colors.surface)
                ) {
                    // 1. Placeholder disc: black circle with center dot
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFF121216),
                            radius = discRadius,
                            center = discCenter
                        )
                        drawCircle(
                            color = Color(0xFF2C2D35),
                            radius = discRadius,
                            center = discCenter,
                            style = Stroke(width = 2.dp.toPx())
                        )
                        drawCircle(
                            color = Color(0xFF888B96),
                            radius = 6.dp.toPx(),
                            center = discCenter
                        )
                    }

                    // 2. Interactive Tonearm Layer with hit target, touch scale & gesture tracking
                    TonearmInteractiveContainer(
                        state = tonearmState,
                        progress = { currentProgress },
                        isPlaying = isPlaying,
                        hasTrack = hasTrack,
                        onScrubStart = {
                            logEvent("onScrubStart -> dragging headshell")
                        },
                        onScrubPreview = { previewProg ->
                            scrubPreviewProgress = previewProg
                        },
                        onScrubEnd = { commit, finalProgress ->
                            scrubPreviewProgress = null
                            if (commit) {
                                fakePlayer.seekToProgress(finalProgress)
                                fakePlayer.play()
                                val time = Formatters.formatDurationMs((finalProgress * fakePlayer.durationMs).toLong())
                                logEvent("onScrubEnd (COMMIT) -> seeked to $time (${String.format(Locale.US, "%.1f%%", finalProgress * 100f)}) & resumed")
                            } else {
                                logEvent("onScrubEnd (CANCELED)")
                            }
                        },
                        onLiftedStop = {
                            scrubPreviewProgress = null
                            val behavior = fakePlayer.liftBehavior
                            fakePlayer.handleLiftedStop()
                            logEvent("onLiftedStop -> needle lifted off record ($behavior)")
                        },
                        onTapToggle = {
                            fakePlayer.togglePlayPause()
                            logEvent("onTapToggle -> toggled playback: isPlaying=${!isPlaying}")
                        },
                        colors = tonearmColors,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 3. Debug Overlay Layer
                    if (showDebugOverlay) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                color = Color.Cyan.copy(alpha = 0.5f),
                                radius = geometry.rOuter,
                                center = discCenter,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(
                                color = Color.Magenta.copy(alpha = 0.5f),
                                radius = geometry.rInner,
                                center = discCenter,
                                style = Stroke(width = 1.dp.toPx())
                            )
                            drawCircle(
                                color = Color.Red,
                                radius = 4.dp.toPx(),
                                center = geometry.pivot
                            )
                            val arcPath = Path()
                            val steps = 40
                            for (i in 0..steps) {
                                val t = i / steps.toFloat()
                                val a = geometry.alphaRest + t * (geometry.alphaInner - geometry.alphaRest)
                                val pos = geometry.stylusPosition(a)
                                if (i == 0) arcPath.moveTo(pos.x, pos.y) else arcPath.lineTo(pos.x, pos.y)
                            }
                            drawPath(
                                path = arcPath,
                                color = Color.Green.copy(alpha = 0.6f),
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }

                // Controls, Timeline Labels & Event Logs
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Playback Timeline & Scrub Readout
                    val displayedPositionMs = (currentProgress * fakePlayer.durationMs).toLong()
                    val remainingMs = (fakePlayer.durationMs - displayedPositionMs).coerceAtLeast(0L)

                    Card(
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = Formatters.formatDurationMs(displayedPositionMs),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (scrubPreviewProgress != null) IsaiTheme.colors.accent else IsaiTheme.colors.onBackground
                                )
                                if (scrubPreviewProgress != null) {
                                    Text(
                                        text = "SCRUBBING",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IsaiTheme.colors.accent
                                    )
                                }
                                Text(
                                    text = "-${Formatters.formatDurationMs(remainingMs)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = IsaiTheme.colors.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val displayDeg = tonearmState.displayAngle(currentProgress)
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Angle: %.1f° | Progress: %.1f%% | Groove: %s",
                                    displayDeg,
                                    currentProgress * 100f,
                                    if (geometry.isOnRecord(displayDeg)) "LOCKED" else "OFF DISC"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Player Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { fakePlayer.togglePlayPause() },
                            colors = ButtonDefaults.buttonColors(containerColor = IsaiTheme.colors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(if (isPlaying) "Pause" else "Play")
                        }

                        Button(
                            onClick = {
                                fakePlayer.handleLiftedStop()
                                logEvent("External Stop/Pause event triggered")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IsaiTheme.colors.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Lift Needle",
                                color = IsaiTheme.colors.onBackground
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Settings: Lift behavior & Track Loaded
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Lift Behavior",
                                style = MaterialTheme.typography.bodyLarge,
                                color = IsaiTheme.colors.onBackground
                            )
                            Text(
                                text = if (fakePlayer.liftBehavior == LiftBehavior.PAUSE) "Pause at position" else "Stop & reset to 0:00",
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = fakePlayer.liftBehavior == LiftBehavior.STOP,
                            onCheckedChange = { isStop ->
                                fakePlayer.liftBehavior = if (isStop) LiftBehavior.STOP else LiftBehavior.PAUSE
                                logEvent("Setting: Lift Behavior -> ${fakePlayer.liftBehavior}")
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Track Loaded",
                                style = MaterialTheme.typography.bodyLarge,
                                color = IsaiTheme.colors.onBackground
                            )
                            Text(
                                text = if (hasTrack) "Track active (allows seek)" else "No track (triggers deny shake on touch)",
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = hasTrack,
                            onCheckedChange = {
                                fakePlayer.setHasTrack(it)
                                logEvent("Setting: HasTrack -> $it")
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Dark Theme",
                            style = MaterialTheme.typography.bodyLarge,
                            color = IsaiTheme.colors.onBackground
                        )
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { isDarkTheme = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Live Event Log Card
                    Text(
                        text = "Real-time Event Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IsaiTheme.colors.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = CardShape,
                        colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (eventLogs.isEmpty()) {
                                Text(
                                    text = "Interact with the tonearm or player controls to log events...",
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
                                        color = if (log.contains("COMMIT")) IsaiTheme.colors.accent else IsaiTheme.colors.onBackground
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "TonearmLabScreen - Prompt B Preview")
@Composable
private fun TonearmLabScreenPreview() {
    TonearmLabScreen(onBackClick = {})
}
