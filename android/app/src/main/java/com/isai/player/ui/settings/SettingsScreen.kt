package com.isai.player.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.player.R
import com.isai.player.data.repository.LiftBehavior
import com.isai.player.data.repository.LyricsTextSize
import com.isai.player.data.repository.SeekBarStyle
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.components.IsaiChip
import com.isai.player.ui.components.SectionHeader
import com.isai.player.ui.components.WaveformSeekBar
import com.isai.player.ui.components.defaultSeekBarColors
import com.isai.player.ui.nowplaying.SleepTimerDialog
import com.isai.player.ui.theme.CardShape
import com.isai.player.ui.theme.IsaiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onBackClick: () -> Unit,
    onOpenLabsClick: () -> Unit,
    onOpenEqualizerClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showPurgeConfirmDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }

    // SAF Document Tree Launcher for changing music directory
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            try {
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: SecurityException) {
                // Ignore if permission already persisted
            }
            val docFile = DocumentFile.fromTreeUri(context, uri)
            val folderName = docFile?.name ?: uri.lastPathSegment ?: "Music"
            viewModel.updateFolderAndRescan(uri, folderName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.nav_screen_settings)) },
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // 1. Appearance Section
            SectionHeader(title = stringResource(id = R.string.settings_section_appearance))

            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_theme_mode),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = IsaiTheme.colors.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val label = when (mode) {
                                ThemeMode.SYSTEM -> stringResource(id = R.string.settings_theme_system)
                                ThemeMode.DARK -> stringResource(id = R.string.settings_theme_dark)
                                ThemeMode.LIGHT -> stringResource(id = R.string.settings_theme_light)
                            }
                            IsaiChip(
                                selected = state.themeMode == mode,
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    onThemeModeChange(mode)
                                },
                                label = label
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Accent Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_dynamic_accent),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = IsaiTheme.colors.onBackground
                            )
                            Text(
                                text = stringResource(id = R.string.settings_dynamic_accent_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = state.dynamicAccentColor,
                            onCheckedChange = { viewModel.setDynamicAccentColor(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = IsaiTheme.colors.onAccent,
                                checkedTrackColor = IsaiTheme.colors.accent,
                                uncheckedThumbColor = IsaiTheme.colors.onSurfaceVariant,
                                uncheckedTrackColor = IsaiTheme.colors.surfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Seekbar Style Selector
                    Text(
                        text = stringResource(id = R.string.settings_seekbar_style),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = IsaiTheme.colors.onBackground
                    )
                    Text(
                        text = stringResource(id = R.string.settings_seekbar_style_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = IsaiTheme.colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SeekBarStyle.entries.forEach { style ->
                            val label = when (style) {
                                SeekBarStyle.AUTO -> stringResource(id = R.string.settings_seekbar_auto)
                                SeekBarStyle.WAVEFORM_BARS -> stringResource(id = R.string.settings_seekbar_bars)
                                SeekBarStyle.WAVEFORM_LINE -> stringResource(id = R.string.settings_seekbar_line)
                                SeekBarStyle.CLASSIC -> stringResource(id = R.string.settings_seekbar_classic)
                            }
                            IsaiChip(
                                selected = state.seekBarStyle == style,
                                onClick = { viewModel.setSeekBarStyle(style) },
                                label = label
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Live Seekbar Preview Tile
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = IsaiTheme.colors.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        WaveformSeekBar(
                            peaks = null, // uses fallback for smooth preview
                            progress = { 0.45f },
                            style = state.seekBarStyle,
                            colors = defaultSeekBarColors(),
                            enabled = false,
                            onScrubStart = {},
                            onScrubPreview = {},
                            onScrubEnd = { _, _ -> },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Lyrics Text Size
                    Text(
                        text = stringResource(id = R.string.settings_lyrics_text_size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = IsaiTheme.colors.onBackground
                    )
                    Text(
                        text = stringResource(id = R.string.settings_lyrics_text_size_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = IsaiTheme.colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LyricsTextSize.entries.forEach { size ->
                            val label = when (size) {
                                LyricsTextSize.SMALL -> stringResource(id = R.string.settings_lyrics_size_small)
                                LyricsTextSize.MEDIUM -> stringResource(id = R.string.settings_lyrics_size_medium)
                                LyricsTextSize.LARGE -> stringResource(id = R.string.settings_lyrics_size_large)
                            }
                            IsaiChip(
                                selected = state.lyricsTextSize == size,
                                onClick = { viewModel.setLyricsTextSize(size) },
                                label = label
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Keep Screen On While Viewing Lyrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_keep_screen_on_lyrics),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = IsaiTheme.colors.onBackground
                            )
                            Text(
                                text = stringResource(id = R.string.settings_keep_screen_on_lyrics_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = state.keepScreenOnWhileLyrics,
                            onCheckedChange = { viewModel.setKeepScreenOnWhileLyrics(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = IsaiTheme.colors.onAccent,
                                checkedTrackColor = IsaiTheme.colors.accent,
                                uncheckedThumbColor = IsaiTheme.colors.onSurfaceVariant,
                                uncheckedTrackColor = IsaiTheme.colors.surfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Lyrics Library Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_lyrics_count),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = IsaiTheme.colors.onBackground
                            )
                            Text(
                                text = stringResource(id = R.string.settings_lyrics_count_desc, state.lyricsCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 2. Vinyl & Playback Section
            SectionHeader(title = stringResource(id = R.string.settings_section_vinyl))

            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_needle_lift_behavior),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = IsaiTheme.colors.onBackground
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IsaiChip(
                            selected = state.liftBehavior == LiftBehavior.PAUSE,
                            onClick = { viewModel.setLiftBehavior(LiftBehavior.PAUSE) },
                            label = stringResource(id = R.string.settings_lift_pause)
                        )
                        IsaiChip(
                            selected = state.liftBehavior == LiftBehavior.STOP,
                            onClick = { viewModel.setLiftBehavior(LiftBehavior.STOP) },
                            label = stringResource(id = R.string.settings_lift_stop)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(
                            id = if (state.liftBehavior == LiftBehavior.PAUSE) {
                                R.string.settings_lift_pause_desc
                            } else {
                                R.string.settings_lift_stop_desc
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = IsaiTheme.colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sleep Timer Trigger
                    OutlinedButton(
                        onClick = { showSleepTimerDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = IsaiTheme.colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.settings_sleep_timer_btn),
                            color = IsaiTheme.colors.onBackground,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Equalizer & Audio Effects Trigger
                    OutlinedButton(
                        onClick = onOpenEqualizerClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = IsaiTheme.colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.settings_equalizer),
                            color = IsaiTheme.colors.onBackground,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Library Management Section
            SectionHeader(title = stringResource(id = R.string.settings_section_library))

            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.settings_current_folder),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = IsaiTheme.colors.onSurfaceVariant
                    )
                    Text(
                        text = state.selectedFolderName ?: stringResource(id = R.string.settings_no_folder),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = IsaiTheme.colors.onBackground
                    )

                    if (state.scanStatusMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.scanStatusMessage ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = IsaiTheme.colors.accent
                        )
                    }

                    if (state.purgedCount != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(id = R.string.settings_status_purged, state.purgedCount ?: 0),
                            style = MaterialTheme.typography.labelSmall,
                            color = IsaiTheme.colors.accent
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.rescanLibrary() },
                            enabled = !state.isScanning && state.selectedFolderUri != null,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IsaiTheme.colors.accent,
                                contentColor = IsaiTheme.colors.onAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (state.isScanning) {
                                CircularProgressIndicator(
                                    color = IsaiTheme.colors.onAccent,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.settings_action_rescan),
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        OutlinedButton(
                            onClick = { folderPickerLauncher.launch(null) },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = IsaiTheme.colors.onBackground,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(id = R.string.settings_action_change_folder),
                                color = IsaiTheme.colors.onBackground,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Clean Missing Songs
                    OutlinedButton(
                        onClick = { showPurgeConfirmDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            tint = IsaiTheme.colors.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(id = R.string.settings_action_purge_missing),
                            color = IsaiTheme.colors.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Waveform Cache Info & Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_waveform_cache),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = IsaiTheme.colors.onBackground
                            )
                            Text(
                                text = stringResource(id = R.string.settings_waveform_cache_desc, state.waveformCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.clearWaveformCache() },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_action_clear_waveform_cache),
                                style = MaterialTheme.typography.labelMedium,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Pre-generate while charging
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_pregenerate_waveforms),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = IsaiTheme.colors.onBackground
                            )
                            Text(
                                text = stringResource(id = R.string.settings_pregenerate_waveforms_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = state.preGenerateWaveforms,
                            onCheckedChange = { viewModel.setPreGenerateWaveforms(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = IsaiTheme.colors.onAccent,
                                checkedTrackColor = IsaiTheme.colors.accent,
                                uncheckedThumbColor = IsaiTheme.colors.onSurfaceVariant,
                                uncheckedTrackColor = IsaiTheme.colors.surfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Lyrics Library Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_lyrics_count),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = IsaiTheme.colors.onBackground
                            )
                            Text(
                                text = stringResource(id = R.string.settings_lyrics_count_desc, state.lyricsCount),
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 4. About & Privacy Section
            SectionHeader(title = stringResource(id = R.string.settings_section_about))

            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IsaiTheme.colors.onBackground
                    )
                    Text(
                        text = stringResource(id = R.string.settings_app_version, "1.0.0"),
                        style = MaterialTheme.typography.labelSmall,
                        color = IsaiTheme.colors.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 100% Offline Guarantee Badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IsaiTheme.colors.accent.copy(alpha = 0.12f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = IsaiTheme.colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(id = R.string.settings_offline_badge_title),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = IsaiTheme.colors.onBackground
                            )
                            Text(
                                text = stringResource(id = R.string.settings_offline_badge_desc),
                                style = MaterialTheme.typography.labelSmall,
                                color = IsaiTheme.colors.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Developer Labs Shortcut
                    OutlinedButton(
                        onClick = onOpenLabsClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = null,
                            tint = IsaiTheme.colors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(id = R.string.settings_action_open_labs),
                            color = IsaiTheme.colors.onBackground,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Purge Missing Tracks Confirmation Dialog
    if (showPurgeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showPurgeConfirmDialog = false },
            containerColor = IsaiTheme.colors.surface,
            title = {
                Text(
                    text = stringResource(id = R.string.settings_purge_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IsaiTheme.colors.onBackground
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.settings_purge_confirm_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = IsaiTheme.colors.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPurgeConfirmDialog = false
                        viewModel.purgeMissingTracks()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.settings_purge_confirm_btn),
                        color = IsaiTheme.colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPurgeConfirmDialog = false }) {
                    Text(
                        text = stringResource(id = R.string.playlist_dialog_cancel_btn),
                        color = IsaiTheme.colors.onSurfaceVariant
                    )
                }
            }
        )
    }

    // Sleep Timer Dialog
    if (showSleepTimerDialog) {
        SleepTimerDialog(
            remainingMs = state.sleepTimerRemainingMs,
            isStopAtEndOfTrack = state.isStopAtEndOfTrack,
            onStartTimer = { durationMs, stopAtEnd ->
                viewModel.startSleepTimer(durationMs, stopAtEnd)
            },
            onCancelTimer = { viewModel.cancelSleepTimer() },
            onDismissRequest = { showSleepTimerDialog = false }
        )
    }
}

@Preview(name = "SettingsScreen - Dark Theme")
@Composable
private fun SettingsScreenDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            SettingsScreen(
                currentThemeMode = ThemeMode.DARK,
                onThemeModeChange = {},
                onBackClick = {},
                onOpenLabsClick = {}
            )
        }
    }
}
