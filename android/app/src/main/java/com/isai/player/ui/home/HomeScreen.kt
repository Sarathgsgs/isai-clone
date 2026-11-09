package com.isai.player.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.player.R
import com.isai.player.data.model.Track
import com.isai.player.data.repository.ThemeMode
import com.isai.player.ui.components.AlbumCard
import com.isai.player.ui.components.SectionHeader
import com.isai.player.ui.theme.CardShape
import com.isai.player.ui.theme.IsaiTheme

@Composable
fun HomeScreen(
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val totalSongs by viewModel.totalSongsCount.collectAsStateWithLifecycle()
    val totalAlbums by viewModel.albumsCount.collectAsStateWithLifecycle()
    val totalArtists by viewModel.artistsCount.collectAsStateWithLifecycle()

    HomeContent(
        recentlyPlayed = recentlyPlayed,
        totalSongs = totalSongs,
        totalAlbums = totalAlbums,
        totalArtists = totalArtists,
        onTrackClick = { track ->
            viewModel.playTrack(track, recentlyPlayed)
        },
        onShuffleAllClick = {
            viewModel.shuffleAll()
        },
        onAlbumClick = onAlbumClick,
        modifier = modifier
    )
}

@Composable
fun HomeContent(
    recentlyPlayed: List<Track>,
    totalSongs: Int,
    totalAlbums: Int,
    totalArtists: Int,
    onTrackClick: (Track) -> Unit,
    onShuffleAllClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Hero "Shuffle All" Card
        Card(
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable(onClick = onShuffleAllClick)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.home_hero_shuffle_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = IsaiTheme.colors.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.home_hero_shuffle_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = IsaiTheme.colors.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = IsaiTheme.colors.accent,
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = stringResource(id = R.string.home_hero_shuffle_title),
                        tint = IsaiTheme.colors.onAccent,
                        modifier = Modifier
                            .padding(14.dp)
                            .size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Played Recently Section
        SectionHeader(
            title = stringResource(id = R.string.home_section_recently_played)
        )

        if (recentlyPlayed.isEmpty()) {
            Card(
                shape = CardShape,
                colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.home_recently_played_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = IsaiTheme.colors.onSurfaceVariant,
                    modifier = Modifier.padding(20.dp)
                )
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = recentlyPlayed,
                    key = { it.id }
                ) { track ->
                    AlbumCard(
                        title = track.title,
                        subtitle = track.artist,
                        artUri = track.artPath,
                        onClick = { onTrackClick(track) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Library Overview Statistics
        SectionHeader(title = stringResource(id = R.string.home_section_library))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                icon = Icons.Default.MusicNote,
                count = totalSongs,
                label = stringResource(id = R.string.library_tab_songs),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.Album,
                count = totalAlbums,
                label = stringResource(id = R.string.library_tab_albums),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                icon = Icons.Default.Person,
                count = totalArtists,
                label = stringResource(id = R.string.library_tab_artists),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = IsaiTheme.colors.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = IsaiTheme.colors.accent,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IsaiTheme.colors.onBackground
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = IsaiTheme.colors.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "HomeScreen - Dark Theme")
@Composable
private fun HomeScreenDarkPreview() {
    IsaiTheme(themeMode = ThemeMode.DARK) {
        Surface(color = IsaiTheme.colors.background) {
            HomeContent(
                recentlyPlayed = emptyList(),
                totalSongs = 120,
                totalAlbums = 14,
                totalArtists = 8,
                onTrackClick = {},
                onShuffleAllClick = {},
                onAlbumClick = {}
            )
        }
    }
}
