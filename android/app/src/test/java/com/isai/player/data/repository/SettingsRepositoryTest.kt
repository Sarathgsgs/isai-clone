package com.isai.player.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private lateinit var testScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: SettingsRepository
    private lateinit var testFile: File

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        testScope = CoroutineScope(Dispatchers.IO + Job())
        testFile = context.preferencesDataStoreFile("test_settings_${System.currentTimeMillis()}")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        repository = SettingsRepositoryImpl(dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        if (testFile.exists()) {
            testFile.delete()
        }
    }

    @Test
    fun defaultSettingsValuesAreCorrect() = runTest {
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.first())
        assertEquals(LiftBehavior.PAUSE, repository.liftBehavior.first())
        assertNull(repository.selectedFolderUri.first())
        assertNull(repository.selectedFolderName.first())
        assertTrue(repository.dynamicAccentColor.first())
        assertEquals(0L, repository.sleepTimerEndTimestamp.first())
        assertFalse(repository.sleepTimerStopAtEnd.first())
    }

    @Test
    fun updateThemeModePersistsAndEmits() = runTest {
        repository.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, repository.themeMode.first())

        repository.setThemeMode(ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, repository.themeMode.first())
    }

    @Test
    fun updateLiftBehaviorPersistsAndEmits() = runTest {
        repository.setLiftBehavior(LiftBehavior.STOP)
        assertEquals(LiftBehavior.STOP, repository.liftBehavior.first())

        repository.setLiftBehavior(LiftBehavior.PAUSE)
        assertEquals(LiftBehavior.PAUSE, repository.liftBehavior.first())
    }

    @Test
    fun updateSelectedFolderAndClear() = runTest {
        val uri = "content://com.android.externalstorage.documents/tree/primary%3AMusic"
        val name = "Music"

        repository.setSelectedFolder(uri, name)
        assertEquals(uri, repository.selectedFolderUri.first())
        assertEquals(name, repository.selectedFolderName.first())

        repository.clearSelectedFolder()
        assertNull(repository.selectedFolderUri.first())
        assertNull(repository.selectedFolderName.first())
    }

    @Test
    fun updateSleepTimerAndClear() = runTest {
        val endEpochMs = System.currentTimeMillis() + 1800000L // 30 mins
        repository.setSleepTimer(endEpochMs, stopAtEndOfTrack = true)

        assertEquals(endEpochMs, repository.sleepTimerEndTimestamp.first())
        assertTrue(repository.sleepTimerStopAtEnd.first())

        repository.clearSleepTimer()
        assertEquals(0L, repository.sleepTimerEndTimestamp.first())
        assertFalse(repository.sleepTimerStopAtEnd.first())
    }
}

