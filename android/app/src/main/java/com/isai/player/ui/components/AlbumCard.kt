package com.isai.player.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.theme.CardShape
import com.isai.player.ui.theme.IsaiTheme

@Composable
fun AlbumCard(
    title: String,
    subtitle: String,
    artUri: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(140.dp)
            .clickable(onClick = onClick)
    ) {
        Card(
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = IsaiTheme.colors.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (IsaiTheme.colors.isDark) 0.dp else 3.dp
            ),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(CardShape)
                    .background(IsaiTheme.colors.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (artUri != null) {
                    AsyncImage(
                        model = artUri,
                        contentDescription = title,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Album,
                        contentDescription = null,
                        tint = IsaiTheme.colors.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = IsaiTheme.colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = IsaiTheme.colors.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(name = "AlbumCard - Dark Theme")
@Composable
private fun AlbumCardDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            AlbumCard(
                title = "Thalapathi",
                subtitle = "Ilaiyaraaja",
                artUri = null,
                onClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Preview(name = "AlbumCard - Light Theme")
@Composable
private fun AlbumCardLightPreview() {
    IsaiTheme(themeMode = ThemeMode.LIGHT) {
        Surface(color = IsaiTheme.colors.background) {
            AlbumCard(
                title = "Roja",
                subtitle = "A. R. Rahman",
                artUri = null,
                onClick = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

