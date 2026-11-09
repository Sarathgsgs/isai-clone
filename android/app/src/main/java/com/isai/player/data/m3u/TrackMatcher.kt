package com.isai.player.data.m3u

import com.isai.player.data.model.Track
import java.util.Locale
import kotlin.math.abs

enum class MatchStage {
    EXACT_RELATIVE_PATH,
    SUFFIX_PATH,
    FILENAME_AND_DURATION,
    TAG_METADATA
}

sealed interface MatchResolution {
    data class Single(val track: Track, val stage: MatchStage) : MatchResolution
    data class Ambiguous(val candidates: List<Track>) : MatchResolution
    data object None : MatchResolution
}

data class ResolvedM3uItem(
    val entry: M3uEntry,
    val resolution: MatchResolution
)

data class MatchSummary(
    val items: List<ResolvedM3uItem>
) {
    val matchedCount: Int get() = items.count { it.resolution is MatchResolution.Single }
    val ambiguousCount: Int get() = items.count { it.resolution is MatchResolution.Ambiguous }
    val notFoundCount: Int get() = items.count { it.resolution is MatchResolution.None }

    val matchedTracks: List<Track>
        get() = items.mapNotNull { (it.resolution as? MatchResolution.Single)?.track }
}

object TrackMatcher {

    /**
     * Matches a list of parsed M3U entries against the local track library using a 4-stage resolution engine.
     */
    fun matchEntries(entries: List<M3uEntry>, library: List<Track>): MatchSummary {
        val resolvedItems = entries.map { entry ->
            val resolution = matchSingleEntry(entry, library)
            ResolvedM3uItem(entry, resolution)
        }
        return MatchSummary(resolvedItems)
    }

    private fun matchSingleEntry(entry: M3uEntry, library: List<Track>): MatchResolution {
        val entryNorm = PathNormalizer.normalizeForSearch(entry.rawPath)
        val entryFilename = PathNormalizer.getFileName(entry.rawPath).lowercase(Locale.ROOT)
        val entryDurationSec = entry.durationSeconds

        // -------------------------------------------------------------
        // Stage 1: Exact relative path match
        // -------------------------------------------------------------
        val exactMatches = library.filter { track ->
            val trackRelNorm = track.relativePath?.let { PathNormalizer.normalizeForSearch(it) }
            trackRelNorm == entryNorm
        }
        if (exactMatches.size == 1) {
            return MatchResolution.Single(exactMatches.first(), MatchStage.EXACT_RELATIVE_PATH)
        }
        if (exactMatches.size > 1) {
            return disambiguateOrAmbiguous(exactMatches, entryDurationSec, MatchStage.EXACT_RELATIVE_PATH)
        }

        // -------------------------------------------------------------
        // Stage 2: Suffix match (trailing path segments)
        // -------------------------------------------------------------
        val trailing2 = PathNormalizer.getTrailingSegments(entryNorm, 2).lowercase(Locale.ROOT)
        val suffixMatches = library.filter { track ->
            val trackRelNorm = track.relativePath?.let { PathNormalizer.normalizeForSearch(it) } ?: ""
            trackRelNorm.endsWith(trailing2) || trackRelNorm.endsWith(entryNorm) || entryNorm.endsWith(trackRelNorm)
        }
        if (suffixMatches.size == 1) {
            return MatchResolution.Single(suffixMatches.first(), MatchStage.SUFFIX_PATH)
        }
        if (suffixMatches.size > 1) {
            return disambiguateOrAmbiguous(suffixMatches, entryDurationSec, MatchStage.SUFFIX_PATH)
        }

        // -------------------------------------------------------------
        // Stage 3: Filename match with duration tolerance (±2s)
        // -------------------------------------------------------------
        val filenameMatches = library.filter { track ->
            val trackFilename = PathNormalizer.getFileName(track.relativePath ?: track.uri).lowercase(Locale.ROOT)
            trackFilename == entryFilename
        }
        if (filenameMatches.size == 1) {
            return MatchResolution.Single(filenameMatches.first(), MatchStage.FILENAME_AND_DURATION)
        }
        if (filenameMatches.size > 1) {
            val disambiguated = disambiguateByDuration(filenameMatches, entryDurationSec, toleranceSec = 2)
            if (disambiguated.size == 1) {
                return MatchResolution.Single(disambiguated.first(), MatchStage.FILENAME_AND_DURATION)
            }
            return MatchResolution.Ambiguous(filenameMatches)
        }

        // -------------------------------------------------------------
        // Stage 4: Tag match (title and artist with ±3s duration)
        // -------------------------------------------------------------
        if (!entry.title.isNullOrBlank()) {
            val tagMatches = library.filter { track ->
                val titleMatch = track.title.equals(entry.title, ignoreCase = true)
                val artistMatch = if (entry.artist.isNullOrBlank()) true else {
                    track.artist.contains(entry.artist, ignoreCase = true) ||
                        entry.artist.contains(track.artist, ignoreCase = true)
                }
                titleMatch && artistMatch
            }

            if (tagMatches.size == 1) {
                return MatchResolution.Single(tagMatches.first(), MatchStage.TAG_METADATA)
            }
            if (tagMatches.size > 1) {
                val disambiguated = disambiguateByDuration(tagMatches, entryDurationSec, toleranceSec = 3)
                if (disambiguated.size == 1) {
                    return MatchResolution.Single(disambiguated.first(), MatchStage.TAG_METADATA)
                }
                return MatchResolution.Ambiguous(tagMatches)
            }
        }

        return MatchResolution.None
    }

    private fun disambiguateOrAmbiguous(
        candidates: List<Track>,
        durationSec: Int?,
        stage: MatchStage
    ): MatchResolution {
        val filtered = disambiguateByDuration(candidates, durationSec, toleranceSec = 2)
        return if (filtered.size == 1) {
            MatchResolution.Single(filtered.first(), stage)
        } else {
            MatchResolution.Ambiguous(candidates)
        }
    }

    private fun disambiguateByDuration(
        candidates: List<Track>,
        targetDurationSec: Int?,
        toleranceSec: Int
    ): List<Track> {
        if (targetDurationSec == null || targetDurationSec <= 0) return candidates
        return candidates.filter { track ->
            val trackSec = (track.durationMs / 1000L).toInt()
            abs(trackSec - targetDurationSec) <= toleranceSec
        }
    }
}

