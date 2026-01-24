package com.owlitech.owli.assist.ui.navigation

import androidx.annotation.StringRes
import com.owlitech.owli.assist.R

sealed class AppRoute(
    val route: String,
    @StringRes val titleRes: Int
) {
    data object Home : AppRoute("home", R.string.app_name)
    data object Settings : AppRoute("settings", R.string.nav_title_settings)
    data object VlmSettings : AppRoute("vlm_settings", R.string.nav_title_vlm_settings)
    data object Diagnostics : AppRoute("diagnostics", R.string.nav_title_diagnostics)
    data object Vlm : AppRoute("vlm", R.string.nav_title_app)
    data object VlmProfiles : AppRoute("vlm_profiles", R.string.nav_title_vlm_profiles)
    data object About : AppRoute("about", R.string.nav_title_about)

    companion object {
        fun fromRoute(route: String?): AppRoute = when (route) {
            Settings.route -> Settings
            VlmSettings.route -> VlmSettings
            Diagnostics.route -> Diagnostics
            Vlm.route -> Vlm
            VlmProfiles.route -> VlmProfiles
            About.route -> About
            else -> Home
        }
    }
}
