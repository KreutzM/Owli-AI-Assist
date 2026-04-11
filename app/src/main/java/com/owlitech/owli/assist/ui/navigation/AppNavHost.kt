package com.owlitech.owli.assist.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.owlitech.owli.assist.settings.SettingsViewModel
import com.owlitech.owli.assist.ui.MainViewModel
import com.owlitech.owli.assist.ui.screens.AboutScreen
import com.owlitech.owli.assist.ui.screens.HelpScreen
import com.owlitech.owli.assist.ui.screens.OpenRouterKeyQrImportScreen
import com.owlitech.owli.assist.ui.screens.OpenRouterKeySettingsScreen
import com.owlitech.owli.assist.ui.screens.VlmProfilesScreen
import com.owlitech.owli.assist.ui.screens.VlmScreen
import com.owlitech.owli.assist.ui.screens.VlmSettingsScreen
import com.owlitech.owli.assist.vlm.VlmProfilesConfig

@Composable
fun AppNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    vlmProfilesConfig: VlmProfilesConfig,
    onVoiceInputActiveChanged: (Boolean) -> Unit,
    onRepeatLastVlmResponse: (String?, String?) -> Unit,
    onAddVlmImage: (ByteArray) -> Int?
) {
    val settings by settingsViewModel.settings.collectAsState()
    val hasOpenRouterUserKey by settingsViewModel.hasOpenRouterUserKey.collectAsState()
    val vlmState by mainViewModel.vlmUiState.collectAsState()
    val isAutoScanRunning by mainViewModel.isAutoScanRunning.collectAsState()
    val vlmAttachments by mainViewModel.vlmAttachments.collectAsState()
    val lastVlmImageBytes by mainViewModel.lastVlmImageBytes.collectAsState()
    val activeVlmProfile = vlmProfilesConfig.resolve(settings.vlmProfileId)
    val autoScanIntervalMs = activeVlmProfile.autoScan?.intervalMs?.takeIf { it > 0 } ?: 2000L
    val layoutDirection = LocalLayoutDirection.current
    val defaultPadding = PaddingValues(
        start = contentPadding.calculateLeftPadding(layoutDirection),
        top = contentPadding.calculateTopPadding(),
        end = contentPadding.calculateRightPadding(layoutDirection),
        bottom = contentPadding.calculateBottomPadding()
    )
    val topOnlyPadding = PaddingValues(
        start = contentPadding.calculateLeftPadding(layoutDirection),
        top = contentPadding.calculateTopPadding(),
        end = contentPadding.calculateRightPadding(layoutDirection),
        bottom = 0.dp
    )

    NavHost(
        navController = navController,
        startDestination = AppRoute.Vlm.route,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(AppRoute.VlmSettings.route) {
            Box(modifier = Modifier.padding(defaultPadding)) {
                VlmSettingsScreen(
                    settings = settings,
                    activeVlmProfileLabel = activeVlmProfile.label,
                    hasOpenRouterUserKey = hasOpenRouterUserKey,
                    onOpenVlmProfiles = { navController.navigate(AppRoute.VlmProfiles.route) },
                    onOpenOpenRouterKeySettings = {
                        navController.navigate(AppRoute.OpenRouterKeySettings.route)
                    },
                    onUpdate = { update -> settingsViewModel.update { update(it) } }
                )
            }
        }
        composable(AppRoute.OpenRouterKeySettings.route) {
            Box(modifier = Modifier.padding(defaultPadding)) {
                OpenRouterKeySettingsScreen(
                    keyMode = settings.openRouterKeyMode,
                    hasStoredKey = hasOpenRouterUserKey,
                    onSelectMode = { mode -> settingsViewModel.update { it.copy(openRouterKeyMode = mode) } },
                    onOpenQrImport = { navController.navigate(AppRoute.OpenRouterKeyQrImport.route) },
                    onSaveKey = { apiKey -> settingsViewModel.saveOpenRouterUserKey(apiKey) },
                    onClearKey = { settingsViewModel.clearOpenRouterUserKey() }
                )
            }
        }
        composable(AppRoute.OpenRouterKeyQrImport.route) {
            Box(modifier = Modifier.padding(defaultPadding)) {
                OpenRouterKeyQrImportScreen(
                    onConfirmKey = { apiKey ->
                        settingsViewModel.saveOpenRouterUserKey(apiKey)
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
        composable(AppRoute.Vlm.route) {
            Box(modifier = Modifier.padding(topOnlyPadding)) {
                VlmScreen(
                    state = vlmState,
                    onNewScene = { jpegBytes -> mainViewModel.requestNewSceneWithSnapshot(jpegBytes) },
                    onAsk = { question -> mainViewModel.askVlm(question) },
                    onRepeatLastResponse = onRepeatLastVlmResponse,
                    onAddImage = onAddVlmImage,
                    attachments = vlmAttachments,
                    onRemoveAttachment = { id -> mainViewModel.removeVlmAttachment(id) },
                    lastImageBytes = lastVlmImageBytes,
                    onReset = { mainViewModel.closeVlm() },
                    onVoiceInputActiveChanged = onVoiceInputActiveChanged,
                    autoScanAvailable = activeVlmProfile.autoScan != null,
                    isAutoScanRunning = isAutoScanRunning,
                    autoScanIntervalMs = autoScanIntervalMs,
                    onStartAutoScan = { mainViewModel.startAutoScan() },
                    onStopAutoScan = { mainViewModel.stopAutoScan() }
                )
            }
        }
        composable(AppRoute.VlmProfiles.route) {
            Box(modifier = Modifier.padding(defaultPadding)) {
                VlmProfilesScreen(
                    profiles = vlmProfilesConfig.profiles,
                    activeProfileId = activeVlmProfile.id,
                    onSelect = { profile ->
                        settingsViewModel.update {
                            it.copy(vlmProfileId = profile.id, vlmProfileIdUserSet = true)
                        }
                        navController.popBackStack()
                    }
                )
            }
        }
        composable(AppRoute.Help.route) {
            Box(modifier = Modifier.padding(defaultPadding)) {
                HelpScreen(modifier = Modifier.fillMaxSize())
            }
        }
        composable(AppRoute.About.route) {
            Box(modifier = Modifier.padding(defaultPadding)) {
                AboutScreen()
            }
        }
    }
}
