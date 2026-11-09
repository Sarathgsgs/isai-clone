package com.isai.player.data.model

import com.isai.player.data.db.entity.TrackEntity
import com.isai.player.data.db.projection.AlbumProjection
import com.isai.player.data.db.projection.ArtistProjection
import com.isai.player.data.db.projection.FolderProjection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class TrackEntityMappingTest {

    @Test
    fun trackEntityMapsBidirectionallyToTrackDomainModel() {
        val entity = TrackEntity(
            id = 123456789L,
            uri = "content://media/external/audio/1",
            title = "Sundari Kannal Oru Sethi",
            titleSort = "sundari kannal oru sethi",
            artist = "Ilaiyaraaja",
            albumArtist = "Ilaiyaraaja",
            album = "Thalapathi",
            year = 1991,
            trackNo = 3,
            discNo = 1,
            durationMs = 312000L,
            mimeType = "audio/mpeg",
            sizeBytes = 7800000L,
            lastModified = 1600000000L,
            artPath = "/cache/art/thalapathi.jpg",
            folderPath = "Music/Soundtracks",
            dateAdded = 1700000000L,
            isMissing = false,
            relativePath = "Soundtracks/Sundari.mp3",
            relativePathNorm = "soundtracks/sundari.mp3",
            lyricsUri = null,
            hasLyrics = false
        )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.uri, domain.uri)
        assertEquals(entity.title, domain.title)
        assertEquals(entity.artist, domain.artist)
        assertEquals(entity.album, domain.album)
        assertEquals(entity.year, domain.year)
        assertEquals(entity.trackNo, domain.trackNo)
        assertEquals(entity.durationMs, domain.durationMs)
        assertEquals(entity.artPath, domain.artPath)
        assertEquals(entity.folderPath, domain.folderPath)
        assertFalse(domain.isMissing)

        val mappedBackEntity = domain.toEntity()
        assertEquals(entity.id, mappedBackEntity.id)
        assertEquals(entity.uri, mappedBackEntity.uri)
        assertEquals(entity.title, mappedBackEntity.title)
        assertEquals(entity.relativePathNorm, mappedBackEntity.relativePathNorm)
    }

    @Test
    fun fallbackDisplayPropertiesWhenStringsAreBlank() {
        val blankTrack = Track(
            id = 1L,
            uri = "content://test",
            title = "",
            titleSort = "",
            artist = "  ",
            albumArtist = "",
            album = "",
            year = 0,
            trackNo = 0,
            discNo = 0,
            durationMs = 0L,
            mimeType = "audio/mpeg",
            sizeBytes = 0L,
            lastModified = 0L,
            artPath = null,
            folderPath = "",
            dateAdded = 0L
        )

        assertEquals("Unknown Title", blankTrack.displayTitle)
        assertEquals("Unknown Artist", blankTrack.displayArtist)
        assertEquals("Unknown Album", blankTrack.displayAlbum)
    }

    @Test
    fun folderProjectionExtractsTrailingFolderName() {
        val folder1 = FolderProjection(folderPath = "Music/Rock/Classic", trackCount = 12).toDomain()
        assertEquals("Classic", folder1.name)
        assertEquals("Music/Rock/Classic", folder1.path)
        assertEquals(12, folder1.trackCount)

        val rootFolder = FolderProjection(folderPath = "Music/", trackCount = 5).toDomain()
        assertEquals("Music", rootFolder.name)

        val emptyFolder = FolderProjection(folderPath = "", trackCount = 0).toDomain()
        assertEquals("Music", emptyFolder.name)
    }

    @Test
    fun albumAndArtistProjectionMapping() {
        val albumProj = AlbumProjection(
            album = "Roja",
            albumArtist = "A. R. Rahman",
            trackCount = 6,
            year = 1992,
            artPath = "/art/roja.jpg"
        ).toDomain()

        assertEquals("Roja", albumProj.title)
        assertEquals("A. R. Rahman", albumProj.artist)
        assertEquals(6, albumProj.trackCount)
        assertEquals(1992, albumProj.year)

        val artistProj = ArtistProjection(
            artist = "A. R. Rahman",
            trackCount = 42,
            albumCount = 8,
            sampleArtPath = "/art/roja.jpg"
        ).toDomain()

        assertEquals("A. R. Rahman", artistProj.name)
        assertEquals(42, artistProj.trackCount)
        assertEquals(8, artistProj.albumCount)
    }
}

