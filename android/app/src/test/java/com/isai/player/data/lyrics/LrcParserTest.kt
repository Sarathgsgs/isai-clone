package com.isai.player.data.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parsesStandardLrcWithHeadersAndTimestamps() {
        val lrc = """
            [ti:Yesterday]
            [ar:The Beatles]
            [al:Help!]
            [offset:250]
            [00:03.50]Yesterday
            [00:06.20]All my troubles seemed so far away
            [00:12.80]Now it looks as though they're here to stay
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertEquals(LyricsFormat.SYNCED, parsed.format)
        assertEquals("Yesterday", parsed.title)
        assertEquals("The Beatles", parsed.artist)
        assertEquals("Help!", parsed.album)
        assertEquals(250, parsed.offsetMs)
        assertEquals(3, parsed.lines.size)

        // Line 1: 00:03.50 -> 3500ms
        assertEquals(3500L, parsed.lines[0].timestampMs)
        assertEquals("Yesterday", parsed.lines[0].text)

        // Line 2: 00:06.20 -> 6200ms
        assertEquals(6200L, parsed.lines[1].timestampMs)
        assertEquals("All my troubles seemed so far away", parsed.lines[1].text)

        // Line 3: 00:12.80 -> 12800ms
        assertEquals(12800L, parsed.lines[2].timestampMs)
    }

    @Test
    fun parsesThreeDigitMilliseconds() {
        val lrc = """
            [01:15.678]High precision timing line
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertEquals(1, parsed.lines.size)
        // 1 min (60,000) + 15 sec (15,000) + 678 ms = 75678 ms
        assertEquals(75678L, parsed.lines[0].timestampMs)
        assertEquals("High precision timing line", parsed.lines[0].text)
    }

    @Test
    fun parsesMultiTimestampLinesAndSortsThem() {
        val lrc = """
            [00:30.00]Second occurrence
            [00:10.00][00:50.00]Repeated chorus
        """.trimIndent()

        val parsed = LrcParser.parse(lrc)
        assertEquals(3, parsed.lines.size)

        // Sorted by time: 10s, 30s, 50s
        assertEquals(10000L, parsed.lines[0].timestampMs)
        assertEquals("Repeated chorus", parsed.lines[0].text)

        assertEquals(30000L, parsed.lines[1].timestampMs)
        assertEquals("Second occurrence", parsed.lines[1].text)

        assertEquals(50000L, parsed.lines[2].timestampMs)
        assertEquals("Repeated chorus", parsed.lines[2].text)
    }

    @Test
    fun handlesUtf8BomPrefixGracefully() {
        val lrcWithBom = "\uFEFF[00:05.00]Line with BOM"
        val parsed = LrcParser.parse(lrcWithBom)
        assertEquals(1, parsed.lines.size)
        assertEquals(5000L, parsed.lines[0].timestampMs)
        assertEquals("Line with BOM", parsed.lines[0].text)
    }

    @Test
    fun parsesPlainTextLyricsWithoutTimestamps() {
        val plain = """
            First verse of the song
            Second line of text
            Third line of text
        """.trimIndent()

        val parsed = LrcParser.parse(plain)
        assertEquals(LyricsFormat.PLAIN, parsed.format)
        assertEquals(3, parsed.lines.size)
        assertEquals(-1L, parsed.lines[0].timestampMs)
        assertEquals("First verse of the song", parsed.lines[0].text)
    }

    @Test
    fun binarySearchLineIndexAtLookup() {
        val lines = listOf(
            LyricsLine(timestampMs = 2000L, text = "Line 1"),
            LyricsLine(timestampMs = 6000L, text = "Line 2"),
            LyricsLine(timestampMs = 10000L, text = "Line 3"),
            LyricsLine(timestampMs = 15000L, text = "Line 4")
        )

        // Before first line -> -1
        assertEquals(-1, LrcParser.lineIndexAt(lines, 0L))
        assertEquals(-1, LrcParser.lineIndexAt(lines, 1999L))

        // Exactly on line 1
        assertEquals(0, LrcParser.lineIndexAt(lines, 2000L))
        // Between line 1 and line 2 -> still line 1
        assertEquals(0, LrcParser.lineIndexAt(lines, 4500L))

        // Line 2
        assertEquals(1, LrcParser.lineIndexAt(lines, 6000L))
        assertEquals(1, LrcParser.lineIndexAt(lines, 8000L))

        // Line 3
        assertEquals(2, LrcParser.lineIndexAt(lines, 10000L))

        // Line 4 (last line)
        assertEquals(3, LrcParser.lineIndexAt(lines, 15000L))
        // Beyond last line -> still line 4
        assertEquals(3, LrcParser.lineIndexAt(lines, 99999L))

        // Empty lines
        assertEquals(-1, LrcParser.lineIndexAt(emptyList(), 5000L))
    }
}

