package com.isai.player.data.scanner

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FolderScannerTest {

    private lateinit var folderScanner: FolderScanner

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        folderScanner = FolderScanner(context)
    }

    @Test
    fun audioMimeTypesAreRecognized() {
        assertTrue(folderScanner.isAudioFile("audio/mpeg", "track.mp3"))
        assertTrue(folderScanner.isAudioFile("audio/flac", "song.flac"))
        assertTrue(folderScanner.isAudioFile("audio/mp4", "audio.m4a"))
        assertTrue(folderScanner.isAudioFile("audio/ogg", "audio.ogg"))
        assertTrue(folderScanner.isAudioFile("audio/x-wav", "sound.wav"))
        assertTrue(folderScanner.isAudioFile("AUDIO/FLAC", "music.flac"))
    }

    @Test
    fun audioExtensionsAreRecognizedRegardlessOfMimeType() {
        assertTrue(folderScanner.isAudioFile(null, "sundari.mp3"))
        assertTrue(folderScanner.isAudioFile("application/octet-stream", "chinna_aasai.flac"))
        assertTrue(folderScanner.isAudioFile("", "song.WAV"))
        assertTrue(folderScanner.isAudioFile(null, "track.M4A"))
        assertTrue(folderScanner.isAudioFile(null, "audio.opus"))
        assertTrue(folderScanner.isAudioFile(null, "melody.aac"))
        assertTrue(folderScanner.isAudioFile(null, "classic.wma"))
        assertTrue(folderScanner.isAudioFile(null, "studio.aiff"))
    }

    @Test
    fun nonAudioFilesAreRejected() {
        assertFalse(folderScanner.isAudioFile("image/jpeg", "cover.jpg"))
        assertFalse(folderScanner.isAudioFile("image/png", "folder.png"))
        assertFalse(folderScanner.isAudioFile("text/plain", "lyrics.txt"))
        assertFalse(folderScanner.isAudioFile("application/pdf", "booklet.pdf"))
        assertFalse(folderScanner.isAudioFile("text/x-lrc", "song.lrc"))
        assertFalse(folderScanner.isAudioFile("audio/x-mpegurl", "playlist.m3u"))
        assertFalse(folderScanner.isAudioFile(null, "notes.doc"))
        assertFalse(folderScanner.isAudioFile(null, null))
    }
}

