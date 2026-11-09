package com.isai.player.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.isai.player.data.equalizer.EqPreset
import com.isai.player.data.equalizer.EqualizerState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : EqualizerRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private object PreferencesKeys {
        val EQ_ENABLED = booleanPreferencesKey("eq_enabled")
        val EQ_PRESET_NAME = stringPreferencesKey("eq_preset_name")
        val EQ_BAND_GAINS = stringPreferencesKey("eq_band_gains")
        val EQ_BASS_BOOST = intPreferencesKey("eq_bass_boost")
        val EQ_VIRTUALIZER = intPreferencesKey("eq_virtualizer")
        val EQ_PREAMP_DB = floatPreferencesKey("eq_preamp_db")
        val EQ_PREVENT_CLIPPING = booleanPreferencesKey("eq_prevent_clipping")
        val EQ_CUSTOM_PRESETS = stringPreferencesKey("eq_custom_presets")
    }

    private val safePreferences: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val equalizerState: Flow<EqualizerState> = safePreferences.map { prefs ->
        val enabled = prefs[PreferencesKeys.EQ_ENABLED] ?: false
        val presetName = prefs[PreferencesKeys.EQ_PRESET_NAME] ?: "Flat"
        val rawGains = prefs[PreferencesKeys.EQ_BAND_GAINS]
        val bandGains = if (rawGains.isNullOrBlank()) {
            emptyList()
        } else {
            rawGains.split(",").mapNotNull { it.trim().toIntOrNull() }
        }
        val bassBoost = prefs[PreferencesKeys.EQ_BASS_BOOST] ?: 0
        val virtualizer = prefs[PreferencesKeys.EQ_VIRTUALIZER] ?: 0
        val preampDb = prefs[PreferencesKeys.EQ_PREAMP_DB] ?: 0.0f
        val preventClipping = prefs[PreferencesKeys.EQ_PREVENT_CLIPPING] ?: true

        EqualizerState(
            enabled = enabled,
            presetName = presetName,
            bandGains = bandGains,
            bassBoostStrength = bassBoost,
            virtualizerStrength = virtualizer,
            preampDb = preampDb,
            preventClipping = preventClipping
        )
    }

    override val customPresets: Flow<List<EqPreset>> = safePreferences.map { prefs ->
        val rawJson = prefs[PreferencesKeys.EQ_CUSTOM_PRESETS]
        if (rawJson.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching {
                json.decodeFromString(ListSerializer(EqPreset.serializer()), rawJson)
            }.getOrDefault(emptyList())
        }
    }

    override suspend fun setEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EQ_ENABLED] = enabled
        }
    }

    override suspend fun setPreset(name: String, bandGainsMilliBels: List<Int>) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EQ_PRESET_NAME] = name
            prefs[PreferencesKeys.EQ_BAND_GAINS] = bandGainsMilliBels.joinToString(",")
        }
    }

    override suspend fun setBandGain(bandIndex: Int, gainMilliBels: Int) {
        val currentState = equalizerState.first()
        val currentGains = currentState.bandGains.toMutableList()
        if (bandIndex in currentGains.indices) {
            currentGains[bandIndex] = gainMilliBels
            dataStore.edit { prefs ->
                prefs[PreferencesKeys.EQ_PRESET_NAME] = "Custom"
                prefs[PreferencesKeys.EQ_BAND_GAINS] = currentGains.joinToString(",")
            }
        }
    }

    override suspend fun setBassBoost(strength: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EQ_BASS_BOOST] = strength.coerceIn(0, 1000)
        }
    }

    override suspend fun setVirtualizer(strength: Int) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EQ_VIRTUALIZER] = strength.coerceIn(0, 1000)
        }
    }

    override suspend fun setPreamp(preampDb: Float) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EQ_PREAMP_DB] = preampDb.coerceIn(-12.0f, 12.0f)
        }
    }

    override suspend fun setPreventClipping(prevent: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EQ_PREVENT_CLIPPING] = prevent
        }
    }

    override suspend fun saveCustomPreset(preset: EqPreset) {
        val current = customPresets.first().toMutableList()
        current.removeAll { it.name.equals(preset.name, ignoreCase = true) }
        current.add(preset.copy(isCustom = true))
        val encoded = json.encodeToString(ListSerializer(EqPreset.serializer()), current)
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EQ_CUSTOM_PRESETS] = encoded
        }
    }

    override suspend fun deleteCustomPreset(presetName: String) {
        val current = customPresets.first().toMutableList()
        current.removeAll { it.name.equals(presetName, ignoreCase = true) }
        val encoded = json.encodeToString(ListSerializer(EqPreset.serializer()), current)
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.EQ_CUSTOM_PRESETS] = encoded
        }
    }
}

