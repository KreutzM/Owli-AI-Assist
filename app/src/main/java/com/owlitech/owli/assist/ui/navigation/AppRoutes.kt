package com.owlitech.owli.assist.ui.navigation

import androidx.annotation.StringRes
import com.owlitech.owli.assist.R

sealed class AppRoute(
    val route: String,
    @StringRes val titleRes: Int
) {
    data object Vlm : AppRoute("vlm", R.string.nav_title_app)
    data object VlmSettings : AppRoute("vlm_settings", R.string.nav_title_vlm_settings)
    data object VlmProfiles : AppRoute("vlm_profiles", R.string.nav_title_vlm_profiles)
    data object OpenRouterKeyQrImport : AppRoute("openrouter_key_qr_import", R.string.nav_title_openrouter_key_qr_import)
    data object Help : AppRoute("help", R.string.nav_title_help)
    data object About : AppRoute("about", R.string.nav_title_about)

    companion object {
        fun fromRoute(route: String?): AppRoute = when (route) {
            VlmSettings.route -> VlmSettings
            VlmProfiles.route -> VlmProfiles
            OpenRouterKeyQrImport.route -> OpenRouterKeyQrImport
            Help.route -> Help
            About.route -> About
            else -> Vlm
        }
    }
}
