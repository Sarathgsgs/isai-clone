package com.isai.player.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.data.model.Playlist
import com.isai.player.ui.theme.IsaiTheme

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onSelectPlaylist: (playlistId: Long) -> Unit,
    onCreateNewPlaylist: (name: String) -> Unit,
    onDismissRequest: () -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onConfirm = { name ->
                showCreateDialog = false
                onCreateNewPlaylist(name)
            },
            onDismissRequest = { showCreateDialog = false }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            containerColor = IsaiTheme.colors.surface,
            title = {
                Text(
                    text = stringResource(id = R.string.add_to_playlist_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IsaiTheme.colors.onBackground
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // + New Playlist Action
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateDialog = true }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = IsaiTheme.colors.accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = stringResource(id = R.string.add_to_playlist_new),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = IsaiTheme.colors.accent
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (playlists.isEmpty()) {
                        Text(
                            text = stringResource(id = R.string.add_to_playlist_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = IsaiTheme.colors.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(playlists, key = { it.id }) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectPlaylist(playlist.id) }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlaylistPlay,
                                        contentDescription = null,
                                        tint = IsaiTheme.colors.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = playlist.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
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
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = stringResource(id = R.string.playlist_dialog_cancel_btn),
                        color = IsaiTheme.colors.onSurfaceVariant
                    )
                }
            }
        )
    }
}

