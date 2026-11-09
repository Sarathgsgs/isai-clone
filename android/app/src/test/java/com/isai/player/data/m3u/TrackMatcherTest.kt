package com.isai.player.data.m3u

import com.isai.player.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMatcherTest {

    private fun createTestTrack(
        id: Long,
        relativePath: String,
        title: String,
        artist: String,
        durationSec: Int
    ): Track {
        return Track(
            id = id,
            uri = "content://music/$id",
            title = title,
            titleSort = title,
            artist = artist,
            albumArtist = artist,
            album = "Album",
            year = 2024,
            trackNo = 1,
            discNo = 1,
            durationMs = durationSec * 1000L,
            mimeType = "audio/mpeg",
            sizeBytes = 4000000L,
            lastModified = 1000L,
            artPath = null,
            folderPath = "Folder",
            dateAdded = 1000L,
            relativePath = relativePath
        )
    }

    @Test
    fun matchEntries_stage1_exactRelativePathMatch() {
        val track = createTestTrack(1L, "Rock/Pink Floyd/01 Speak to Me.flac", "Speak to Me", "Pink Floyd", 67)
        val entry = M3uEntry(
            rawPath = "Rock/Pink Floyd/01 Speak to Me.flac",
            normalizedPath = "Rock/Pink Floyd/01 Speak to Me.flac"
        )

        val summary = TrackMatcher.matchEntries(listOf(entry), listOf(track))
        assertEquals(1, summary.matchedCount)
        val single = summary.items[0].resolution as MatchResolution.Single
        assertEquals(MatchStage.EXACT_RELATIVE_PATH, single.stage)
        assertEquals(1L, single.track.id)
    }

    @Test
    fun matchEntries_stage2_suffixPathMatch() {
        // Track stored under local music root "FLAC/Rock/Pink Floyd/01 Speak to Me.flac"
        // M3U created with relative "Pink Floyd/01 Speak to Me.flac"
        val track = createTestTrack(2L, "FLAC/Rock/Pink Floyd/01 Speak to Me.flac", "Speak to Me", "Pink Floyd", 67)
        val entry = M3uEntry(
            rawPath = "Pink Floyd/01 Speak to Me.flac",
            normalizedPath = "Pink Floyd/01 Speak to Me.flac"
        )

        val summary = TrackMatcher.matchEntries(listOf(entry), listOf(track))
        assertEquals(1, summary.matchedCount)
        val single = summary.items[0].resolution as MatchResolution.Single
        assertEquals(MatchStage.SUFFIX_PATH, single.stage)
        assertEquals(2L, single.track.id)
    }

    @Test
    fun matchEntries_stage3_filenameAndDurationMatch() {
        // Same filename in different directories; duration matches one of them
        val track1 = createTestTrack(3L, "Albums/Studio/01 Intro.mp3", "Intro", "Artist", 120)
        val track2 = createTestTrack(4L, "Albums/Live/01 Intro.mp3", "Intro Live", "Artist", 180)

        val entry = M3uEntry(
            rawPath = "External/01 Intro.mp3",
            normalizedPath = "External/01 Intro.mp3",
            durationSeconds = 121 // Within ±2s of 120
        )

        val summary = TrackMatcher.matchEntries(listOf(entry), listOf(track1, track2))
        assertEquals(1, summary.matchedCount)
        val single = summary.items[0].resolution as MatchResolution.Single
        assertEquals(MatchStage.FILENAME_AND_DURATION, single.stage)
        assertEquals(3L, single.track.id)
    }

    @Test
    fun matchEntries_stage4_tagMatch() {
        // Path does not match, but tags (Title + Artist) match
        val track = createTestTrack(5L, "Local/DifferentFileName.mp3", "Bohemian Rhapsody", "Queen", 354)
        val entry = M3uEntry(
            rawPath = "USB/01_Track.mp3",
            normalizedPath = "USB/01_Track.mp3",
            title = "Bohemian Rhapsody",
            artist = "Queen",
            durationSeconds = 355
        )

        val summary = TrackMatcher.matchEntries(listOf(entry), listOf(track))
        assertEquals(1, summary.matchedCount)
        val single = summary.items[0].resolution as MatchResolution.Single
        assertEquals(MatchStage.TAG_METADATA, single.stage)
        assertEquals(5L, single.track.id)
    }

    @Test
    fun matchEntries_noMatch_returnsNone() {
        val track = createTestTrack(6L, "Local/Song.mp3", "Song", "Artist", 200)
        val entry = M3uEntry(
            rawPath = "NonExistent/Missing.flac",
            normalizedPath = "NonExistent/Missing.flac"
        )

        val summary = TrackMatcher.matchEntries(listOf(entry), listOf(track))
        assertEquals(0, summary.matchedCount)
        assertEquals(1, summary.notFoundCount)
        assertTrue(summary.items[0].resolution is MatchResolution.None)
    }
}

