package com.owlitech.owli.assist.ui.navigation

sealed class AppRoute(
    val route: String,
    val title: String
) {
    data object Home : AppRoute("home", "Owli-AI")
    data object Settings : AppRoute("settings", "Einstellungen")
    data object Diagnostics : AppRoute("diagnostics", "Diagnostics")
    data object Vlm : AppRoute("vlm", "VLM")
    data object VlmProfiles : AppRoute("vlm_profiles", "VLM Profile")
    data object About : AppRoute("about", "About")

    companion object {
        fun fromRoute(route: String?): AppRoute = when (route) {
            Settings.route -> Settings
            Diagnostics.route -> Diagnostics
            Vlm.route -> Vlm
            VlmProfiles.route -> VlmProfiles
            About.route -> About
            else -> Home
        }
    }
}
