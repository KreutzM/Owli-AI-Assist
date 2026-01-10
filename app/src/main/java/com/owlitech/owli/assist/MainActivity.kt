package com.owlitech.owli.assist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.owlitech.owli.assist.audio.AudioFeedbackEngine
import com.owlitech.owli.assist.audio.StreamingTtsController
import com.owlitech.owli.assist.camera.CameraFrameSource
import com.owlitech.owli.assist.pipeline.VisionPipelineModule
import com.owlitech.owli.assist.settings.AppSettings
import com.owlitech.owli.assist.settings.AppSettingsDefaults
import com.owlitech.owli.assist.settings.LanguagePreference
import com.owlitech.owli.assist.settings.SettingsRepository
import com.owlitech.owli.assist.settings.SettingsViewModel
import com.owlitech.owli.assist.ui.MainViewModel
import com.owlitech.owli.assist.util.AppLogger
import com.owlitech.owli.assist.vlm.OpenRouterVlmClient
import com.owlitech.owli.assist.vlm.VlmProfile
import com.owlitech.owli.assist.vlm.VlmProfileLoader
import com.owlitech.owli.assist.vlm.VlmProfilesConfig
import com.owlitech.owli.assist.vlm.VlmUiState
import com.owlitech.owli.assist.ui.AppTopBar
import com.owlitech.owli.assist.ui.navigation.AppNavHost
import com.owlitech.owli.assist.ui.navigation.AppRoute
import com.owlitech.owli.assist.ui.theme.OwliTheme
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val cameraFrameSource by lazy { CameraFrameSource(this, this) }
    private val audioFeedbackEngine by lazy { AudioFeedbackEngine(this) }
    private val streamingTtsController by lazy {
        StreamingTtsController(
            speaker = object : StreamingTtsController.Speaker {
                override fun speakChunk(text: String, queueMode: com.owlitech.owli.assist.audio.QueueMode) {
                    audioFeedbackEngine.speakVlmStreamingChunk(text, queueMode)
                }

                override fun stop() {
                    audioFeedbackEngine.stopVlmTts()
                }
            }
        )
    }

    private lateinit var vlmProfilesConfig: VlmProfilesConfig
    private val vlmClient by lazy {
        OpenRouterVlmClient(vlmProfilesConfig.resolve(vlmProfilesConfig.defaultProfileId))
    }
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(vlmClient)
    }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(SettingsRepository(applicationContext))
    }
    private var settingsCollectJob: Job? = null
    private var streamingTimeoutJob: Job? = null
    private var streamingActive = false
    private var lastStreamingText = ""
    private var streamingTtsEnabled: Boolean = AppSettingsDefaults.streamingVlmTtsEnabled
    private var voiceInputActive = false
    private lateinit var activeVlmProfile: VlmProfile

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                AppLogger.d("Permission granted -> autoStartIfNeeded")
                mainViewModel.autoStartIfNeeded()
            } else {
                AppLogger.d("Permission denied -> no start")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("Activity onCreate")
        vlmProfilesConfig = VlmProfileLoader.load(applicationContext)
        activeVlmProfile = vlmProfilesConfig.resolve(vlmProfilesConfig.defaultProfileId)
        mainViewModel.applyVlmProfile(activeVlmProfile)
        audioFeedbackEngine.setOnVlmStreamIdleListener {
            updateSceneSpeechSuppression()
        }
        enableEdgeToEdge()
        observeSceneState()
        observeVlmState()
        observeSettings()
        setContent {
            OwliTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = AppRoute.fromRoute(navBackStackEntry?.destination?.route)
                val canNavigateBack = navController.previousBackStackEntry != null

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        AppTopBar(
                            titleRes = currentRoute.titleRes,
                            canNavigateBack = canNavigateBack,
                            showVlmAction = currentRoute == AppRoute.Home,
                            onNavigateBack = { navController.popBackStack() },
                            onOpenVlm = {
                                navController.navigate(AppRoute.Vlm.route) { launchSingleTop = true }
                            },
                            onOpenSettings = {
                                navController.navigate(AppRoute.Settings.route) { launchSingleTop = true }
                            },
                            onOpenVlmSettings = {
                                navController.navigate(AppRoute.VlmSettings.route) { launchSingleTop = true }
                            },
                            onOpenDiagnostics = {
                                navController.navigate(AppRoute.Diagnostics.route) { launchSingleTop = true }
                            },
                            onOpenAbout = {
                                navController.navigate(AppRoute.About.route) { launchSingleTop = true }
                            }
                        )
                    }
                ) { innerPadding ->
                    AppNavHost(
                        navController = navController,
                        contentPadding = innerPadding,
                        mainViewModel = mainViewModel,
                        settingsViewModel = settingsViewModel,
                        cameraFrameSource = cameraFrameSource,
                        vlmProfilesConfig = vlmProfilesConfig,
                        onStart = { onUserStart() },
                        onStop = { onUserStop() },
                        onVoiceInputActiveChanged = { active -> setVoiceInputActive(active) }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppLogger.d("Activity onStart, shouldAutoStart=${mainViewModel.shouldAutoStart.value}")
        ensurePermissionAndAutoStart()
    }

    override fun onStop() {
        super.onStop()
        AppLogger.d("Activity onStop -> stop pipeline (lifecycle)")
        mainViewModel.stopForLifecycle()
        streamingTimeoutJob?.cancel()
        streamingActive = false
        lastStreamingText = ""
        streamingTtsController.cancel()
        audioFeedbackEngine.stopAllTts()
    }

    override fun onDestroy() {
        AppLogger.d("Activity onDestroy")
        audioFeedbackEngine.close()
        super.onDestroy()
    }

    private fun onUserStart() {
        AppLogger.d("User requested start")
        mainViewModel.requestStart()
        ensurePermissionAndAutoStart()
    }

    private fun onUserStop() {
        AppLogger.d("User requested stop")
        mainViewModel.stopUser()
    }

    private fun ensurePermissionAndAutoStart() {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> mainViewModel.autoStartIfNeeded()
            else -> {
                AppLogger.d("Requesting camera permission for autoStart=${mainViewModel.shouldAutoStart.value}")
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun observeSceneState() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                mainViewModel.sceneState.collect { state ->
                    state?.let { audioFeedbackEngine.onSceneUpdated(it) }
                }
            }
        }
    }

    private fun observeVlmState() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                mainViewModel.vlmUiState.collect { state ->
                    val backgroundVolume = 0.25f
                    val standardVolume = if (state is VlmUiState.Inactive) 1.0f else backgroundVolume
                    audioFeedbackEngine.setStandardVolume(standardVolume)
                    when (state) {
                        is VlmUiState.Streaming -> handleStreamingState(state)
                        else -> handleStreamingEnd(state)
                    }
                }
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSettings() {
        settingsCollectJob?.cancel()
        settingsCollectJob = lifecycleScope.launch {
            settingsViewModel.settings
                .debounce(400)
                .distinctUntilChanged()
                .collect { settings ->
                    applySettings(settings)
                }
        }
    }

    private fun applySettings(settings: AppSettings) {
        applyLocale(settings.languagePreference)
        val migratedProfileId = when (settings.vlmProfileId) {
            "nano_safe" -> "nano-low"
            "nano_fast" -> "nano-high"
            else -> settings.vlmProfileId
        }
        if (migratedProfileId != settings.vlmProfileId) {
            settingsViewModel.update { it.copy(vlmProfileId = migratedProfileId) }
            return
        }
        val defaultProfileId = vlmProfilesConfig.defaultProfileId
        val profileExists = vlmProfilesConfig.profiles.any { it.id == settings.vlmProfileId }
        if ((!settings.vlmProfileIdUserSet && settings.vlmProfileId != defaultProfileId) || !profileExists) {
            settingsViewModel.update {
                it.copy(vlmProfileId = defaultProfileId, vlmProfileIdUserSet = false)
            }
            return
        }
        audioFeedbackEngine.updateSpeechRate(settings.ttsSpeechRate)
        audioFeedbackEngine.updatePitch(settings.ttsPitch)
        streamingTtsEnabled = settings.streamingVlmTtsEnabled
        updateSceneSpeechSuppression()
        com.owlitech.owli.assist.diagnostics.DiagnosticsCollector.updateSettings(settings)
        com.owlitech.owli.assist.diagnostics.DiagnosticsCollector.updatePipelineStatus(
            isRunning = mainViewModel.isRunning.value,
            detectorInfo = mainViewModel.status.value,
            analysisIntervalMs = settings.analysisIntervalMs
        )
        val handle = VisionPipelineModule.create(
            context = this,
            lifecycleOwner = this,
            scope = lifecycleScope,
            cameraFrameSource = cameraFrameSource,
            useFake = false,
            detectorOptions = settings.toDetectorOptions(),
            mode = settings.appMode,
            blindViewConfig = settings.toBlindViewConfig(),
            analysisIntervalMs = settings.analysisIntervalMs
        )
        mainViewModel.setPipeline(handle)
        activeVlmProfile = vlmProfilesConfig.resolve(settings.vlmProfileId)
        mainViewModel.applyVlmProfile(activeVlmProfile)
        ensurePermissionAndAutoStart()
    }

    private fun applyLocale(preference: LanguagePreference) {
        val targetLocales = when (preference) {
            LanguagePreference.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            LanguagePreference.DE -> LocaleListCompat.forLanguageTags("de")
            LanguagePreference.EN -> LocaleListCompat.forLanguageTags("en")
        }
        if (AppCompatDelegate.getApplicationLocales() != targetLocales) {
            AppCompatDelegate.setApplicationLocales(targetLocales)
            if (!isFinishing && !isDestroyed) {
                recreate()
            }
        }
    }

    private fun handleStreamingState(state: VlmUiState.Streaming) {
        if (!streamingActive) {
            streamingActive = true
            lastStreamingText = ""
            if (shouldUseStreamingTts()) {
                streamingTtsController.startNewRun()
            }
        }
        val delta = computeStreamingDelta(lastStreamingText, state.partialText)
        lastStreamingText = state.partialText
        if (!voiceInputActive && delta.isNotEmpty() && shouldUseStreamingTts()) {
            streamingTtsController.onDelta(delta)
            scheduleStreamingTimeout()
        }
        updateSceneSpeechSuppression()
    }

    private fun handleStreamingEnd(state: VlmUiState) {
        val hadStreamingOutput = streamingActive || lastStreamingText.isNotEmpty()
        if (streamingActive) {
            streamingActive = false
            streamingTimeoutJob?.cancel()
            if (!voiceInputActive) {
                val shouldFlush = shouldUseStreamingTts() &&
                    (state is VlmUiState.OverviewReady || state is VlmUiState.OverviewReadyRaw)
                if (shouldFlush) {
                    streamingTtsController.flushRemaining()
                } else {
                    streamingTtsController.cancel()
                }
            } else {
                streamingTtsController.cancel()
            }
            lastStreamingText = ""
        }
        updateSceneSpeechSuppression()
        if (!voiceInputActive && (!shouldUseStreamingTts() || !hadStreamingOutput)) {
            when (state) {
                is VlmUiState.OverviewReady -> {
                    audioFeedbackEngine.speakVlmResponse(
                        state.description.ttsOneLiner,
                        state.description.actionSuggestion
                    )
                }
                is VlmUiState.OverviewReadyRaw -> {
                    audioFeedbackEngine.speakVlmResponse(state.rawText, null)
                }
                else -> Unit
            }
        }
    }

    private fun scheduleStreamingTimeout() {
        streamingTimeoutJob?.cancel()
        streamingTimeoutJob = lifecycleScope.launch {
            delay(StreamingTtsController.DEFAULT_IDLE_TIMEOUT_MS)
            streamingTtsController.onIdleTimeout()
        }
    }

    private fun computeStreamingDelta(previous: String, current: String): String {
        return if (current.startsWith(previous)) {
            current.substring(previous.length)
        } else {
            AppLogger.w("VLM", "Streaming text reset detected.")
            if (shouldUseStreamingTts()) {
                streamingTtsController.startNewRun()
            }
            current
        }
    }

    private fun updateSceneSpeechSuppression() {
        val shouldSuppress = voiceInputActive || (shouldUseStreamingTts() &&
            (streamingActive || streamingTtsController.hasPending() || audioFeedbackEngine.isVlmStreamBusy())
        )
        audioFeedbackEngine.setSceneSpeechSuppressed(shouldSuppress)
    }

    private fun shouldUseStreamingTts(): Boolean {
        return streamingTtsEnabled && activeVlmProfile.streamingEnabled
    }

    private fun setVoiceInputActive(active: Boolean) {
        if (voiceInputActive == active) return
        voiceInputActive = active
        if (active) {
            streamingTtsController.cancel()
            audioFeedbackEngine.stopAllTts()
            audioFeedbackEngine.setSceneSpeechSuppressed(true)
        } else {
            updateSceneSpeechSuppression()
        }
    }
}
