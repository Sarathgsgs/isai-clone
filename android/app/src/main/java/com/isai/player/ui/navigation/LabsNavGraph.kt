package com.isai.player.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation destination routes for Developer Labs and isolated component testing.
 */
sealed interface LabsRoute {
    @Serializable
    data object LabsDashboard : LabsRoute

    @Serializable
    data object TonearmLab : LabsRoute

    @Serializable
    data object VinylDiscLab : LabsRoute

    @Serializable
    data object VinylStageLab : LabsRoute
}

