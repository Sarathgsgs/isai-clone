package com.isai.player.ui.components

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TonearmStateTest {

    private lateinit var geometry: TonearmGeometry
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setup() {
        geometry = TonearmGeometry.forDisc(Offset(200f, 200f), 150f)
    }

    @Test
    fun initialStateIsParkedAtRest() {
        val state = TonearmState(geometry, testScope)

        assertEquals(1f, state.lift.value, 0.001f)
        assertNull(state.dragAngle)
        assertFalse(state.isScrubbing)

        // When parked (lift = 1f), display angle should be at alphaRest regardless of progress
        val displayAngleAtZero = state.displayAngle(0f)
        val displayAngleAtHalf = state.displayAngle(0.5f)
        assertEquals(geometry.alphaRest, displayAngleAtZero, 0.001f)
        assertEquals(geometry.alphaRest, displayAngleAtHalf, 0.001f)
    }

    @Test
    fun loweredStateTracksProgressAccurately() = testScope.runTest {
        val state = TonearmState(geometry, testScope)
        state.animateLower()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0f, state.lift.value, 0.001f)

        // At progress 0, displayAngle equals alphaOuter
        assertEquals(geometry.alphaOuter, state.displayAngle(0f), 0.001f)

        // At progress 1, displayAngle equals alphaInner
        assertEquals(geometry.alphaInner, state.displayAngle(1f), 0.001f)

        // At progress 0.5, displayAngle is midpoint
        val expectedMidpoint = (geometry.alphaOuter + geometry.alphaInner) / 2f
        assertEquals(expectedMidpoint, state.displayAngle(0.5f), 0.001f)
    }

    @Test
    fun dragAngleOverridesTrackFollowMode() = testScope.runTest {
        val state = TonearmState(geometry, testScope)
        state.animateLower()
        testDispatcher.scheduler.advanceUntilIdle()

        val arbitraryDragAngle = 105f
        state.setDragging(arbitraryDragAngle)

        assertTrue(state.isScrubbing)
        assertEquals(arbitraryDragAngle, state.displayAngle(0.2f), 0.001f)
        assertEquals(arbitraryDragAngle, state.displayAngle(0.9f), 0.001f)

        // Release drag
        state.setDragging(null)
        assertFalse(state.isScrubbing)
        assertEquals(geometry.trackAngle(0.5f), state.displayAngle(0.5f), 0.001f)
    }

    @Test
    fun fakePlayerPlaybackAndSeeking() = testScope.runTest {
        val player = FakePlayer(testScope, durationMs = 200_000L)

        assertEquals(0L, player.positionMs.value)
        assertFalse(player.isPlaying.value)

        player.play()
        assertTrue(player.isPlaying.value)

        // Advance 5 seconds (5000 ms)
        advanceTimeBy(5000L)
        testDispatcher.scheduler.runCurrent()
        assertTrue("Position should have advanced", player.positionMs.value > 0L)

        // Seek to 50%
        player.seekToProgress(0.5f)
        assertEquals(100_000L, player.positionMs.value)

        // Lifted stop with PAUSE retains position
        player.liftBehavior = LiftBehavior.PAUSE
        player.handleLiftedStop()
        assertFalse(player.isPlaying.value)
        assertEquals(100_000L, player.positionMs.value)

        // Lifted stop with STOP resets position to 0
        player.liftBehavior = LiftBehavior.STOP
        player.play()
        player.handleLiftedStop()
        assertFalse(player.isPlaying.value)
        assertEquals(0L, player.positionMs.value)
    }

    @Test
    fun fakePlayerNoTrackDisablesPlayback() = testScope.runTest {
        val player = FakePlayer(testScope)
        player.setHasTrack(false)

        player.play()
        assertFalse("Playback should not start when hasTrack is false", player.isPlaying.value)
    }
}

