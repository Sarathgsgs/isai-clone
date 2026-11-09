package com.isai.player.ui.library

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import com.isai.player.R
import com.isai.player.data.model.Album
import com.isai.player.data.model.Artist
import com.isai.player.data.model.Folder
import com.isai.player.data.model.Playlist
import com.isai.player.data.model.Track
import com.isai.player.data.model.TrackSortOrder
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.components.AlbumCard
import com.isai.player.ui.components.EmptyState
import com.isai.player.ui.components.TrackRow
import com.isai.player.ui.playlists.AddToPlaylistDialog
import com.isai.player.ui.playlists.CreatePlaylistDialog
import com.isai.player.ui.theme.IsaiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onTrackClick: (String) -> Unit,
    onAlbumClick: (String) -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    onPlaylistClick: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTabIndex.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val tracks by viewModel.tracks.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteTrackIds.collectAsStateWithLifecycle()

    var activeMenuTrack by remember { mutableStateOf<Track?>(null) }
    var activeInfoTrack by remember { mutableStateOf<Track?>(null) }
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LibraryContent(
        selectedTab = selectedTab,
        sortOrder = sortOrder,
        tracks = tracks,
        albums = albums,
        artists = artists,
        playlists = playlists,
        folders = folders,
        onTabSelected = { viewModel.selectTab(it) },
        onSortOrderChanged = { viewModel.setSortOrder(it) },
        onTrackClick = { track ->
            viewModel.playTrack(track, tracks)
            onTrackClick(track.id.toString())
        },
        onAlbumClick = onAlbumClick,
        onArtistClick = onArtistClick,
        onPlaylistClick = onPlaylistClick,
        onCreatePlaylistClick = { showCreatePlaylistDialog = true },
        onTrackMoreClick = { track ->
            activeMenuTrack = track
        },
        modifier = modifier
    )

    // 3-dot Context Menu
    activeMenuTrack?.let { track ->
        TrackMenuBottomSheet(
            sheetState = menuSheetState,
            track = track,
            isFavorite = track.id in favoriteIds,
            onPlayNext = { viewModel.playNext(track) },
            onAddToQueue = { viewModel.addToQueue(track) },
            onToggleFavorite = { viewModel.toggleFavorite(track.id) },
            onAddToPlaylist = { trackToAddToPlaylist = track },
            onShowInfo = { activeInfoTrack = track },
            onDismissRequest = { activeMenuTrack = null }
        )
    }

    // Add to Playlist Dialog
    trackToAddToPlaylist?.let { track ->
        AddToPlaylistDialog(
            playlists = playlists,
            onSelectPlaylist = { playlistId ->
                viewModel.addTracksToPlaylist(playlistId, listOf(track.id))
                trackToAddToPlaylist = null
            },
            onCreateNewPlaylist = { name ->
                viewModel.createPlaylist(name, listOf(track.id))
                trackToAddToPlaylist = null
            },
            onDismissRequest = { trackToAddToPlaylist = null }
        )
    }

    // Create New Playlist Dialog from Library Playlists tab
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                viewModel.createPlaylist(name)
                showCreatePlaylistDialog = false
            },
            onDismissRequest = { showCreatePlaylistDialog = false }
        )
    }

    // Song Info Dialog
    activeInfoTrack?.let { track ->
        TrackInfoDialog(
            track = track,
            onDismissRequest = { activeInfoTrack = null }
        )
    }
}

@Composable
fun LibraryContent(
    selectedTab: Int,
    sortOrder: TrackSortOrder,
    tracks: List<Track>,
    albums: List<Album>,
    artists: List<Artist>,
    playlists: List<Playlist>,
    folders: List<Folder>,
    onTabSelected: (Int) -> Unit,
    onSortOrderChanged: (TrackSortOrder) -> Unit,
    onTrackClick: (Track) -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onTrackMoreClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabTitles = listOf(
        stringResource(id = R.string.library_tab_songs),
        stringResource(id = R.string.library_tab_albums),
        stringResource(id = R.string.library_tab_artists),
        stringResource(id = R.string.library_tab_playlists),
        stringResource(id = R.string.library_tab_folders)
    )

    Column(modifier = modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = IsaiTheme.colors.background,
            contentColor = IsaiTheme.colors.accent,
            edgePadding = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) IsaiTheme.colors.accent else IsaiTheme.colors.onSurfaceVariant
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> SongsTab(
                tracks = tracks,
                sortOrder = sortOrder,
                onSortOrderChanged = onSortOrderChanged,
                onTrackClick = onTrackClick,
                onTrackMoreClick = onTrackMoreClick
            )
            1 -> AlbumsTab(
                albums = albums,
                onAlbumClick = onAlbumClick
            )
            2 -> ArtistsTab(
                artists = artists,
                onArtistClick = onArtistClick
            )
            3 -> PlaylistsTab(
                playlists = playlists,
                onPlaylistClick = onPlaylistClick,
                onCreatePlaylistClick = onCreatePlaylistClick
            )
            4 -> FoldersTab(folders = folders)
        }
    }
}

