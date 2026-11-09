package com.isai.player.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.data.model.Track
import com.isai.player.ui.theme.IsaiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackMenuBottomSheet(
    sheetState: SheetState,
    track: Track,
    isFavorite: Boolean,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit = {},
    onShowInfo: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = IsaiTheme.colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Track Header
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

            // Menu Items
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
                icon = Icons.Default.PlaylistPlay,
                title = stringResource(id = R.string.track_menu_action_add_to_playlist),
                onClick = {
                    onDismissRequest()
                    onAddToPlaylist()
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
