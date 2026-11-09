package com.isai.player.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.data.model.Track
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackPickerBottomSheet(
    sheetState: SheetState,
    allTracks: List<Track>,
    alreadyInPlaylistTrackIds: Set<Long> = emptySet(),
    onConfirm: (List<Long>) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTrackIds by remember { mutableStateOf(setOf<Long>()) }

    val filteredTracks = remember(searchQuery, allTracks, alreadyInPlaylistTrackIds) {
        val query = searchQuery.trim().lowercase()
        allTracks.filter { track ->
            !alreadyInPlaylistTrackIds.contains(track.id) &&
                (query.isEmpty() ||
                    track.title.lowercase().contains(query) ||
                    track.artist.lowercase().contains(query) ||
                    track.album.lowercase().contains(query))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = IsaiTheme.colors.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.track_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = IsaiTheme.colors.onBackground
                )

                Text(
                    text = stringResource(id = R.string.track_picker_selected_count, selectedTrackIds.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = IsaiTheme.colors.accent
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.track_picker_search_hint),
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
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = stringResource(id = R.string.search_clear_description),
                                tint = IsaiTheme.colors.onSurfaceVariant
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = IsaiTheme.colors.background,
                    unfocusedContainerColor = IsaiTheme.colors.background,
                    focusedBorderColor = IsaiTheme.colors.accent,
                    unfocusedBorderColor = IsaiTheme.colors.surfaceVariant,
                    focusedTextColor = IsaiTheme.colors.onBackground,
                    unfocusedTextColor = IsaiTheme.colors.onBackground,
                    cursorColor = IsaiTheme.colors.accent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Track list with checkboxes
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(filteredTracks, key = { it.id }) { track ->
                    val isChecked = selectedTrackIds.contains(track.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedTrackIds = if (isChecked) {
                                    selectedTrackIds - track.id
                                } else {
                                    selectedTrackIds + track.id
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                selectedTrackIds = if (checked) {
                                    selectedTrackIds + track.id
                                } else {
                                    selectedTrackIds - track.id
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = IsaiTheme.colors.accent,
                                uncheckedColor = IsaiTheme.colors.onSurfaceVariant,
                                checkmarkColor = IsaiTheme.colors.onAccent
                            )
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Confirm Add Button
            Button(
                onClick = {
                    onConfirm(selectedTrackIds.toList())
                    onDismissRequest()
                },
                enabled = selectedTrackIds.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IsaiTheme.colors.accent,
                    contentColor = IsaiTheme.colors.onAccent,
                    disabledContainerColor = IsaiTheme.colors.surfaceVariant,
                    disabledContentColor = IsaiTheme.colors.onSurfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.track_picker_action_add, selectedTrackIds.size),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

