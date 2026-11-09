package com.isai.player.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.isai.player.R
import com.isai.player.data.model.Track
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.components.EmptyState
import com.isai.player.ui.components.VinylColors
import com.isai.player.ui.components.VinylDisc
import com.isai.player.ui.components.rememberVinylColors
import com.isai.player.ui.components.rememberVinylDiscState
import com.isai.player.ui.library.TrackInfoDialog
import com.isai.player.ui.library.TrackMenuBottomSheet
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumKey: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(albumKey) {
        viewModel.setAlbumKey(albumKey)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMenuTrack by remember { mutableStateOf<Track?>(null) }
    var selectedInfoTrack by remember { mutableStateOf<Track?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.albumTitle.ifBlank { albumKey },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
        if (state.isLoading && state.tracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.loading_state_default_message),
                    color = IsaiTheme.colors.onSurfaceVariant
                )
            }
        } else if (state.tracks.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Album,
                title = state.albumTitle.ifBlank { albumKey },
                message = stringResource(id = R.string.empty_state_default_message),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Header with peeking vinyl record
                item {
                    AlbumHeroHeader(
                        title = state.albumTitle,
                        artist = state.albumArtist,
                        artPath = state.artPath,
                        year = state.releaseYear,
                        trackCount = state.tracks.size,
                        totalDurationMs = state.totalDurationMs,
                        onPlayAll = { viewModel.playAll() },
                        onShuffle = { viewModel.shuffleAll() }
                    )
                }

                // Disc-grouped track listing
                state.discGroups.forEach { (discNo, discTracks) ->
                    if (state.discGroups.size > 1) {
                        item {
                            Text(
                                text = stringResource(id = R.string.album_detail_disc_header, discNo),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = IsaiTheme.colors.accent,
                                modifier = Modifier.padding(start = 20.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }
                    }

                    itemsIndexed(discTracks, key = { _, track -> track.id }) { index, track ->
                        AlbumTrackRow(
                            trackIndex = if (track.trackNo > 0) track.trackNo else index + 1,
                            track = track,
                            onClick = { viewModel.playTrack(track) },
                            onMoreOptions = { selectedMenuTrack = track }
                        )
                    }
                }
            }
        }
    }

    // 3-dot Context Menu
    selectedMenuTrack?.let { track ->
        val isFav = state.favoriteTrackIds.contains(track.id)
        TrackMenuBottomSheet(
            sheetState = sheetState,
            track = track,
            isFavorite = isFav,
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
private fun AlbumHeroHeader(
    title: String,
    artist: String,
    artPath: String?,
    year: Int,
    trackCount: Int,
    totalDurationMs: Long,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    val vinylDiscState = rememberVinylDiscState()
    val vinylColors = rememberVinylColors()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Signature Peeking Vinyl Sleeve
        Box(
            modifier = Modifier
                .size(240.dp, 160.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Peeking Vinyl Disc (shifted 56dp right and rotated 12°)
            Box(
                modifier = Modifier
                    .offset(x = 64.dp)
                    .size(150.dp)
                    .rotate(12f)
            ) {
                VinylDisc(
                    state = vinylDiscState,
                    art = artPath,
                    fallbackSeed = title,
                    colors = vinylColors,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Outer Vinyl Jacket / Sleeve Card
            Card(
                shape = RoundedCornerShape(10.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
                modifier = Modifier
                    .size(150.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (artPath != null) {
                        SubcomposeAsyncImage(
                            model = artPath,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(IsaiTheme.colors.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Album,
                                contentDescription = null,
                                tint = IsaiTheme.colors.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Artist Info
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = IsaiTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (artist.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = IsaiTheme.colors.accent,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        val durationFormatted = Formatters.formatDurationMs(totalDurationMs)
        val infoText = if (year > 0) {
            "$year • " + stringResource(id = R.string.album_detail_track_count, trackCount, durationFormatted)
        } else {
            stringResource(id = R.string.album_detail_track_count, trackCount, durationFormatted)
        }

        Text(
            text = infoText,
            style = MaterialTheme.typography.labelMedium,
            color = IsaiTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

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
                    text = stringResource(id = R.string.album_detail_play_all),
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
                    text = stringResource(id = R.string.album_detail_shuffle),
                    color = IsaiTheme.colors.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun AlbumTrackRow(
    trackIndex: Int,
    track: Track,
    onClick: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Track number index
        Text(
            text = trackIndex.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = IsaiTheme.colors.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
            textAlign = TextAlign.Start
        )

        // Title and artist
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = IsaiTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (track.artist.isNotBlank()) {
                Text(
                    text = track.artist,
                    style = MaterialTheme.typography.labelSmall,
                    color = IsaiTheme.colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Duration
        Text(
            text = Formatters.formatDurationMs(track.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = IsaiTheme.colors.onSurfaceVariant
        )

        // 3-dot more options
        IconButton(
            onClick = onMoreOptions,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(id = R.string.global_action_more_options),
                tint = IsaiTheme.colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(name = "AlbumDetailScreen - Dark Theme")
@Composable
private fun AlbumDetailScreenDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            AlbumDetailScreen(albumKey = "Roja", onBackClick = {})
        }
    }
}
