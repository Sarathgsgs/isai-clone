package com.isai.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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

@Composable
fun MiniPlayer(
    title: String,
    artist: String,
    artUri: Any?,
    isPlaying: Boolean,
    progress: Float,
    onClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = IsaiTheme.colors.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (IsaiTheme.colors.isDark) 4.dp else 6.dp
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular art stand-in for vinyl record
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(IsaiTheme.colors.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (artUri != null) {
                        AsyncImage(
                            model = artUri,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = IsaiTheme.colors.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Track Title and Artist
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = IsaiTheme.colors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = IsaiTheme.colors.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play / Pause Action
                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = stringResource(
                            id = if (isPlaying) R.string.miniplayer_content_description_pause
                            else R.string.miniplayer_content_description_play
                        ),
                        tint = IsaiTheme.colors.accent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Skip Next Action
                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = stringResource(id = R.string.miniplayer_content_description_next),
                        tint = IsaiTheme.colors.onBackground,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Hairline Progress Line at bottom
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = IsaiTheme.colors.accent,
                trackColor = IsaiTheme.colors.surfaceVariant
            )
        }
    }
}

@Preview(name = "MiniPlayer - Dark Theme")
@Composable
private fun MiniPlayerDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            MiniPlayer(
                title = "Kannodu Kaanbadhellam",
                artist = "A. R. Rahman · Jeans",
                artUri = null,
                isPlaying = true,
                progress = 0.42f,
                onClick = {},
                onPlayPauseClick = {},
                onNextClick = {}
            )
        }
    }
}

@Preview(name = "MiniPlayer - Light Theme")
@Composable
private fun MiniPlayerLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        Surface(color = IsaiTheme.colors.background) {
            MiniPlayer(
                title = "Ennavale Adi Ennavale",
                artist = "P. Unnikrishnan · Kadhalan",
                artUri = null,
                isPlaying = false,
                progress = 0.15f,
                onClick = {},
                onPlayPauseClick = {},
                onNextClick = {}
            )
        }
    }
}

