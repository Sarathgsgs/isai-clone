package com.isai.player.ui.playlists

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.player.R
import com.isai.player.data.model.Track
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.components.EmptyState
import com.isai.player.ui.library.TrackInfoDialog
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(playlistId) {
        viewModel.setPlaylistId(playlistId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showTrackPickerSheet by remember { mutableStateOf(false) }
    var selectedMenuTrack by remember { mutableStateOf<Track?>(null) }
    var selectedInfoTrack by remember { mutableStateOf<Track?>(null) }

    val trackPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val trackMenuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val playlistName = state.playlist?.name ?: stringResource(id = R.string.nav_screen_playlist_detail)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = playlistName,
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
                actions = {
                    Box {
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(id = R.string.global_action_more_options),
                                tint = IsaiTheme.colors.onBackground
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.playlist_action_rename)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = IsaiTheme.colors.onBackground
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showRenameDialog = true
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(id = R.string.playlist_action_delete),
                                        color = IsaiTheme.colors.accent
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = IsaiTheme.colors.accent
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
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
        if (state.isLoading && state.playlist == null) {
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
                // Hero Header
                item {
                    PlaylistHeroHeader(
                        title = playlistName,
                        trackCount = state.tracks.size,
                        totalDurationMs = state.totalDurationMs,
                        onPlayAll = { viewModel.playAll() },
                        onShuffle = { viewModel.shuffleAll() },
                        onAddSongs = { showTrackPickerSheet = true }
                    )
                }

                if (state.tracks.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Default.QueueMusic,
                            title = stringResource(id = R.string.playlist_detail_empty_title),
                            message = stringResource(id = R.string.playlist_detail_empty_message),
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                } else {
                    itemsIndexed(state.tracks, key = { _, track -> track.id }) { index, track ->
                        PlaylistTrackRow(
                            index = index + 1,
                            track = track,
                            onClick = { viewModel.playTrack(track) },
                            onMoreOptions = { selectedMenuTrack = track }
                        )
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (showRenameDialog) {
        CreatePlaylistDialog(
            initialName = playlistName,
            isRename = true,
            onConfirm = { newName ->
                viewModel.renamePlaylist(newName)
                showRenameDialog = false
            },
            onDismissRequest = { showRenameDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = IsaiTheme.colors.surface,
            title = {
                Text(
                    text = stringResource(id = R.string.playlist_delete_confirm_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IsaiTheme.colors.onBackground
                )
            },
            text = {
                Text(
                    text = stringResource(id = R.string.playlist_delete_confirm_message, playlistName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = IsaiTheme.colors.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deletePlaylist(onDeleted = onBackClick)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.playlist_action_delete),
                        color = IsaiTheme.colors.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(
                        text = stringResource(id = R.string.playlist_dialog_cancel_btn),
                        color = IsaiTheme.colors.onSurfaceVariant
                    )
                }
            }
        )
    }

    // Multi-Select Track Picker Bottom Sheet
    if (showTrackPickerSheet) {
        TrackPickerBottomSheet(
            sheetState = trackPickerSheetState,
            allTracks = state.allLibraryTracks,
            alreadyInPlaylistTrackIds = state.tracks.map { it.id }.toSet(),
            onConfirm = { selectedIds ->
                viewModel.addTracksToPlaylist(selectedIds)
                showTrackPickerSheet = false
            },
            onDismissRequest = { showTrackPickerSheet = false }
        )
    }

    // Playlist Track Context Menu
    selectedMenuTrack?.let { track ->
        val isFav = state.favoriteTrackIds.contains(track.id)
        PlaylistTrackMenuBottomSheet(
            sheetState = trackMenuSheetState,
            track = track,
            isFavorite = isFav,
            onPlayNext = { viewModel.playNext(track) },
            onAddToQueue = { viewModel.addToQueue(track) },
            onToggleFavorite = { viewModel.toggleFavorite(track.id) },
            onRemoveFromPlaylist = { viewModel.removeTrackFromPlaylist(track.id) },
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
private fun PlaylistHeroHeader(
    title: String,
    trackCount: Int,
    totalDurationMs: Long,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onAddSongs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Playlist Icon Badge
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(IsaiTheme.colors.accent.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlaylistPlay,
                contentDescription = null,
                tint = IsaiTheme.colors.accent,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = IsaiTheme.colors.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(
                id = R.string.album_detail_track_count,
                trackCount,
                Formatters.formatDurationMs(totalDurationMs)
            ),
            style = MaterialTheme.typography.labelMedium,
            color = IsaiTheme.colors.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Play All and Shuffle Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onPlayAll,
                enabled = trackCount > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = IsaiTheme.colors.accent,
                    contentColor = IsaiTheme.colors.onAccent
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(id = R.string.global_action_play_all),
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onShuffle,
                enabled = trackCount > 0,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = null,
                    tint = IsaiTheme.colors.onBackground,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(id = R.string.global_action_shuffle),
                    color = IsaiTheme.colors.onBackground,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Add Songs Action Button
        OutlinedButton(
            onClick = onAddSongs,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = IsaiTheme.colors.accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(id = R.string.playlist_action_add_songs),
                color = IsaiTheme.colors.accent,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun PlaylistTrackRow(
    index: Int,
    track: Track,
    onClick: () -> Unit,
    onMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = IsaiTheme.colors.onSurfaceVariant,
            modifier = Modifier.width(28.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = IsaiTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} · ${Formatters.formatDurationMs(track.durationMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = IsaiTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaylistTrackMenuBottomSheet(
    sheetState: androidx.compose.material3.SheetState,
    track: Track,
    isFavorite: Boolean,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemoveFromPlaylist: () -> Unit,
    onShowInfo: () -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = IsaiTheme.colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IsaiTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.artist} · ${track.album}",
                style = MaterialTheme.typography.labelSmall,
                color = IsaiTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            MenuActionRow(
                icon = Icons.Default.QueueMusic,
                title = stringResource(id = R.string.track_menu_action_play_next),
                onClick = {
                    onPlayNext()
                    onDismissRequest()
                }
            )

            MenuActionRow(
                icon = Icons.Default.PlaylistAdd,
                title = stringResource(id = R.string.track_menu_action_add_queue),
                onClick = {
                    onAddToQueue()
                    onDismissRequest()
                }
            )

            MenuActionRow(
                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                title = stringResource(
                    id = if (isFavorite) {
                        R.string.track_menu_action_remove_favorite
                    } else {
                        R.string.track_menu_action_add_favorite
                    }
                ),
                tint = if (isFavorite) IsaiTheme.colors.accent else IsaiTheme.colors.onBackground,
                onClick = {
                    onToggleFavorite()
                    onDismissRequest()
                }
            )

            MenuActionRow(
                icon = Icons.Default.Delete,
                title = stringResource(id = R.string.playlist_track_menu_remove),
                tint = IsaiTheme.colors.accent,
                onClick = {
                    onRemoveFromPlaylist()
                    onDismissRequest()
                }
            )

            MenuActionRow(
                icon = Icons.Default.Info,
                title = stringResource(id = R.string.track_menu_action_song_info),
                onClick = {
                    onDismissRequest()
                    onShowInfo()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MenuActionRow(
    icon: ImageVector,
    title: String,
    tint: androidx.compose.ui.graphics.Color = IsaiTheme.colors.onBackground,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = IsaiTheme.colors.onBackground
        )
    }
}

@Preview(name = "PlaylistDetailScreen - Dark Theme")
@Composable
private fun PlaylistDetailScreenDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            PlaylistDetailScreen(playlistId = 1L, onBackClick = {})
        }
    }
}
