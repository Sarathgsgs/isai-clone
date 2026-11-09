package com.isai.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.isai.player.R
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.IsaiTheme
import com.isai.player.util.Formatters

@Composable
fun TrackRow(
    title: String,
    artist: String,
    durationMs: Long,
    artUri: Any?,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Art with Vinyl/Music fallback
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(IsaiTheme.colors.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (artUri != null) {
                AsyncImage(
                    model = artUri,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = IsaiTheme.colors.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(IsaiTheme.colors.scrim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = IsaiTheme.colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Artist
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isPlaying) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isPlaying) IsaiTheme.colors.accent else IsaiTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = artist,
                style = MaterialTheme.typography.labelMedium,
                color = IsaiTheme.colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Duration
        Text(
            text = Formatters.formatDurationMs(durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = IsaiTheme.colors.onSurfaceVariant
        )

        // Overflow More Button
        IconButton(onClick = onMoreClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = stringResource(id = R.string.global_action_more_options),
                tint = IsaiTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "TrackRow - Dark Theme")
@Composable
private fun TrackRowDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            TrackRow(
                title = "Aasai Oru Mani",
                artist = "Ilaiyaraaja",
                durationMs = 274000L,
                artUri = null,
                isPlaying = true,
                onClick = {},
                onMoreClick = {}
            )
        }
    }
}

@Preview(name = "TrackRow - Light Theme")
@Composable
private fun TrackRowLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        Surface(color = IsaiTheme.colors.background) {
            TrackRow(
                title = "Nilaave Vaa",
                artist = "S. P. Balasubrahmanyam",
                durationMs = 289000L,
                artUri = null,
                isPlaying = false,
                onClick = {},
                onMoreClick = {}
            )
        }
    }
}

