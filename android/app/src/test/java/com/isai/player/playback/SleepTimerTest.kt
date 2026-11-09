package com.isai.player.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SleepTimerTest {

    private lateinit var sleepTimer: SleepTimer
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    @Before
    fun setup() {
        sleepTimer = SleepTimer()
    }

    @Test
    fun timerDefaultStateIsIdle() {
        assertFalse(sleepTimer.isRunning)
        assertEquals(0L, sleepTimer.remainingMs.value)
        assertFalse(sleepTimer.isStopAtEndOfTrack.value)
    }

    @Test
    fun startingTimerSetsRunningFlag() {
        sleepTimer.startTimer(
            durationMs = 900_000L, // 15 mins
            stopAtEndOfTrack = false,
            scope = scope,
            onVolumeUpdate = {},
            onTimerComplete = {}
        )

        assertTrue(sleepTimer.isRunning)
        assertEquals(900_000L, sleepTimer.remainingMs.value)
        assertFalse(sleepTimer.isStopAtEndOfTrack.value)

        sleepTimer.cancel()
        assertFalse(sleepTimer.isRunning)
        assertEquals(0L, sleepTimer.remainingMs.value)
    }

    @Test
    fun stopAtEndOfTrackFlagIsSet() {
        var completed = false
        sleepTimer.startTimer(
            durationMs = 0L,
            stopAtEndOfTrack = true,
            scope = scope,
            onVolumeUpdate = {},
            onTimerComplete = { completed = true }
        )

        assertTrue(sleepTimer.isRunning)
        assertTrue(sleepTimer.isStopAtEndOfTrack.value)
        assertFalse(completed)

        sleepTimer.onTrackEnded { completed = true }
        assertTrue(completed)
        assertFalse(sleepTimer.isRunning)
    }
}