@Composable
private fun SongsTab(
    tracks: List<Track>,
    sortOrder: TrackSortOrder,
    onSortOrderChanged: (TrackSortOrder) -> Unit,
    onTrackClick: (Track) -> Unit,
    onTrackMoreClick: (Track) -> Unit
) {
    var sortMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-header with count and sort picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.library_track_count, tracks.size),
                style = MaterialTheme.typography.labelMedium,
                color = IsaiTheme.colors.onSurfaceVariant
            )

            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { sortMenuExpanded = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = stringResource(id = R.string.library_sort_button_desc),
                        tint = IsaiTheme.colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (sortOrder) {
                            TrackSortOrder.TITLE_ASC -> stringResource(id = R.string.library_sort_title_asc)
                            TrackSortOrder.ARTIST_ASC -> stringResource(id = R.string.library_sort_artist_asc)
                            TrackSortOrder.ALBUM_ASC -> stringResource(id = R.string.library_sort_album_asc)
                            TrackSortOrder.YEAR_DESC -> stringResource(id = R.string.library_sort_year_desc)
                            TrackSortOrder.DATE_ADDED_DESC -> stringResource(id = R.string.library_sort_date_added)
                            else -> stringResource(id = R.string.library_sort_title_asc)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = IsaiTheme.colors.accent
                    )
                }

                DropdownMenu(
                    expanded = sortMenuExpanded,
                    onDismissRequest = { sortMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.library_sort_title_asc)) },
                        onClick = {
                            onSortOrderChanged(TrackSortOrder.TITLE_ASC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.library_sort_artist_asc)) },
                        onClick = {
                            onSortOrderChanged(TrackSortOrder.ARTIST_ASC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.library_sort_album_asc)) },
                        onClick = {
                            onSortOrderChanged(TrackSortOrder.ALBUM_ASC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.library_sort_year_desc)) },
                        onClick = {
                            onSortOrderChanged(TrackSortOrder.YEAR_DESC)
                            sortMenuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.library_sort_date_added)) },
                        onClick = {
                            onSortOrderChanged(TrackSortOrder.DATE_ADDED_DESC)
                            sortMenuExpanded = false
                        }
                    )
                }
            }
        }

        if (tracks.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Folder,
                title = stringResource(id = R.string.empty_state_default_title),
                message = stringResource(id = R.string.empty_state_default_message),
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                items(
                    items = tracks,
                    key = { it.id }
                ) { track ->
                    TrackRow(
                        track = track,
                        onClick = { onTrackClick(track) },
                        onMoreOptions = { onTrackMoreClick(track) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumsTab(
    albums: List<Album>,
    onAlbumClick: (String) -> Unit
) {
    if (albums.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Folder,
            title = stringResource(id = R.string.library_empty_albums_title),
            message = stringResource(id = R.string.library_empty_albums_message)
        )
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = albums,
                key = { "${it.title}_${it.artist}" }
            ) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album.title) }
                )
            }
        }
    }
}

@Composable
private fun ArtistsTab(
    artists: List<Artist>,
    onArtistClick: (String) -> Unit
) {
    if (artists.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Person,
            title = stringResource(id = R.string.library_empty_artists_title),
            message = stringResource(id = R.string.library_empty_artists_message)
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = artists,
                key = { it.name }
            ) { artist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onArtistClick(artist.name) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
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
                                tint = IsaiTheme.colors.accent,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = artist.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = IsaiTheme.colors.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                id = R.string.library_artist_stats,
                                artist.albumCount,
                                artist.trackCount
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = IsaiTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistsTab(
    playlists: List<Playlist>,
    onPlaylistClick: (Long) -> Unit,
    onCreatePlaylistClick: () -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // "+ Create Playlist" Action Row
        item {
            OutlinedButton(
                onClick = onCreatePlaylistClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = IsaiTheme.colors.accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.playlist_dialog_create_title),
                    color = IsaiTheme.colors.accent,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }

        if (playlists.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.PlaylistPlay,
                    title = stringResource(id = R.string.library_empty_playlists_title),
                    message = stringResource(id = R.string.library_empty_playlists_message),
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        } else {
            items(
                items = playlists,
                key = { it.id }
            ) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlaylistClick(playlist.id) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(IsaiTheme.colors.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistPlay,
                            contentDescription = null,
                            tint = IsaiTheme.colors.accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = playlist.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = IsaiTheme.colors.onBackground
                        )
                        Text(
                            text = stringResource(id = R.string.library_track_count, playlist.trackCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = IsaiTheme.colors.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FoldersTab(folders: List<Folder>) {
    if (folders.isEmpty()) {
        EmptyState(
            icon = Icons.Default.Folder,
            title = stringResource(id = R.string.library_empty_folders_title),
            message = stringResource(id = R.string.library_empty_folders_message)
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = folders,
                key = { it.path }
            ) { folder ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {}
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = IsaiTheme.colors.accent,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = IsaiTheme.colors.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                id = R.string.library_folder_stats,
                                folder.trackCount,
                                folder.path
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = IsaiTheme.colors.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "LibraryScreen - Dark Theme")
@Composable
private fun LibraryScreenDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            LibraryContent(
                selectedTab = 0,
                sortOrder = TrackSortOrder.TITLE_ASC,
                tracks = emptyList(),
                albums = emptyList(),
                artists = emptyList(),
                playlists = emptyList(),
                folders = emptyList(),
                onTabSelected = {},
                onSortOrderChanged = {},
                onTrackClick = {},
                onAlbumClick = {},
                onArtistClick = {},
                onPlaylistClick = {},
                onCreatePlaylistClick = {},
                onTrackMoreClick = {}
            )
        }
    }
}
