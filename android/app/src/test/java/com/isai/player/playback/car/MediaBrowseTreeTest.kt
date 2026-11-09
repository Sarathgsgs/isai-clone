package com.isai.player.playback.car

import android.content.Context
import com.isai.player.data.db.dao.AlbumDao
import com.isai.player.data.db.dao.ArtistDao
import com.isai.player.data.db.dao.FavoriteDao
import com.isai.player.data.db.dao.HistoryDao
import com.isai.player.data.db.dao.PlaylistDao
import com.isai.player.data.db.dao.TrackDao
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MediaBrowseTreeTest {

    private val context = mockk<Context>(relaxed = true)
    private val trackDao = mockk<TrackDao>(relaxed = true)
    private val albumDao = mockk<AlbumDao>(relaxed = true)
    private val artistDao = mockk<ArtistDao>(relaxed = true)
    private val playlistDao = mockk<PlaylistDao>(relaxed = true)
    private val favoriteDao = mockk<FavoriteDao>(relaxed = true)
    private val historyDao = mockk<HistoryDao>(relaxed = true)

    private lateinit var browseTree: MediaBrowseTree

    @Before
    fun setUp() {
        every { context.getString(any()) } returns "TestLabel"
        every { context.packageName } returns "com.isai.player"
        browseTree = MediaBrowseTree(
            context, trackDao, albumDao, artistDao, playlistDao, favoriteDao, historyDao
        )
    }

    @Test
    fun getRootItem_returnsRootWithCorrectMediaId() {
        val root = browseTree.getRootItem()
        assertEquals(MediaBrowseTree.MEDIA_ID_ROOT, root.mediaId)
        assertTrue(root.mediaMetadata.isBrowsable == true)
    }

    @Test
    fun getChildren_root_returnsSixStandardCategories() = runTest {
        val children = browseTree.getChildren(MediaBrowseTree.MEDIA_ID_ROOT)
        assertEquals(6, children.size)

        val ids = children.map { it.mediaId }
        assertTrue(ids.contains(MediaBrowseTree.MEDIA_ID_RECENT))
        assertTrue(ids.contains(MediaBrowseTree.MEDIA_ID_FAVORITES))
        assertTrue(ids.contains(MediaBrowseTree.MEDIA_ID_PLAYLISTS))
        assertTrue(ids.contains(MediaBrowseTree.MEDIA_ID_ALBUMS))
        assertTrue(ids.contains(MediaBrowseTree.MEDIA_ID_ARTISTS))
        assertTrue(ids.contains(MediaBrowseTree.MEDIA_ID_SONGS))
    }

    @Test
    fun getChildren_rootAlbumsAndArtists_haveGridStyleHint() = runTest {
        val children = browseTree.getChildren(MediaBrowseTree.MEDIA_ID_ROOT)

        val albumsNode = children.first { it.mediaId == MediaBrowseTree.MEDIA_ID_ALBUMS }
        val artistsNode = children.first { it.mediaId == MediaBrowseTree.MEDIA_ID_ARTISTS }

        val albumHint = albumsNode.mediaMetadata.extras?.getInt(MediaBrowseTree.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE)
        val artistHint = artistsNode.mediaMetadata.extras?.getInt(MediaBrowseTree.EXTRAS_KEY_CONTENT_STYLE_BROWSABLE)

        assertEquals(MediaBrowseTree.CONTENT_STYLE_GRID, albumHint)
        assertEquals(MediaBrowseTree.CONTENT_STYLE_GRID, artistHint)
    }
}

