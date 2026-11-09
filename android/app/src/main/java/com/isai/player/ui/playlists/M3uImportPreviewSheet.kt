package com.isai.player.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.data.m3u.MatchResolution
import com.isai.player.data.m3u.MatchSummary
import com.isai.player.data.m3u.ResolvedM3uItem
import com.isai.player.data.model.Track
import com.isai.player.ui.theme.CardShape
import com.isai.player.ui.theme.IsaiTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun M3uImportPreviewSheet(
    initialPlaylistName: String,
    matchSummary: MatchSummary,
    onConfirmImport: (playlistName: String, resolvedTracks: List<Track>) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var playlistName by remember { mutableStateOf(initialPlaylistName) }

    // Track state of resolved items allowing user to pick ambiguous candidates
    var currentItems by remember { mutableStateOf(matchSummary.items) }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = IsaiTheme.colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            // Sheet Title
            Text(
                text = stringResource(R.string.m3u_import_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IsaiTheme.colors.onBackground
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Playlist Name Input
            OutlinedTextField(
                value = playlistName,
                onValueChange = { playlistName = it },
                label = { Text(stringResource(R.string.m3u_playlist_name_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Match Summary Card
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    SummaryBadge(
                        count = currentItems.count { it.resolution is MatchResolution.Single },
                        label = stringResource(R.string.m3u_matched),
                        color = Color(0xFF4CAF50)
                    )
                    SummaryBadge(
                        count = currentItems.count { it.resolution is MatchResolution.Ambiguous },
                        label = stringResource(R.string.m3u_ambiguous),
                        color = Color(0xFFFF9800)
                    )
                    SummaryBadge(
                        count = currentItems.count { it.resolution is MatchResolution.None },
                        label = stringResource(R.string.m3u_not_found),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Preview List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(currentItems) { item ->
                    ImportItemRow(
                        item = item,
                        onSelectCandidate = { chosenTrack ->
                            currentItems = currentItems.map {
                                if (it.entry == item.entry) {
                                    it.copy(resolution = MatchResolution.Single(chosenTrack, com.isai.player.data.m3u.MatchStage.FILENAME_AND_DURATION))
                                } else it
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Confirm Import Button
            val readyTracks = currentItems.mapNotNull { (it.resolution as? MatchResolution.Single)?.track }
            Button(
                onClick = {
                    if (playlistName.isNotBlank() && readyTracks.isNotEmpty()) {
                        onConfirmImport(playlistName.trim(), readyTracks)
                    }
                },
                enabled = playlistName.isNotBlank() && readyTracks.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IsaiTheme.colors.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.PlaylistAdd, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.m3u_action_import_count, readyTracks.size),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SummaryBadge(
    count: Int,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IsaiTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
private fun ImportItemRow(
    item: ResolvedM3uItem,
    onSelectCandidate: (Track) -> Unit
) {
    var showCandidatePicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(IsaiTheme.colors.surfaceVariant.copy(alpha = 0.3f))
            .clickable(enabled = item.resolution is MatchResolution.Ambiguous) {
                showCandidatePicker = true
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status Icon
        when (val res = item.resolution) {
            is MatchResolution.Single -> {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(18.dp)
                )
            }
            is MatchResolution.Ambiguous -> {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(18.dp)
                )
            }
            is MatchResolution.None -> {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Title & Path
        Column(modifier = Modifier.weight(1f)) {
            val title = when (val res = item.resolution) {
                is MatchResolution.Single -> res.track.title
                else -> item.entry.title ?: item.entry.normalizedPath.substringAfterLast('/')
            }
            val subtitle = when (val res = item.resolution) {
                is MatchResolution.Single -> res.track.artist
                is MatchResolution.Ambiguous -> stringResource(R.string.m3u_tap_to_resolve, res.candidates.size)
                is MatchResolution.None -> item.entry.normalizedPath
            }

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (item.resolution is MatchResolution.None) IsaiTheme.colors.onSurfaceVariant.copy(alpha = 0.6f) else IsaiTheme.colors.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (item.resolution is MatchResolution.Ambiguous) Color(0xFFFF9800) else IsaiTheme.colors.onSurfaceVariant
            )
        }
    }

    if (showCandidatePicker && item.resolution is MatchResolution.Ambiguous) {
        val candidates = (item.resolution as MatchResolution.Ambiguous).candidates
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCandidatePicker = false },
            title = { Text(stringResource(R.string.m3u_choose_match_title)) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(candidates) { candidate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectCandidate(candidate)
                                    showCandidatePicker = false
                                }
                                .padding(8.dp)
                        ) {
                            Column {
                                Text(candidate.title, fontWeight = FontWeight.Bold)
                                Text("${candidate.artist} · ${candidate.album}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCandidatePicker = false }) {
                    Text(stringResource(R.string.global_action_cancel))
                }
            }
        )
    }
}

