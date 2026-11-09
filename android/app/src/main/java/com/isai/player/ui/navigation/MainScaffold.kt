package com.isai.player.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.isai.player.R
import com.isai.player.data.repository.ThemeMode
import com.isai.player.playback.PlayerController
import com.isai.player.playback.PlayerState
import com.isai.player.ui.components.BottomBarTab
import com.isai.player.ui.components.IsaiBottomBar
import com.isai.player.ui.components.IsaiTopBar
import com.isai.player.ui.components.MiniPlayer
import com.isai.player.ui.detail.AlbumDetailScreen
import com.isai.player.ui.detail.ArtistDetailScreen
import com.isai.player.ui.favorites.FavoritesScreen
import com.isai.player.ui.home.HomeScreen
import com.isai.player.ui.library.LibraryScreen
import com.isai.player.ui.nowplaying.NowPlayingScreen
import com.isai.player.ui.onboarding.OnboardingScreen
import com.isai.player.ui.playlists.PlaylistDetailScreen
import com.isai.player.ui.search.SearchScreen
import com.isai.player.ui.settings.SettingsScreen
import com.isai.player.ui.theme.IsaiTheme

@Composable
fun MainScaffold(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    startDestination: NavRoute = NavRoute.Home,
    playerController: PlayerController? = null,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val defaultPlayerState = remember { PlayerState() }
    val playerState = playerController?.playerState?.collectAsStateWithLifecycle()?.value ?: defaultPlayerState

    // Track active bottom tab
    var selectedTab by remember { mutableStateOf(BottomBarTab.HOME) }

    // Check if bottom bar and mini player should be visible
    val isPrimaryTab = currentDestination?.let { route ->
        route.contains("Home") || route.contains("Library") ||
            route.contains("Favorites") || route.contains("Search")
    } ?: true

    val isNowPlayingOrOnboarding = currentDestination?.let { route ->
        route.contains("NowPlaying") || route.contains("Onboarding")
    } ?: false

    Scaffold(
        topBar = {
            if (isPrimaryTab) {
                IsaiTopBar(
                    actions = {
                        IconButton(onClick = { navController.navigate(NavRoute.Search) }) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { navController.navigate(NavRoute.Settings) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        bottomBar = {
            if (isPrimaryTab) {
                IsaiBottomBar(
                    currentTab = selectedTab,
                    onTabSelected = { tab ->
                        selectedTab = tab
                        val route = when (tab) {
                            BottomBarTab.HOME -> NavRoute.Home
                            BottomBarTab.LIBRARY -> NavRoute.Library
                            BottomBarTab.FAVORITES -> NavRoute.Favorites
                            BottomBarTab.SEARCH -> NavRoute.Search
                        }
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        containerColor = IsaiTheme.colors.background,
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Navigation Host
            NavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize()
            ) {
                composable<NavRoute.Home> {
                    HomeScreen(
                        onAlbumClick = { albumKey ->
                            navController.navigate(NavRoute.AlbumDetail(albumKey))
                        }
                    )
                }

                composable<NavRoute.Library> {
                    LibraryScreen(
                        onTrackClick = {
                            navController.navigate(NavRoute.NowPlaying)
                        },
                        onAlbumClick = { albumKey ->
                            navController.navigate(NavRoute.AlbumDetail(albumKey))
                        },
                        onArtistClick = { artistKey ->
                            navController.navigate(NavRoute.ArtistDetail(artistKey))
                        },
                        onPlaylistClick = { playlistId ->
                            navController.navigate(NavRoute.PlaylistDetail(playlistId))
                        }
                    )
                }

                composable<NavRoute.Favorites> {
                    FavoritesScreen(
                        onTrackClick = {
                            navController.navigate(NavRoute.NowPlaying)
                        }
                    )
                }

                composable<NavRoute.Search> {
                    SearchScreen(
                        onTrackClick = {
                            navController.navigate(NavRoute.NowPlaying)
                        },
                        onAlbumClick = { albumKey ->
                            navController.navigate(NavRoute.AlbumDetail(albumKey))
                        },
                        onArtistClick = { artistKey ->
                            navController.navigate(NavRoute.ArtistDetail(artistKey))
                        }
                    )
                }

                composable<NavRoute.AlbumDetail> { backStackEntry ->
                    val route: NavRoute.AlbumDetail = backStackEntry.toRoute()
                    AlbumDetailScreen(
                        albumKey = route.albumKey,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<NavRoute.ArtistDetail> { backStackEntry ->
                    val route: NavRoute.ArtistDetail = backStackEntry.toRoute()
                    ArtistDetailScreen(
                        artistKey = route.artistKey,
                        onBackClick = { navController.popBackStack() },
                        onAlbumClick = { albumKey ->
                            navController.navigate(NavRoute.AlbumDetail(albumKey))
                        }
                    )
                }

                composable<NavRoute.PlaylistDetail> { backStackEntry ->
                    val route: NavRoute.PlaylistDetail = backStackEntry.toRoute()
                    PlaylistDetailScreen(
                        playlistId = route.playlistId,
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<NavRoute.Settings> {
                    SettingsScreen(
                        currentThemeMode = currentThemeMode,
                        onThemeModeChange = onThemeModeChange,
                        onBackClick = { navController.popBackStack() },
                        onOpenLabsClick = { navController.navigate(NavRoute.LabsDashboard) },
                        onOpenEqualizerClick = { navController.navigate(NavRoute.Equalizer) }
                    )
                }

                composable<NavRoute.Equalizer> {
                    com.isai.player.ui.equalizer.EqualizerScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<NavRoute.Onboarding> {
                    OnboardingScreen(
                        onScanCompleted = {
                            navController.navigate(NavRoute.Home) {
                                popUpTo<NavRoute.Onboarding> { inclusive = true }
                            }
                        }
                    )
                }

                composable<NavRoute.LabsDashboard> {
                    LabsDashboardScreen(
                        onBackClick = { navController.popBackStack() },
                        onNavigateToLab = { labRoute ->
                            when (labRoute) {
                                is LabsRoute.TonearmLab -> navController.navigate(NavRoute.TonearmLab)
                                is LabsRoute.VinylDiscLab -> navController.navigate(NavRoute.VinylDiscLab)
                                is LabsRoute.VinylStageLab -> navController.navigate(NavRoute.VinylStageLab)
                                else -> {}
                            }
                        }
                    )
                }

                composable<NavRoute.TonearmLab> {
                    com.isai.player.ui.components.TonearmLabScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<NavRoute.VinylDiscLab> {
                    com.isai.player.ui.components.VinylDiscLabScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                composable<NavRoute.VinylStageLab> {
                    com.isai.player.ui.components.VinylStageLabScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }

                // Full-screen Now Playing with slide-up / slide-down animation
                composable<NavRoute.NowPlaying>(
                    enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
                    exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() },
                    popEnterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
                    popExitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() }
                ) {
                    NowPlayingScreen(
                        onCollapseClick = { navController.popBackStack() }
                    )
                }
            }

            // Floating MiniPlayer pinned above bottom bar (visible on primary & detail tabs)
            AnimatedVisibility(
                visible = isPrimaryTab && !isNowPlayingOrOnboarding && playerState.hasTrack,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                val track = playerState.currentTrack
                val trackTitle = track?.title ?: stringResource(id = R.string.miniplayer_placeholder_title)
                val trackArtist = track?.let { "${it.artist} · ${it.album}" } ?: stringResource(id = R.string.miniplayer_placeholder_artist)

                MiniPlayer(
                    title = trackTitle,
                    artist = trackArtist,
                    artUri = track?.artPath,
                    isPlaying = playerState.isPlaying,
                    progress = playerState.progress,
                    onClick = { navController.navigate(NavRoute.NowPlaying) },
                    onPlayPauseClick = { playerController?.togglePlayPause() },
                    onNextClick = { playerController?.skipToNext() }
                )
            }
        }
    }
}

