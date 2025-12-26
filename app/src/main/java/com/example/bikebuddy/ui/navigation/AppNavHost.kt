package com.example.bikebuddy.ui.navigation

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
import com.example.bikeassist.camera.CameraFrameSource
import com.example.bikeassist.settings.SettingsViewModel
import com.example.bikeassist.ui.MainViewModel
import com.example.bikeassist.vlm.VlmProfilesConfig
import com.example.bikebuddy.ui.screens.AboutScreen
import com.example.bikebuddy.ui.screens.DiagnosticsScreen
import com.example.bikebuddy.ui.screens.HomeScreen
import com.example.bikebuddy.ui.screens.SettingsScreen
import com.example.bikebuddy.ui.screens.VlmProfilesScreen
import com.example.bikebuddy.ui.screens.VlmScreen

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
                onNewScene = { mainViewModel.enterVlmMode() },
                onAsk = { question -> mainViewModel.askVlm(question) },
                cameraFrameSource = cameraFrameSource
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
