package com.isai.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.isai.player.data.repository.SettingsRepository
import com.isai.player.data.repository.ThemeMode
import com.isai.player.playback.PlayerController
import com.isai.player.ui.navigation.MainScaffold
import com.isai.player.ui.theme.IsaiTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var playerController: PlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentThemeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(
                initialValue = ThemeMode.SYSTEM
            )
            val selectedFolderUri by settingsRepository.selectedFolderUri.collectAsStateWithLifecycle(
                initialValue = "CHECKING"
            )
            val scope = rememberCoroutineScope()

            if (selectedFolderUri != "CHECKING") {
                val hasPermission = isFolderPermissionGranted(selectedFolderUri)
                if (!hasPermission && !selectedFolderUri.isNullOrBlank()) {
                    // Permission was revoked externally; reset stored folder
                    scope.launch {
                        settingsRepository.clearSelectedFolder()
                    }
                }

                val startRoute = if (!hasPermission || selectedFolderUri.isNullOrBlank()) {
                    com.isai.player.ui.navigation.NavRoute.Onboarding
                } else {
                    com.isai.player.ui.navigation.NavRoute.Home
                }

                IsaiTheme(themeMode = currentThemeMode) {
                    MainScaffold(
                        currentThemeMode = currentThemeMode,
                        onThemeModeChange = { newMode ->
                            scope.launch {
                                settingsRepository.setThemeMode(newMode)
                            }
                        },
                        startDestination = startRoute,
                        playerController = playerController,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    private fun isFolderPermissionGranted(uriString: String?): Boolean {
        if (uriString.isNullOrBlank()) return false
        return try {
            val targetUri = android.net.Uri.parse(uriString)
            contentResolver.persistedUriPermissions.any { perm ->
                perm.uri == targetUri && perm.isReadPermission
            }
        } catch (e: Exception) {
            false
        }
    }
}
