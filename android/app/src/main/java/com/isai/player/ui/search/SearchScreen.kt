package com.isai.player.ui.search

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.isai.player.data.model.Artist
import com.isai.player.data.model.Track
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.components.AlbumCard
import com.isai.player.ui.components.EmptyState
import com.isai.player.ui.components.SectionHeader
import com.isai.player.ui.components.TrackRow
import com.isai.player.ui.library.TrackInfoDialog
import com.isai.player.ui.library.TrackMenuBottomSheet
import com.isai.player.ui.theme.IsaiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onTrackClick: (String) -> Unit = {},
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedMenuTrack by remember { mutableStateOf<Track?>(null) }
    var selectedInfoTrack by remember { mutableStateOf<Track?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IsaiTheme.colors.background)
    ) {
        // Search Input Bar
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = {
                Text(
                    text = stringResource(id = R.string.search_input_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = IsaiTheme.colors.onSurfaceVariant
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = IsaiTheme.colors.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearQuery) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(id = R.string.search_clear_description),
                            tint = IsaiTheme.colors.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = IsaiTheme.colors.surface,
                unfocusedContainerColor = IsaiTheme.colors.surface,
                focusedBorderColor = IsaiTheme.colors.accent,
                unfocusedBorderColor = IsaiTheme.colors.surfaceVariant,
                focusedTextColor = IsaiTheme.colors.onBackground,
                unfocusedTextColor = IsaiTheme.colors.onBackground,
                cursorColor = IsaiTheme.colors.accent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        )

        // Body Content
        if (query.isBlank()) {
            EmptyState(
                icon = Icons.Default.Search,
                title = stringResource(id = R.string.nav_tab_search),
                message = stringResource(id = R.string.search_initial_message),
                modifier = Modifier.weight(1f)
            )
        } else if (state.matchingTracks.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Search,
                title = stringResource(id = R.string.search_empty_title),
                message = stringResource(id = R.string.search_empty_message),
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Albums Match
                if (state.matchingAlbums.isNotEmpty()) {
                    item {
                        SectionHeader(title = stringResource(id = R.string.search_section_albums))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(state.matchingAlbums, key = { it.title }) { album ->
                                AlbumCard(
                                    album = album,
                                    onClick = { onAlbumClick(album.title) },
                                    modifier = Modifier.width(135.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Artists Match
                if (state.matchingArtists.isNotEmpty()) {
                    item {
                        SectionHeader(title = stringResource(id = R.string.search_section_artists))
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.matchingArtists, key = { it.name }) { artist ->
                                SearchArtistChip(
                                    artist = artist,
                                    onClick = { onArtistClick(artist.name) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Songs Match
                if (state.matchingTracks.isNotEmpty()) {
                    item {
                        SectionHeader(title = stringResource(id = R.string.search_section_songs))
                    }

                    items(state.matchingTracks, key = { it.id }) { track ->
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
private fun SearchArtistChip(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(80.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(IsaiTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (artist.sampleArtPath != null) {
                SubcomposeAsyncImage(
                    model = artist.sampleArtPath,
                    contentDescription = artist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = IsaiTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = artist.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = IsaiTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "SearchScreen - Dark Theme")
@Composable
private fun SearchScreenDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            SearchScreen()
        }
    }
}
