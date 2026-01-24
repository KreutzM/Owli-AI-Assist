package com.owlitech.owli.assist.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.R
import com.owlitech.owli.assist.camera.CameraFrameSource
import com.owlitech.owli.assist.domain.HazardLevel
import com.owlitech.owli.assist.settings.SettingsViewModel
import com.owlitech.owli.assist.ui.MainViewModel
import com.owlitech.owli.assist.vlm.VlmProfilesConfig
import com.owlitech.owli.assist.ui.screens.AboutScreen
import com.owlitech.owli.assist.ui.screens.DiagnosticsScreen
import com.owlitech.owli.assist.ui.screens.HomeScreen
import com.owlitech.owli.assist.ui.screens.SettingsScreen
import com.owlitech.owli.assist.ui.screens.VlmSettingsScreen
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
    onStop: () -> Unit,
    onVoiceInputActiveChanged: (Boolean) -> Unit
) {
    val sceneState by mainViewModel.sceneState.collectAsState()
    val isRunning by mainViewModel.isRunning.collectAsState()
    val lastError by mainViewModel.lastError.collectAsState()
    val status by mainViewModel.status.collectAsState()
    val settings by settingsViewModel.settings.collectAsState()
    val vlmState by mainViewModel.vlmUiState.collectAsState()
    val isAutoScanRunning by mainViewModel.isAutoScanRunning.collectAsState()
    val activeVlmProfile = vlmProfilesConfig.resolve(settings.vlmProfileId)
    val hazardLabel = when (sceneState?.overallHazardLevel) {
        HazardLevel.WARNING -> stringResource(R.string.hazard_level_warning)
        HazardLevel.DANGER -> stringResource(R.string.hazard_level_danger)
        HazardLevel.NONE, null -> stringResource(R.string.hazard_level_none)
    }
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
        if (settings.detectorModeEnabled) {
            composable(AppRoute.Home.route) {
                Box(modifier = Modifier.padding(defaultPadding)) {
                    HomeScreen(
                        isRunning = isRunning,
                        sceneMessage = sceneState?.primaryMessage,
                        detections = sceneState?.detections.orEmpty(),
                        lastError = lastError,
                        statusMessage = status,
                        detectionsCount = sceneState?.detections?.size ?: 0,
                        hazardLevel = hazardLabel,
                        trafficLights = sceneState?.trafficLights.orEmpty(),
                        blindViewPreview = if (settings.showBlindViewPreview) {
                            sceneState?.blindViewUtterancePreview
                        } else {
                            null
                        },
                        showOverlay = settings.showOverlay,
                        showLabels = settings.showOverlayLabels,
                        frameMapping = sceneState?.frameMapping,
                        detectorDebugBitmap = sceneState?.detectorDebugBitmap,
                        showDetectorDebugView = settings.enableDetectorDebugView,
                        onStart = onStart,
                        onStop = onStop,
                        cameraFrameSource = cameraFrameSource,
                        rotationDegrees = cameraFrameSource.lastRotationDegrees,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            composable(AppRoute.Settings.route) {
                Box(modifier = Modifier.padding(defaultPadding)) {
                    SettingsScreen(
                        settings = settings,
                        onUpdate = { update -> settingsViewModel.update { update(it) } },
                        onReset = { settingsViewModel.reset() }
                    )
                }
            }
            composable(AppRoute.Diagnostics.route) {
                Box(modifier = Modifier.padding(defaultPadding)) {
                    DiagnosticsScreen(settings = settings)
                }
            }
        }
        composable(AppRoute.VlmSettings.route) {
            Box(modifier = Modifier.padding(defaultPadding)) {
                VlmSettingsScreen(
                    settings = settings,
                    activeVlmProfileLabel = activeVlmProfile.label,
                    onOpenVlmProfiles = { navController.navigate(AppRoute.VlmProfiles.route) },
                    onUpdate = { update -> settingsViewModel.update { update(it) } }
                )
            }
        }
        composable(AppRoute.Vlm.route) {
            LaunchedEffect(Unit) {
                mainViewModel.enterVlmMode()
            }
            DisposableEffect(Unit) {
                onDispose { mainViewModel.closeVlm() }
            }
            Box(modifier = Modifier.padding(topOnlyPadding)) {
                VlmScreen(
                    state = vlmState,
                    onNewScene = { mainViewModel.requestNewScene() },
                    onAsk = { question -> mainViewModel.askVlm(question) },
                    onVoiceInputActiveChanged = onVoiceInputActiveChanged,
                    cameraFrameSource = cameraFrameSource,
                    autoScanAvailable = activeVlmProfile.autoScan != null,
                    isAutoScanRunning = isAutoScanRunning,
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
        composable(AppRoute.About.route) {
            Box(modifier = Modifier.padding(defaultPadding)) {
                AboutScreen()
            }
        }
    }
}
