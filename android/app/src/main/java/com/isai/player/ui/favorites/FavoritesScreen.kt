package com.isai.player.ui.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.player.R
import com.isai.player.data.model.Track
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.components.EmptyState
import com.isai.player.ui.components.SectionHeader
import com.isai.player.ui.components.TrackRow
import com.isai.player.ui.library.TrackInfoDialog
import com.isai.player.ui.library.TrackMenuBottomSheet
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onTrackClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMenuTrack by remember { mutableStateOf<Track?>(null) }
    var selectedInfoTrack by remember { mutableStateOf<Track?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IsaiTheme.colors.background)
    ) {
        SectionHeader(
            title = stringResource(id = R.string.nav_tab_favorites)
        )

        if (state.isLoading && state.tracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.loading_state_default_message),
                    color = IsaiTheme.colors.onSurfaceVariant
                )
            }
        } else if (state.tracks.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Favorite,
                title = stringResource(id = R.string.favorites_empty_title),
                message = stringResource(id = R.string.favorites_empty_message),
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Favorites Hero Header
                item {
                    FavoritesHeroHeader(
                        trackCount = state.tracks.size,
                        totalDurationMs = state.totalDurationMs,
                        onPlayAll = { viewModel.playAll() },
                        onShuffle = { viewModel.shuffleAll() }
                    )
                }

                // Track Rows
                items(state.tracks, key = { it.id }) { track ->
                    TrackRow(
                        track = track,
                        onClick = {
                            viewModel.playTrack(track)
                            onTrackClick(track.id.toString())
                        },
                        onMoreOptions = { selectedMenuTrack = track }
                    )
                }
            }
        }
    }

    // 3-dot Context Menu
    selectedMenuTrack?.let { track ->
        TrackMenuBottomSheet(
            sheetState = sheetState,
            track = track,
            isFavorite = true, // It is in Favorites
            onPlayNext = { viewModel.playNext(track) },
            onAddToQueue = { viewModel.addToQueue(track) },
            onToggleFavorite = { viewModel.toggleFavorite(track.id) },
            onShowInfo = { selectedInfoTrack = track },
            onDismissRequest = { selectedMenuTrack = null }
        )
    }

    // Track Metadata Dialog
    selectedInfoTrack?.let { track ->
        TrackInfoDialog(
            track = track,
            onDismissRequest = { selectedInfoTrack = null }
        )
    }
}

@Composable
private fun FavoritesHeroHeader(
    trackCount: Int,
    totalDurationMs: Long,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Red heart accent badge
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(IsaiTheme.colors.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = IsaiTheme.colors.accent,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(
                id = R.string.favorites_stats,
                trackCount,
                Formatters.formatDurationMs(totalDurationMs)
            ),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = IsaiTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Play All and Shuffle Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onPlayAll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IsaiTheme.colors.accent,
                    contentColor = IsaiTheme.colors.onAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.favorites_play_all),
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onShuffle,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = IsaiTheme.colors.onBackground,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.favorites_shuffle),
                    color = IsaiTheme.colors.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Preview(name = "FavoritesScreen - Dark Theme")
@Composable
private fun FavoritesScreenDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            FavoritesScreen()
        }
    }
}
