package com.isai.player.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.isai.player.data.db.dao.AlbumDao
import com.isai.player.data.db.dao.ArtistDao
import com.isai.player.data.db.dao.FavoriteDao
import com.isai.player.data.db.dao.FolderDao
import com.isai.player.data.db.dao.HistoryDao
import com.isai.player.data.db.dao.PlaylistDao
import com.isai.player.data.db.dao.QueueDao
import com.isai.player.data.db.dao.TrackDao
import com.isai.player.data.db.entity.PlaylistEntity
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.db.util.Fnv1a
import com.isai.player.data.model.AlbumSortOrder
import com.isai.player.data.model.ArtistSortOrder
import com.isai.player.data.model.TrackSortOrder
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IsaiDatabaseTest {

    private lateinit var database: IsaiDatabase
    private lateinit var trackDao: TrackDao
    private lateinit var albumDao: AlbumDao
    private lateinit var artistDao: ArtistDao
    private lateinit var folderDao: FolderDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var playlistDao: PlaylistDao
    private lateinit var historyDao: HistoryDao
    private lateinit var queueDao: QueueDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, IsaiDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        trackDao = database.trackDao()
        albumDao = database.albumDao()
        artistDao = database.artistDao()
        folderDao = database.folderDao()
        favoriteDao = database.favoriteDao()
        playlistDao = database.playlistDao()
        historyDao = database.historyDao()
        queueDao = database.queueDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun createSampleTrack(
        uri: String,
        title: String,
        artist: String,
        album: String,
        year: Int = 1991,
        trackNo: Int = 1,
        folderPath: String = "Music/Soundtracks"
    ): TrackEntity {
        return TrackEntity(
            id = Fnv1a.hash64(uri),
            uri = uri,
            title = title,
            titleSort = title.lowercase(),
            artist = artist,
            albumArtist = artist,
            album = album,
            year = year,
            trackNo = trackNo,
            discNo = 1,
            durationMs = 240000L,
            mimeType = "audio/mpeg",
            sizeBytes = 5000000L,
            lastModified = System.currentTimeMillis(),
            artPath = "/art/$title.jpg",
            folderPath = folderPath,
            dateAdded = System.currentTimeMillis(),
            isMissing = false
        )
    }

    @Test
    fun insertAndQueryTracks() = runTest {
        val track1 = createSampleTrack("uri:1", "Sundari", "Ilaiyaraaja", "Thalapathi")
        val track2 = createSampleTrack("uri:2", "Chinna Chinna Aasai", "A. R. Rahman", "Roja", year = 1992)

        trackDao.insertTracks(listOf(track1, track2))

        val retrieved1 = trackDao.getTrackById(track1.id)
        assertNotNull(retrieved1)
        assertEquals("Sundari", retrieved1?.title)

        val allTracks = trackDao.observeAllTracks(TrackSortOrder.TITLE_ASC).first()
        assertEquals(2, allTracks.size)
        assertEquals("Chinna Chinna Aasai", allTracks[0].title)
        assertEquals("Sundari", allTracks[1].title)
    }

    @Test
    fun missingTrackFlaggingAndPurging() = runTest {
        val track1 = createSampleTrack("uri:1", "Sundari", "Ilaiyaraaja", "Thalapathi")
        val track2 = createSampleTrack("uri:2", "Chinna Chinna Aasai", "A. R. Rahman", "Roja")
        trackDao.insertTracks(listOf(track1, track2))

        // Mark track 1 as missing
        trackDao.markTracksMissing(listOf("uri:1"))

        val activeTracks = trackDao.observeAllTracks(TrackSortOrder.TITLE_ASC).first()
        assertEquals(1, activeTracks.size)
        assertEquals("Chinna Chinna Aasai", activeTracks[0].title)

        // Purge missing tracks from DB
        val purgedCount = trackDao.purgeMissingTracks()
        assertEquals(1, purgedCount)

        val retrieved1 = trackDao.getTrackById(track1.id)
        assertNull(retrieved1)
    }

    @Test
    fun searchTracksByTitleArtistOrAlbum() = runTest {
        val track1 = createSampleTrack("uri:1", "Sundari Kannal", "Ilaiyaraaja", "Thalapathi")
        val track2 = createSampleTrack("uri:2", "Chinna Chinna Aasai", "A. R. Rahman", "Roja")
        val track3 = createSampleTrack("uri:3", "Rakkamma Kaiya Thattu", "Ilaiyaraaja", "Thalapathi")
        trackDao.insertTracks(listOf(track1, track2, track3))

        val searchByArtist = trackDao.searchTracks("Ilaiyaraaja").first()
        assertEquals(2, searchByArtist.size)

        val searchByTitle = trackDao.searchTracks("Aasai").first()
        assertEquals(1, searchByTitle.size)
        assertEquals("Chinna Chinna Aasai", searchByTitle[0].title)

        val searchByAlbum = trackDao.searchTracks("Thalapathi").first()
        assertEquals(2, searchByAlbum.size)
    }

    @Test
    fun albumAndArtistGroupingAggregations() = runTest {
        val track1 = createSampleTrack("uri:1", "Sundari", "Ilaiyaraaja", "Thalapathi", trackNo = 1)
        val track2 = createSampleTrack("uri:2", "Rakkamma", "Ilaiyaraaja", "Thalapathi", trackNo = 2)
        val track3 = createSampleTrack("uri:3", "Roja Jaaneman", "A. R. Rahman", "Roja", trackNo = 1)
        trackDao.insertTracks(listOf(track1, track2, track3))

        val albums = albumDao.observeAlbums(AlbumSortOrder.TITLE_ASC).first()
        assertEquals(2, albums.size)
        assertEquals("Roja", albums[0].album)
        assertEquals(1, albums[0].trackCount)
        assertEquals("Thalapathi", albums[1].album)
        assertEquals(2, albums[1].trackCount)

        val artists = artistDao.observeArtists(ArtistSortOrder.NAME_ASC).first()
        assertEquals(2, artists.size)
        assertEquals("A. R. Rahman", artists[0].artist)
        assertEquals("Ilaiyaraaja", artists[1].artist)
        assertEquals(2, artists[1].trackCount)
    }

    @Test
    fun favoriteToggleAndCascadeDelete() = runTest {
        val track = createSampleTrack("uri:1", "Sundari", "Ilaiyaraaja", "Thalapathi")
        trackDao.insertTrack(track)

        assertFalse(favoriteDao.isFavorite(track.id))

        // Toggle on
        favoriteDao.toggleFavorite(track.id)
        assertTrue(favoriteDao.isFavorite(track.id))
        val favTracks = favoriteDao.observeFavoriteTracks().first()
        assertEquals(1, favTracks.size)

        // Toggle off
        favoriteDao.toggleFavorite(track.id)
        assertFalse(favoriteDao.isFavorite(track.id))

        // Toggle back on, then delete track to verify foreign key cascade
        favoriteDao.toggleFavorite(track.id)
        assertTrue(favoriteDao.isFavorite(track.id))

        trackDao.deleteTracks(listOf(track))
        assertFalse(favoriteDao.isFavorite(track.id))
    }

    @Test
    fun playlistCreationTrackOrderingAndCascade() = runTest {
        val track1 = createSampleTrack("uri:1", "Sundari", "Ilaiyaraaja", "Thalapathi")
        val track2 = createSampleTrack("uri:2", "Chinna Chinna Aasai", "A. R. Rahman", "Roja")
        val track3 = createSampleTrack("uri:3", "Rakkamma", "Ilaiyaraaja", "Thalapathi")
        trackDao.insertTracks(listOf(track1, track2, track3))

        val playlistId = playlistDao.insertPlaylist(
            PlaylistEntity(name = "Tamil Classics", createdAt = 1000L, updatedAt = 1000L)
        )

        playlistDao.addTracksToPlaylist(playlistId, listOf(track1.id, track2.id, track3.id))

        val playlistTracks = playlistDao.observeTracksForPlaylist(playlistId).first()
        assertEquals(3, playlistTracks.size)
        assertEquals(track1.id, playlistTracks[0].id)
        assertEquals(track2.id, playlistTracks[1].id)
        assertEquals(track3.id, playlistTracks[2].id)

        // Reorder: 3, 1, 2
        playlistDao.reorderPlaylist(playlistId, listOf(track3.id, track1.id, track2.id))
        val reordered = playlistDao.observeTracksForPlaylist(playlistId).first()
        assertEquals(track3.id, reordered[0].id)
        assertEquals(track1.id, reordered[1].id)
        assertEquals(track2.id, reordered[2].id)

        // Remove track 1
        playlistDao.removeTrackFromPlaylist(playlistId, track1.id)
        val afterRemoval = playlistDao.observeTracksForPlaylist(playlistId).first()
        assertEquals(2, afterRemoval.size)

        // Delete playlist -> tracks still exist
        playlistDao.deletePlaylist(playlistId)
        val remainingTracks = trackDao.observeAllTracks(TrackSortOrder.TITLE_ASC).first()
        assertEquals(3, remainingTracks.size)
    }

    @Test
    fun playHistoryRecordAndLimit() = runTest {
        val track1 = createSampleTrack("uri:1", "Sundari", "Ilaiyaraaja", "Thalapathi")
        val track2 = createSampleTrack("uri:2", "Chinna Chinna Aasai", "A. R. Rahman", "Roja")
        trackDao.insertTracks(listOf(track1, track2))

        historyDao.recordPlay(track1.id, playedAt = 1000L)
        historyDao.recordPlay(track2.id, playedAt = 2000L)
        historyDao.recordPlay(track1.id, playedAt = 3000L) // track1 played again most recently

        val recent = historyDao.observeRecentlyPlayed(limit = 10).first()
        assertEquals(2, recent.size)
        assertEquals(track1.id, recent[0].id) // Most recent play
        assertEquals(track2.id, recent[1].id)

        historyDao.clearHistory()
        val emptyRecent = historyDao.observeRecentlyPlayed(limit = 10).first()
        assertTrue(emptyRecent.isEmpty())
    }

    @Test
    fun queuePersistenceAndRestoration() = runTest {
        val track1 = createSampleTrack("uri:1", "Sundari", "Ilaiyaraaja", "Thalapathi")
        val track2 = createSampleTrack("uri:2", "Chinna Chinna Aasai", "A. R. Rahman", "Roja")
        trackDao.insertTracks(listOf(track1, track2))

        queueDao.saveQueue(listOf(track2.id, track1.id))

        val queueIds = queueDao.getQueueTrackIds()
        assertEquals(listOf(track2.id, track1.id), queueIds)

        val queueTracks = queueDao.getQueueTracks()
        assertEquals(2, queueTracks.size)
        assertEquals(track2.id, queueTracks[0].id)
        assertEquals(track1.id, queueTracks[1].id)

        queueDao.clearQueue()
        assertTrue(queueDao.getQueueTracks().isEmpty())
    }
}

