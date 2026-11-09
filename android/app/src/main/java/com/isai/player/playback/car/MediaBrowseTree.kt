package com.isai.player.playback.car

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.isai.player.R
import com.isai.player.data.db.dao.AlbumDao
import com.isai.player.data.db.dao.ArtistDao
import com.isai.player.data.db.dao.FavoriteDao
import com.isai.player.data.db.dao.HistoryDao
import com.isai.player.data.db.dao.PlaylistDao
import com.isai.player.data.db.dao.TrackDao
import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.model.toDomain
import com.isai.player.data.model.Track
import com.isai.player.playback.MediaItemMapper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaBrowseTree @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trackDao: TrackDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val playlistDao: PlaylistDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao
) {

    companion object {
        const val MEDIA_ID_ROOT = "/"
        const val MEDIA_ID_RECENT = "root/recent"
        const val MEDIA_ID_FAVORITES = "root/favorites"
        const val MEDIA_ID_PLAYLISTS = "root/playlists"
        const val MEDIA_ID_ALBUMS = "root/albums"
        const val MEDIA_ID_ARTISTS = "root/artists"
        const val MEDIA_ID_SONGS = "root/songs"
        const val MEDIA_ID_SHUFFLE_ALL = "action/shuffle_all"

        const val EXTRAS_KEY_CONTENT_STYLE_BROWSABLE = "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
        const val EXTRAS_KEY_CONTENT_STYLE_PLAYABLE = "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
        const val CONTENT_STYLE_LIST = 1
        const val CONTENT_STYLE_GRID = 2
    }

    fun getRootItem(): MediaItem {
        return MediaItem.Builder()
            .setMediaId(MEDIA_ID_ROOT)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(context.getString(R.string.app_name))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build()
            )
            .build()
    }

    suspend fun getChildren(
        parentId: String,
        page: Int = 0,
        pageSize: Int = 50
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val offset = (page * pageSize).coerceAtLeast(0)

        when (parentId) {
            MEDIA_ID_ROOT -> getRootChildren()
            MEDIA_ID_RECENT -> historyDao.getRecentlyPlayedSync(20).map(::trackToCarMediaItem)
            MEDIA_ID_FAVORITES -> favoriteDao.getFavoriteTracksSync(200).map(::trackToCarMediaItem)
            MEDIA_ID_PLAYLISTS -> getPlaylistNodes()
            MEDIA_ID_ALBUMS -> getAlbumNodes(pageSize, offset)
            MEDIA_ID_ARTISTS -> getArtistNodes(pageSize, offset)
            MEDIA_ID_SONGS -> getSongNodes(pageSize, offset)
            else -> resolveDynamicNodeChildren(parentId)
        }
    }

    private fun getRootChildren(): List<MediaItem> {
        val listExtras = Bundle().apply {
            putInt(EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, CONTENT_STYLE_LIST)
            putInt(EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, CONTENT_STYLE_LIST)
        }
        val gridExtras = Bundle().apply {
            putInt(EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, CONTENT_STYLE_GRID)
            putInt(EXTRAS_KEY_CONTENT_STYLE_PLAYABLE, CONTENT_STYLE_GRID)
        }

        return listOf(
            buildBrowsableNode(MEDIA_ID_RECENT, context.getString(R.string.car_node_recent), listExtras),
            buildBrowsableNode(MEDIA_ID_FAVORITES, context.getString(R.string.car_node_favorites), listExtras),
            buildBrowsableNode(MEDIA_ID_PLAYLISTS, context.getString(R.string.car_node_playlists), listExtras),
            buildBrowsableNode(MEDIA_ID_ALBUMS, context.getString(R.string.car_node_albums), gridExtras),
            buildBrowsableNode(MEDIA_ID_ARTISTS, context.getString(R.string.car_node_artists), gridExtras),
            buildBrowsableNode(MEDIA_ID_SONGS, context.getString(R.string.car_node_songs), listExtras)
        )
    }

    private suspend fun getPlaylistNodes(): List<MediaItem> {
        val playlists = playlistDao.getAllPlaylistsSync()
        return playlists.map { p ->
            MediaItem.Builder()
                .setMediaId("$MEDIA_ID_PLAYLISTS/${p.playlist.id}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(p.playlist.name)
                        .setSubtitle(context.getString(R.string.car_track_count, p.trackCount))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }
    }

    private suspend fun getAlbumNodes(limit: Int, offset: Int): List<MediaItem> {
        val albums = albumDao.getAlbumsPaged(limit, offset)
        return albums.map { a ->
            val artUri = if (!a.artPath.isNullOrBlank()) {
                Uri.parse("file://${a.artPath}")
            } else null

            MediaItem.Builder()
                .setMediaId("$MEDIA_ID_ALBUMS/${a.album}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(a.album)
                        .setSubtitle(a.albumArtist)
                        .setArtworkUri(artUri)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }
    }

    private suspend fun getArtistNodes(limit: Int, offset: Int): List<MediaItem> {
        val artists = artistDao.getArtistsPaged(limit, offset)
        return artists.map { ar ->
            MediaItem.Builder()
                .setMediaId("$MEDIA_ID_ARTISTS/${ar.artist}")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(ar.artist)
                        .setSubtitle(context.getString(R.string.car_track_count, ar.trackCount))
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build()
                )
                .build()
        }
    }

    private suspend fun getSongNodes(limit: Int, offset: Int): List<MediaItem> {
        val tracks = trackDao.getTracksPaged(limit, offset)
        val items = mutableListOf<MediaItem>()

        if (offset == 0) {
            // First item is Shuffle All action
            items.add(
                MediaItem.Builder()
                    .setMediaId(MEDIA_ID_SHUFFLE_ALL)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(context.getString(R.string.car_action_shuffle_all))
                            .setIsBrowsable(false)
                            .setIsPlayable(true)
                            .build()
                    )
                    .build()
            )
        }

        items.addAll(tracks.map(::trackToCarMediaItem))
        return items
    }

    private suspend fun resolveDynamicNodeChildren(parentId: String): List<MediaItem> {
        return when {
            parentId.startsWith("$MEDIA_ID_PLAYLISTS/") -> {
                val playlistId = parentId.removePrefix("$MEDIA_ID_PLAYLISTS/").toLongOrNull() ?: return emptyList()
                playlistDao.getTracksForPlaylistSync(playlistId).map(::trackToCarMediaItem)
            }
            parentId.startsWith("$MEDIA_ID_ALBUMS/") -> {
                val albumName = parentId.removePrefix("$MEDIA_ID_ALBUMS/")
                albumDao.getTracksForAlbumSync(albumName).map(::trackToCarMediaItem)
            }
            parentId.startsWith("$MEDIA_ID_ARTISTS/") -> {
                val artistName = parentId.removePrefix("$MEDIA_ID_ARTISTS/")
                artistDao.getTracksForArtistSync(artistName).map(::trackToCarMediaItem)
            }
            else -> emptyList()
        }
    }

    suspend fun resolveTracksForPlayback(mediaId: String): List<Track> = withContext(Dispatchers.IO) {
        when {
            mediaId == MEDIA_ID_SHUFFLE_ALL -> trackDao.getRandomTracks(100).map { it.toDomain() }
            mediaId == MEDIA_ID_RECENT -> historyDao.getRecentlyPlayedSync(20).map { it.toDomain() }
            mediaId == MEDIA_ID_FAVORITES -> favoriteDao.getFavoriteTracksSync(200).map { it.toDomain() }
            mediaId.startsWith("$MEDIA_ID_PLAYLISTS/") -> {
                val id = mediaId.removePrefix("$MEDIA_ID_PLAYLISTS/").toLongOrNull() ?: return@withContext emptyList()
                playlistDao.getTracksForPlaylistSync(id).map { it.toDomain() }
            }
            mediaId.startsWith("$MEDIA_ID_ALBUMS/") -> {
                val album = mediaId.removePrefix("$MEDIA_ID_ALBUMS/")
                albumDao.getTracksForAlbumSync(album).map { it.toDomain() }
            }
            mediaId.startsWith("$MEDIA_ID_ARTISTS/") -> {
                val artist = mediaId.removePrefix("$MEDIA_ID_ARTISTS/")
                artistDao.getTracksForArtistSync(artist).map { it.toDomain() }
            }
            else -> {
                val trackId = mediaId.toLongOrNull()
                if (trackId != null) {
                    val entity = trackDao.getTrackById(trackId)
                    listOfNotNull(entity?.toDomain())
                } else {
                    emptyList()
                }
            }
        }
    }

    private fun buildBrowsableNode(mediaId: String, title: String, extras: Bundle): MediaItem {
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    private fun trackToCarMediaItem(entity: TrackEntity): MediaItem {
        val track = entity.toDomain()
        val baseItem = MediaItemMapper.toMediaItem(track)

        // For car head units, point artwork to CarArtworkProvider content URI
        val artContentUri = if (!track.artPath.isNullOrBlank()) {
            CarArtworkProvider.buildArtworkUri(context.packageName, track.id)
        } else null

        val carMetadata = baseItem.mediaMetadata.buildUpon()
            .setArtworkUri(artContentUri ?: baseItem.mediaMetadata.artworkUri)
            .build()

        return baseItem.buildUpon()
            .setMediaMetadata(carMetadata)
            .build()
    }
}

