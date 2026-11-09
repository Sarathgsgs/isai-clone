package com.isai.player.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesOf
import com.isai.player.data.equalizer.EqPreset
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EqualizerRepositoryTest {

    private val dataStore = mockk<DataStore<Preferences>>(relaxed = true)
    private lateinit var repository: EqualizerRepositoryImpl

    @Before
    fun setUp() {
        repository = EqualizerRepositoryImpl(dataStore)
    }

    @Test
    fun equalizerState_defaultsToSafeValuesWhenEmpty() = runTest {
        every { dataStore.data } returns flowOf(preferencesOf())

        val state = repository.equalizerState.first()
        assertFalse(state.enabled)
        assertEquals("Flat", state.presetName)
        assertTrue(state.bandGains.isEmpty())
        assertEquals(0, state.bassBoostStrength)
        assertEquals(0, state.virtualizerStrength)
        assertEquals(0.0f, state.preampDb, 0.01f)
        assertTrue(state.preventClipping)
    }

    @Test
    fun customPresets_decodesJsonListProperly() = runTest {
        val json = """[{"name":"MyRock","referenceFreqsHz":[60,1000],"referenceGainsDb":[4.0,2.0],"isCustom":true}]"""
        val prefs = preferencesOf(
            androidx.datastore.preferences.core.stringPreferencesKey("eq_custom_presets") to json
        )
        every { dataStore.data } returns flowOf(prefs)

        val presets = repository.customPresets.first()
        assertEquals(1, presets.size)
        assertEquals("MyRock", presets[0].name)
        assertTrue(presets[0].isCustom)
        assertEquals(4.0f, presets[0].referenceGainsDb[0], 0.01f)
    }
}

