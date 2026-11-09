package com.isai.player.playback.car

import com.isai.player.data.db.dao.AlbumDao
import com.isai.player.data.db.dao.ArtistDao
import com.isai.player.data.db.dao.TrackDao
import com.isai.player.data.db.entity.toDomain
import com.isai.player.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceSearchResolver @Inject constructor(
    private val trackDao: TrackDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao
) {

    /**
     * Resolves a voice search query or Android Auto search input into a playable list of tracks.
     *
     * Hierarchy:
     * 1. Blank query -> Shuffle all library tracks.
     * 2. Artist match -> All tracks by that artist.
     * 3. Album match -> All tracks on that album in track order.
     * 4. Title / General query match -> Matching tracks from search.
     */
    suspend fun resolveSearch(query: String?): List<Track> = withContext(Dispatchers.IO) {
        val trimmed = query?.trim().orEmpty()

        if (trimmed.isEmpty()) {
            return@withContext trackDao.getRandomTracks(100).map { it.toDomain() }
        }

        // 1. Check for exact/close artist match
        val artistTracks = artistDao.getTracksForArtistSync(trimmed)
        if (artistTracks.isNotEmpty()) {
            return@withContext artistTracks.map { it.toDomain() }
        }

        // 2. Check for exact/close album match
        val albumTracks = albumDao.getTracksForAlbumSync(trimmed)
        if (albumTracks.isNotEmpty()) {
            return@withContext albumTracks.map { it.toDomain() }
        }

        // 3. General search by title/artist/album
        val searchResults = trackDao.searchTracks(trimmed).first()
        if (searchResults.isNotEmpty()) {
            return@withContext searchResults.map { it.toDomain() }
        }

        // Fallback: If no matches, return random tracks so playback doesn't stall
        trackDao.getRandomTracks(50).map { it.toDomain() }
    }
}

