package com.owlitech.owli.assist

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
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
import com.owlitech.owli.assist.settings.AppSettings
import com.owlitech.owli.assist.settings.AppSettingsDefaults
import com.owlitech.owli.assist.settings.LanguagePreference
import com.owlitech.owli.assist.settings.SettingsRepository
import com.owlitech.owli.assist.settings.SettingsViewModel
import com.owlitech.owli.assist.ui.AppTopBar
import com.owlitech.owli.assist.ui.MainViewModel
import com.owlitech.owli.assist.ui.navigation.AppNavHost
import com.owlitech.owli.assist.ui.navigation.AppRoute
import com.owlitech.owli.assist.ui.theme.OwliTheme
import com.owlitech.owli.assist.util.AppLogger
import com.owlitech.owli.assist.vlm.OpenRouterVlmClient
import com.owlitech.owli.assist.vlm.VlmProfile
import com.owlitech.owli.assist.vlm.VlmProfileLoader
import com.owlitech.owli.assist.vlm.VlmProfilesConfig
import com.owlitech.owli.assist.vlm.VlmUiState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

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
    private var ttsEnabled: Boolean = AppSettingsDefaults.ttsEnabled
    private var voiceInputActive = false
    private lateinit var activeVlmProfile: VlmProfile

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                AppLogger.d("Camera permission granted")
            } else {
                AppLogger.d("Camera permission denied")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("Activity onCreate")
        vlmProfilesConfig = VlmProfileLoader.load(applicationContext)
        activeVlmProfile = vlmProfilesConfig.resolve(vlmProfilesConfig.defaultProfileId)
        mainViewModel.applyVlmProfile(activeVlmProfile)
        audioFeedbackEngine.setOnVlmStreamIdleListener {
            updateVlmAudioState()
        }
        enableEdgeToEdge()
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
                            onNavigateBack = { navController.popBackStack() },
                            onOpenVlmSettings = {
                                navController.navigate(AppRoute.VlmSettings.route) { launchSingleTop = true }
                            },
                            onOpenHelp = {
                                navController.navigate(AppRoute.Help.route) { launchSingleTop = true }
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
                        vlmProfilesConfig = vlmProfilesConfig,
                        onVoiceInputActiveChanged = { active -> setVoiceInputActive(active) },
                        onRepeatLastVlmResponse = { primary, secondary ->
                            repeatLastVlmResponse(primary, secondary)
                        },
                        onAddVlmImage = { bytes -> mainViewModel.addVlmAttachmentForActiveSession(bytes) }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ensureCameraPermission()
    }

    override fun onStop() {
        super.onStop()
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

    private fun ensureCameraPermission() {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> Unit
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun observeVlmState() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                mainViewModel.vlmUiState.collect { state ->
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
        audioFeedbackEngine.setTtsEnabled(settings.ttsEnabled)
        ttsEnabled = settings.ttsEnabled
        streamingTtsEnabled = settings.streamingVlmTtsEnabled
        if (!ttsEnabled) {
            streamingTtsController.cancel()
        }
        activeVlmProfile = vlmProfilesConfig.resolve(settings.vlmProfileId)
        mainViewModel.applyVlmProfile(activeVlmProfile)
        updateVlmAudioState()
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
        updateVlmAudioState()
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
        updateVlmAudioState()
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

    private fun updateVlmAudioState() {
        audioFeedbackEngine.setVlmVolume(
            if (voiceInputActive) 0.0f else 1.0f
        )
    }

    private fun shouldUseStreamingTts(): Boolean {
        return ttsEnabled && streamingTtsEnabled && activeVlmProfile.streamingEnabled
    }

    private fun setVoiceInputActive(active: Boolean) {
        if (voiceInputActive == active) return
        voiceInputActive = active
        if (active) {
            streamingTtsController.cancel()
            audioFeedbackEngine.stopAllTts()
        } else {
            updateVlmAudioState()
        }
    }

    private fun repeatLastVlmResponse(primary: String?, secondary: String?) {
        if (primary.isNullOrBlank() && secondary.isNullOrBlank()) return
        audioFeedbackEngine.speakVlmResponse(primary, secondary)
    }
}
