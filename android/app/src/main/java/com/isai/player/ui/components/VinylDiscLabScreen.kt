package com.isai.player.ui.components

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
import androidx.compose.material.icons.filled.RestartAlt
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.CardShape
import com.isai.player.ui.theme.IsaiTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VinylDiscLabScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isDarkTheme by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var isReducedMotion by remember { mutableStateOf(false) }

    // Art sample presets
    val artPresets = listOf(
        "Thalapathi Ilaiyaraaja",
        "Roja A. R. Rahman",
        "Mouna Ragam Maestro",
        "Bombay Harris Jayaraj"
    )
    var selectedPresetIndex by remember { mutableIntStateOf(0) }

    val discState = rememberVinylDiscState()
    val vinylColors = if (isDarkTheme) VinylColors.dark() else VinylColors.light()

    // Connect spin effect to playback state
    discState.SpinEffect(spinning = isPlaying, reducedMotion = isReducedMotion)

    // Real-time FPS counter via withFrameNanos
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
                    if (elapsed >= 1_000_000_000L) { // 1 second
                        currentFps = frameCount
                        frameCount = 0
                        lastFpsTimestamp = nowNanos
                    }
                }
            }
        }
    }

    IsaiTheme(themeMode = if (isDarkTheme) ThemeMode.DARK else ThemeMode.LIGHT) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Prompt C: Vinyl Disc Lab") },
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
                Spacer(modifier = Modifier.height(16.dp))

                // 320dp square stage hosting the VinylDisc
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    VinylDisc(
                        state = discState,
                        art = null, // Demonstrates full-bleed fallback art
                        fallbackSeed = artPresets[selectedPresetIndex],
                        colors = vinylColors,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Readouts & Controls
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Live Stats Card (FPS, Angle, Speed)
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
                                    text = String.format(Locale.US, "Angle: %.1f°", discState.angle),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = IsaiTheme.colors.onBackground
                                )
                                Text(
                                    text = "$currentFps FPS",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentFps >= 55) IsaiTheme.colors.accent else MaterialTheme.colorScheme.error
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = String.format(
                                    Locale.US,
                                    "Speed: %.2f (%.0f°/s) | Target: %s",
                                    discState.speed.value,
                                    discState.speed.value * 200f,
                                    if (isPlaying) "33⅓ RPM" else "0 RPM (Coast)"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play/Pause & Reset Angle Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { isPlaying = !isPlaying },
                            colors = ButtonDefaults.buttonColors(containerColor = IsaiTheme.colors.accent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(if (isPlaying) "Pause Spin" else "Spin 33⅓ RPM")
                        }

                        Button(
                            onClick = { discState.resetAngle() },
                            colors = ButtonDefaults.buttonColors(containerColor = IsaiTheme.colors.surfaceVariant),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = IsaiTheme.colors.onBackground,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Reset Angle", color = IsaiTheme.colors.onBackground)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Art Presets Chips
                    Text(
                        text = "Album Art Fallback Presets",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = IsaiTheme.colors.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        artPresets.forEachIndexed { index, preset ->
                            IsaiChip(
                                selected = selectedPresetIndex == index,
                                onClick = { selectedPresetIndex = index },
                                label = preset.split(" ").first()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Settings: Theme & Reduced Motion
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Reduced Motion",
                                style = MaterialTheme.typography.bodyLarge,
                                color = IsaiTheme.colors.onBackground
                            )
                            Text(
                                text = "Disables rotation completely",
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isReducedMotion,
                            onCheckedChange = { isReducedMotion = it }
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "VinylDiscLabScreen - Prompt C Preview")
@Composable
private fun VinylDiscLabScreenPreview() {
    VinylDiscLabScreen(onBackClick = {})
}
