package com.owlitech.owli.assist.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.owlitech.owli.assist.camera.CameraFrameSource
import com.owlitech.owli.assist.settings.SettingsViewModel
import com.owlitech.owli.assist.ui.MainViewModel
import com.owlitech.owli.assist.vlm.VlmProfilesConfig
import com.owlitech.owli.assist.ui.screens.AboutScreen
import com.owlitech.owli.assist.ui.screens.DiagnosticsScreen
import com.owlitech.owli.assist.ui.screens.HomeScreen
import com.owlitech.owli.assist.ui.screens.SettingsScreen
import com.owlitech.owli.assist.ui.screens.VlmProfilesScreen
import com.owlitech.owli.assist.ui.screens.VlmScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    mainViewModel: MainViewModel,
    settingsViewModel: SettingsViewModel,
    cameraFrameSource: CameraFrameSource,
    vlmProfilesConfig: VlmProfilesConfig,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val sceneState by mainViewModel.sceneState.collectAsState()
    val isRunning by mainViewModel.isRunning.collectAsState()
    val lastError by mainViewModel.lastError.collectAsState()
    val status by mainViewModel.status.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val vlmState by mainViewModel.vlmUiState.collectAsState()
    val isAutoScanRunning by mainViewModel.isAutoScanRunning.collectAsState()
    val activeVlmProfile = vlmProfilesConfig.resolve(settings.vlmProfileId)

    NavHost(
        navController = navController,
        startDestination = AppRoute.Home.route,
        modifier = Modifier.padding(contentPadding)
    ) {
        composable(AppRoute.Home.route) {
            HomeScreen(
                isRunning = isRunning,
                sceneMessage = sceneState?.primaryMessage,
                detections = sceneState?.detections.orEmpty(),
                lastError = lastError,
                statusMessage = status,
                detectionsCount = sceneState?.detections?.size ?: 0,
                hazardLevel = sceneState?.overallHazardLevel?.name ?: "NONE",
                trafficLights = sceneState?.trafficLights.orEmpty(),
                blindViewPreview = if (settings.showBlindViewPreview) sceneState?.blindViewUtterancePreview else null,
                showOverlay = settings.showOverlay,
                showLabels = settings.showOverlayLabels,
                onStart = onStart,
                onStop = onStop,
                cameraFrameSource = cameraFrameSource,
                rotationDegrees = cameraFrameSource.lastRotationDegrees,
                modifier = Modifier
            )
        }
        composable(AppRoute.Settings.route) {
            SettingsScreen(
                settings = settings,
                activeVlmProfileLabel = activeVlmProfile.label,
                onOpenVlmProfiles = { navController.navigate(AppRoute.VlmProfiles.route) },
                onUpdate = { update -> settingsViewModel.update { update(it) } },
                onReset = { settingsViewModel.reset() }
            )
        }
        composable(AppRoute.Diagnostics.route) {
            DiagnosticsScreen(settings = settings)
        }
        composable(AppRoute.Vlm.route) {
            LaunchedEffect(Unit) {
                mainViewModel.enterVlmMode()
            }
            DisposableEffect(Unit) {
                onDispose { mainViewModel.closeVlm() }
            }
            VlmScreen(
                state = vlmState,
                onNewScene = { mainViewModel.requestNewScene() },
                onAsk = { question -> mainViewModel.askVlm(question) },
                cameraFrameSource = cameraFrameSource,
                autoScanAvailable = activeVlmProfile.autoScan != null,
                isAutoScanRunning = isAutoScanRunning,
                onStartAutoScan = { mainViewModel.startAutoScan() },
                onStopAutoScan = { mainViewModel.stopAutoScan() }
            )
        }
        composable(AppRoute.VlmProfiles.route) {
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
        composable(AppRoute.About.route) {
            AboutScreen()
        }
    }
}
