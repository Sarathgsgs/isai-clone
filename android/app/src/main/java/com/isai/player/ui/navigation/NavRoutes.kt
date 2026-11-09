package com.isai.player.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute {

    @Serializable
    data object Onboarding : NavRoute

    @Serializable
    data object Main : NavRoute

    @Serializable
    data object Home : NavRoute

    @Serializable
    data object Library : NavRoute

    @Serializable
    data object Favorites : NavRoute

    @Serializable
    data object Search : NavRoute

    @Serializable
    data class AlbumDetail(val albumKey: String) : NavRoute

    @Serializable
    data class ArtistDetail(val artistKey: String) : NavRoute

    @Serializable
    data class PlaylistDetail(val playlistId: Long) : NavRoute

    @Serializable
    data object NowPlaying : NavRoute

    @Serializable
    data object Settings : NavRoute

    @Serializable
    data object Equalizer : NavRoute

    @Serializable
    data object LabsDashboard : NavRoute

    @Serializable
    data object TonearmLab : NavRoute

    @Serializable
    data object VinylDiscLab : NavRoute

    @Serializable
    data object VinylStageLab : NavRoute
}

