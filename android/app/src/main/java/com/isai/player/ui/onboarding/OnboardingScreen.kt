package com.isai.player.ui.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.player.R
import com.isai.player.data.repository.ThemeMode
import com.isai.player.data.scanner.ScanState
import com.isai.player.ui.components.IsaiPrimaryButton
import com.isai.player.ui.theme.IsaiTheme
import kotlinx.coroutines.delay

@Composable
fun OnboardingScreen(
    onScanCompleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onFolderSelected(uri)
        }
    }

    OnboardingContent(
        scanState = scanState,
        onChooseFolderClick = { launcher.launch(null) },
        onScanCompleted = onScanCompleted,
        modifier = modifier
    )
}

@Composable
fun OnboardingContent(
    scanState: ScanState,
    onChooseFolderClick: () -> Unit,
    onScanCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(scanState) {
        if (scanState is ScanState.Completed) {
            delay(800L) // Brief pause to show completion summary
            onScanCompleted()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (scanState) {
            is ScanState.Idle -> {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    tint = IsaiTheme.colors.accent,
                    modifier = Modifier.size(88.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(id = R.string.onboarding_title),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = IsaiTheme.colors.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(id = R.string.onboarding_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = IsaiTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                IsaiPrimaryButton(
                    onClick = onChooseFolderClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(id = R.string.onboarding_action_choose_folder),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            is ScanState.Scanning -> {
                CircularProgressIndicator(
                    color = IsaiTheme.colors.accent,
                    modifier = Modifier.size(64.dp),
                    strokeWidth = 4.dp
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = stringResource(id = R.string.scanner_scanning_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IsaiTheme.colors.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (scanState.totalDiscovered > 0) {
                        "Processing ${scanState.scannedCount} of ${scanState.totalDiscovered} tracks…"
                    } else {
                        "Discovering audio files…"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = IsaiTheme.colors.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                LinearProgressIndicator(
                    progress = { scanState.progress },
                    color = IsaiTheme.colors.accent,
                    trackColor = IsaiTheme.colors.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = scanState.currentFileName,
                    style = MaterialTheme.typography.labelSmall,
                    color = IsaiTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            is ScanState.Completed -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = IsaiTheme.colors.accent,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Library Ready",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IsaiTheme.colors.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${scanState.totalTracks} songs ready for offline listening",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IsaiTheme.colors.onSurfaceVariant
                )
            }

            is ScanState.Error -> {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(72.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = stringResource(id = R.string.scanner_status_error),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IsaiTheme.colors.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = scanState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = IsaiTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                IsaiPrimaryButton(
                    onClick = onChooseFolderClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(id = R.string.global_action_retry))
                }
            }
        }
    }
}

@Preview(name = "OnboardingScreen - Idle - Dark Theme")
@Composable
private fun OnboardingScreenIdleDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            OnboardingContent(
                scanState = ScanState.Idle,
                onChooseFolderClick = {},
                onScanCompleted = {}
            )
        }
    }
}

@Preview(name = "OnboardingScreen - Scanning - Dark Theme")
@Composable
private fun OnboardingScreenScanningDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            OnboardingContent(
                scanState = ScanState.Scanning(
                    scannedCount = 42,
                    totalDiscovered = 120,
                    currentFileName = "Sundari Kannal Oru Sethi.flac"
                ),
                onChooseFolderClick = {},
                onScanCompleted = {}
            )
        }
    }
}

@Preview(name = "OnboardingScreen - Completed - Light Theme")
@Composable
private fun OnboardingScreenCompletedLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        Surface(color = IsaiTheme.colors.background) {
            OnboardingContent(
                scanState = ScanState.Completed(
                    addedCount = 120,
                    updatedCount = 0,
                    missingCount = 0,
                    totalTracks = 120
                ),
                onChooseFolderClick = {},
                onScanCompleted = {}
            )
        }
    }
}
