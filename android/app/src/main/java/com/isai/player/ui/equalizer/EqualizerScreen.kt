package com.isai.player.ui.equalizer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.player.R
import com.isai.player.data.equalizer.EqPresets
import com.isai.player.ui.theme.CardShape
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showSaveDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.equalizer_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_navigate_back)
                        )
                    }
                },
                actions = {
                    Switch(
                        checked = uiState.state.enabled,
                        onCheckedChange = { viewModel.toggleEnabled(it) },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Preset Selector Row
            PresetSelectorRow(
                currentPresetName = uiState.state.presetName,
                customPresets = uiState.customPresets,
                enabled = uiState.state.enabled,
                onSelectPreset = { viewModel.selectPreset(it) },
                onSaveCustomClick = { showSaveDialog = true }
            )

            // 2. Catmull-Rom Curve View Card
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    EqCurveView(
                        bandGainsMilliBels = uiState.state.bandGains,
                        minLevelMilliBels = uiState.capabilities.minLevelMilliBels,
                        maxLevelMilliBels = uiState.capabilities.maxLevelMilliBels,
                        enabled = uiState.state.enabled,
                        onBandGainChanged = { idx, gain -> viewModel.updateBandGain(idx, gain) },
                        accentColor = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Frequency Labels Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        uiState.capabilities.centerFreqsHz.forEach { freq ->
                            Text(
                                text = formatFreq(freq),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Band Sliders
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.equalizer_bands_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        uiState.state.bandGains.forEachIndexed { index, gainMilliBels ->
                            val freqHz = uiState.capabilities.centerFreqsHz.getOrElse(index) { 1000 }
                            BandSliderItem(
                                bandIndex = index,
                                freqHz = freqHz,
                                gainMilliBels = gainMilliBels,
                                minMilliBels = uiState.capabilities.minLevelMilliBels,
                                maxMilliBels = uiState.capabilities.maxLevelMilliBels,
                                enabled = uiState.state.enabled,
                                onGainChange = { newGain -> viewModel.updateBandGain(index, newGain) }
                            )
                        }
                    }
                }
            }

            // 4. Audio Effects (Bass Boost, Virtualizer, Preamp)
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.equalizer_effects_header),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Bass Boost Slider
                    if (uiState.capabilities.isBassBoostSupported) {
                        EffectSliderRow(
                            label = stringResource(R.string.equalizer_bass_boost),
                            valuePercent = (uiState.state.bassBoostStrength / 10f).roundToInt(),
                            valueFraction = uiState.state.bassBoostStrength / 1000f,
                            enabled = uiState.state.enabled,
                            onValueChange = { viewModel.updateBassBoost((it * 1000).roundToInt()) }
                        )
                    }

                    // Virtualizer Slider
                    if (uiState.capabilities.isVirtualizerSupported) {
                        EffectSliderRow(
                            label = stringResource(R.string.equalizer_virtualizer),
                            valuePercent = (uiState.state.virtualizerStrength / 10f).roundToInt(),
                            valueFraction = uiState.state.virtualizerStrength / 1000f,
                            enabled = uiState.state.enabled,
                            onValueChange = { viewModel.updateVirtualizer((it * 1000).roundToInt()) }
                        )
                    }

                    // Preamp Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.equalizer_preamp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format("%+.1f dB", uiState.state.preampDb),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = uiState.state.preampDb,
                            onValueChange = { viewModel.updatePreamp(((it * 2).roundToInt() / 2f)) },
                            valueRange = -12.0f..12.0f,
                            steps = 47, // 0.5 dB steps between -12 and +12
                            enabled = uiState.state.enabled,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    // Prevent Clipping Switch & Status Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.equalizer_prevent_clipping),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = stringResource(R.string.equalizer_prevent_clipping_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.state.preventClipping,
                            onCheckedChange = { viewModel.togglePreventClipping(it) },
                            enabled = uiState.state.enabled
                        )
                    }

                    // Clipping Status Warning / Info Badge
                    if (uiState.state.enabled && uiState.clippingAnalysis.isClippingPotential) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.state.preventClipping) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.state.preventClipping) Icons.Default.Shield else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (uiState.state.preventClipping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (uiState.state.preventClipping) {
                                        stringResource(
                                            R.string.equalizer_clipping_protected,
                                            String.format("%.1f", -uiState.clippingAnalysis.attenuationDb)
                                        )
                                    } else {
                                        stringResource(R.string.equalizer_clipping_warning)
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (uiState.state.preventClipping) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // 5. System Equalizer Launch Button
            OutlinedButton(
                onClick = { viewModel.launchSystemEqualizer(context) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.equalizer_open_system))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                viewModel.saveCustomPreset(name)
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun PresetSelectorRow(
    currentPresetName: String,
    customPresets: List<com.isai.player.data.equalizer.EqPreset>,
    enabled: Boolean,
    onSelectPreset: (com.isai.player.data.equalizer.EqPreset) -> Unit,
    onSaveCustomClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Built-in presets
        EqPresets.BUILT_IN_PRESETS.forEach { preset ->
            val isSelected = currentPresetName == preset.name
            FilterChip(
                selected = isSelected,
                onClick = { onSelectPreset(preset) },
                label = { Text(preset.name) },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        // Custom presets
        customPresets.forEach { preset ->
            val isSelected = currentPresetName == preset.name
            FilterChip(
                selected = isSelected,
                onClick = { onSelectPreset(preset) },
                label = { Text("⭐ ${preset.name}") },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }

        // Save Custom Preset Button
        OutlinedButton(
            onClick = onSaveCustomClick,
            enabled = enabled,
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.equalizer_save_preset))
        }
    }
}

@Composable
private fun BandSliderItem(
    bandIndex: Int,
    freqHz: Int,
    gainMilliBels: Int,
    minMilliBels: Int,
    maxMilliBels: Int,
    enabled: Boolean,
    onGainChange: (Int) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(52.dp)
    ) {
        // Gain display (e.g. +3.5 dB)
        val gainDb = gainMilliBels / 100f
        Text(
            text = String.format("%+.1f", gainDb),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (gainMilliBels != 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Vertical Slider column representation
        Box(
            modifier = Modifier
                .height(140.dp)
                .width(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Slider(
                value = gainMilliBels.toFloat(),
                onValueChange = {
                    val snapped = (((it + 25) / 50).toInt()) * 50
                    onGainChange(snapped.coerceIn(minMilliBels, maxMilliBels))
                },
                valueRange = minMilliBels.toFloat()..maxMilliBels.toFloat(),
                enabled = enabled,
                modifier = Modifier
                    .size(width = 140.dp, height = 28.dp)
                    .androidx.compose.ui.draw.rotate(-90f),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Frequency
        Text(
            text = formatFreq(freqHz),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EffectSliderRow(
    label: String,
    valuePercent: Int,
    valueFraction: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$valuePercent%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = valueFraction,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var presetName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.equalizer_save_custom_preset_title)) },
        text = {
            Column {
                Text(stringResource(R.string.equalizer_save_custom_preset_prompt))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = presetName,
                    onValueChange = { presetName = it },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.equalizer_preset_name_placeholder)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (presetName.isNotBlank()) onSave(presetName.trim()) },
                enabled = presetName.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private fun formatFreq(freqHz: Int): String {
    return if (freqHz >= 1000) {
        val kHz = freqHz / 1000f
        if (kHz % 1f == 0f) "${kHz.toInt()}k" else String.format("%.1fk", kHz)
    } else {
        "$freqHz"
    }
}

