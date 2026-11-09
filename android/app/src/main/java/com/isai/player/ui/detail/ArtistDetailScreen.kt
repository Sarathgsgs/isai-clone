package com.isai.player.ui.detail

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.isai.player.data.model.Album
import com.isai.player.data.model.Track
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.components.AlbumCard
import com.isai.player.ui.components.EmptyState
import com.isai.player.ui.components.TrackRow
import com.isai.player.ui.library.TrackInfoDialog
import com.isai.player.ui.library.TrackMenuBottomSheet
import com.isai.player.ui.theme.IsaiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailScreen(
    artistKey: String,
    onBackClick: () -> Unit,
    onAlbumClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ArtistDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(artistKey) {
        viewModel.setArtistKey(artistKey)
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
                        text = state.artistName.ifBlank { artistKey },
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
                icon = Icons.Default.Person,
                title = state.artistName.ifBlank { artistKey },
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
                // Artist Hero Header
                item {
                    ArtistHeroHeader(
                        artistName = state.artistName,
                        sampleArt = state.sampleArtPath,
                        albumCount = state.albums.size,
                        trackCount = state.tracks.size,
                        onPlayAll = { viewModel.playAll() },
                        onShuffle = { viewModel.shuffleAll() }
                    )
                }

                // Discography Carousel
                if (state.albums.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            Text(
                                text = stringResource(id = R.string.artist_detail_section_discography),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IsaiTheme.colors.onBackground,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                items(state.albums, key = { it.title }) { albumSummary ->
                                    val album = Album(
                                        title = albumSummary.title,
                                        artist = state.artistName,
                                        artPath = albumSummary.artPath,
                                        trackCount = albumSummary.trackCount,
                                        year = albumSummary.year
                                    )
                                    AlbumCard(
                                        album = album,
                                        onClick = { onAlbumClick(albumSummary.title) },
                                        modifier = Modifier.width(140.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // All Songs Header
                item {
                    Text(
                        text = stringResource(id = R.string.artist_detail_section_all_songs),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IsaiTheme.colors.onBackground,
                        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
                    )
                }

                // Track List
                itemsIndexed(state.tracks, key = { _, track -> track.id }) { _, track ->
                    TrackRow(
                        track = track,
                        onClick = { viewModel.playTrack(track) },
                        onMoreOptions = { selectedMenuTrack = track }
                    )
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
private fun ArtistHeroHeader(
    artistName: String,
    sampleArt: String?,
    albumCount: Int,
    trackCount: Int,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Large Circular Avatar
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(IsaiTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (sampleArt != null) {
                SubcomposeAsyncImage(
                    model = sampleArt,
                    contentDescription = artistName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = IsaiTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(56.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Name
        Text(
            text = artistName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = IsaiTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Stats
        Text(
            text = stringResource(id = R.string.artist_detail_stats, albumCount, trackCount),
            style = MaterialTheme.typography.labelMedium,
            color = IsaiTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Buttons
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
    }
}

@Preview(name = "ArtistDetailScreen - Light Theme")
@Composable
private fun ArtistDetailScreenLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        Surface(color = IsaiTheme.colors.background) {
            ArtistDetailScreen(artistKey = "A. R. Rahman", onBackClick = {})
        }
    }
}
