package com.isai.player.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isai.player.R
import com.isai.player.data.model.Track
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.util.Formatters

@Composable
fun TrackInfoDialog(
    track: Track,
    onDismissRequest: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = IsaiTheme.colors.surface,
        title = {
            Text(
                text = stringResource(id = R.string.track_info_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = IsaiTheme.colors.onBackground
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                InfoRow(label = stringResource(id = R.string.track_info_label_title), value = track.title)
                InfoRow(label = stringResource(id = R.string.track_info_label_artist), value = track.artist)
                InfoRow(label = stringResource(id = R.string.track_info_label_album), value = track.album)
                InfoRow(label = stringResource(id = R.string.track_info_label_duration), value = Formatters.formatDurationMs(track.durationMs))
                InfoRow(label = stringResource(id = R.string.track_info_label_format), value = track.mimeType)
                InfoRow(label = stringResource(id = R.string.track_info_label_size), value = Formatters.formatFileSize(track.sizeBytes))
                InfoRow(label = stringResource(id = R.string.track_info_label_folder), value = track.folderPath)
                if (track.year > 0) {
                    InfoRow(label = stringResource(id = R.string.track_info_label_year), value = track.year.toString())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(
                    text = stringResource(id = R.string.track_info_action_close),
                    color = IsaiTheme.colors.accent
                )
            }
        }
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = IsaiTheme.colors.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = IsaiTheme.colors.onBackground
        )
    }
}
