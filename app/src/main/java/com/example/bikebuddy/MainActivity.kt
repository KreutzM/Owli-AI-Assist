package com.example.bikebuddy

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bikeassist.audio.AudioFeedbackEngine
import com.example.bikeassist.camera.CameraFrameSource
import com.example.bikeassist.domain.TrafficLightObservation
import com.example.bikeassist.ml.Detection
import com.example.bikeassist.pipeline.VisionPipelineModule
import com.example.bikeassist.diagnostics.DiagnosticsCollector
import com.example.bikeassist.diagnostics.DiagnosticsReportBuilder
import com.example.bikeassist.settings.AppSettings
import com.example.bikeassist.settings.SettingsRepository
import com.example.bikeassist.settings.SettingsViewModel
import com.example.bikeassist.ui.MainViewModel
import com.example.bikeassist.util.AppLogger
import com.example.bikeassist.vlm.OpenRouterVlmClient
import com.example.bikeassist.vlm.VlmConfigLoader
import com.example.bikeassist.vlm.VlmProfile
import com.example.bikeassist.vlm.VlmProfileLoader
import com.example.bikeassist.vlm.VlmUiState
import com.example.bikebuddy.ui.theme.BikeBuddyTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val cameraFrameSource by lazy { CameraFrameSource(this, this) }
    private val audioFeedbackEngine by lazy { AudioFeedbackEngine(this) }

    private val vlmProfilesConfig by lazy { VlmProfileLoader.load(applicationContext) }
    private val vlmConfig by lazy { VlmConfigLoader.load(applicationContext) }
    private val vlmClient by lazy {
        OpenRouterVlmClient(vlmConfig, vlmProfilesConfig.resolve(vlmProfilesConfig.defaultProfileId))
    }
    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.Factory(vlmClient)
    }
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(SettingsRepository(applicationContext))
    }
    private var settingsCollectJob: Job? = null
    private val showSettings = mutableStateOf(false)
    private val showDiagnostics = mutableStateOf(false)
    private val showVlm = mutableStateOf(false)
    private val showVlmProfiles = mutableStateOf(false)

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
        mainViewModel.applyVlmProfile(vlmProfilesConfig.resolve(vlmProfilesConfig.defaultProfileId))
        enableEdgeToEdge()
        observeSceneState()
        observeVlmState()
        observeSettings()
        setContent {
            BikeBuddyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val sceneState by mainViewModel.sceneState.collectAsState()
                    val isRunning by mainViewModel.isRunning.collectAsState()
                    val lastError by mainViewModel.lastError.collectAsState()
                    val status by mainViewModel.status.collectAsState()
                    val settings by settingsViewModel.settings.collectAsState()
                    val vlmState by mainViewModel.vlmUiState.collectAsState()
                    val activeVlmProfile = vlmProfilesConfig.resolve(settings.vlmProfileId)
                    val showSettingsState = remember { showSettings }
                    val showVlmState = remember { showVlm }
                    val showVlmProfilesState = remember { showVlmProfiles }

                    DemoScreen(
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
                        onStart = { onUserStart() },
                        onStop = { onUserStop() },
                        onOpenSettings = { showSettingsState.value = true },
                        onOpenDiagnostics = { showDiagnostics.value = true },
                        onOpenVlm = {
                            showVlmState.value = true
                            mainViewModel.enterVlmMode()
                        },
                        cameraFrameSource = cameraFrameSource,
                        rotationDegrees = cameraFrameSource.lastRotationDegrees,
                        modifier = Modifier.padding(innerPadding)
                    )

                    if (showSettingsState.value) {
                        SettingsScreen(
                            settings = settings,
                            activeVlmProfileLabel = activeVlmProfile.label,
                            onOpenVlmProfiles = { showVlmProfilesState.value = true },
                            onUpdate = { update -> settingsViewModel.update { update(it) } },
                            onClose = { showSettingsState.value = false },
                            onReset = { settingsViewModel.reset() }
                        )
                    }
                    if (showDiagnostics.value) {
                        DiagnosticsScreen(
                            settings = settings,
                            onClose = { showDiagnostics.value = false }
                        )
                    }
                    if (showVlmState.value) {
                        VlmOverlay(
                            state = vlmState,
                            onNewScene = { mainViewModel.enterVlmMode() },
                            onAsk = { question -> mainViewModel.askVlm(question) },
                            onClose = {
                                showVlmState.value = false
                                mainViewModel.closeVlm()
                            }
                        )
                    }
                    if (showVlmProfilesState.value) {
                        VlmProfileScreen(
                            profiles = vlmProfilesConfig.profiles,
                            activeProfileId = activeVlmProfile.id,
                            onSelect = { profile ->
                                settingsViewModel.update { it.copy(vlmProfileId = profile.id) }
                                showVlmProfilesState.value = false
                            },
                            onClose = { showVlmProfilesState.value = false }
                        )
                    }
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
                    if (state is VlmUiState.OverviewReadyRaw) {
                        return@collect
                    }
                }
            }
        }
    }

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
        val migratedProfileId = when (settings.vlmProfileId) {
            "nano_safe" -> "nano-low"
            "nano_fast" -> "nano-high"
            else -> settings.vlmProfileId
        }
        if (migratedProfileId != settings.vlmProfileId) {
            settingsViewModel.update { it.copy(vlmProfileId = migratedProfileId) }
            return
        }
        audioFeedbackEngine.updateSpeechRate(settings.ttsSpeechRate)
        com.example.bikeassist.diagnostics.DiagnosticsCollector.updateSettings(settings)
        com.example.bikeassist.diagnostics.DiagnosticsCollector.updatePipelineStatus(
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
        mainViewModel.applyVlmProfile(vlmProfilesConfig.resolve(settings.vlmProfileId))
        ensurePermissionAndAutoStart()
    }
}

@Composable
fun DemoScreen(
    isRunning: Boolean,
    sceneMessage: String?,
    detections: List<Detection>,
    lastError: String?,
    statusMessage: String,
    detectionsCount: Int,
    hazardLevel: String,
    trafficLights: List<com.example.bikeassist.domain.TrafficLightObservation>,
    blindViewPreview: String?,
    showOverlay: Boolean,
    showLabels: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenVlm: () -> Unit,
    cameraFrameSource: CameraFrameSource,
    rotationDegrees: Int?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(modifier = Modifier.weight(1f)) {
            CameraPreview(cameraFrameSource = cameraFrameSource, modifier = Modifier.fillMaxSize())
            if (showOverlay) {
                DetectionOverlay(
                    detections = detections,
                    showLabels = showLabels,
                    modifier = Modifier.fillMaxSize()
                )
                SceneOverlay(
                    hazardLevel = hazardLevel,
                    detectionsCount = detectionsCount,
                    message = sceneMessage,
                    rotationText = rotationDegrees?.let { "${it}°" },
                    trafficLights = trafficLights,
                    blindViewPreview = blindViewPreview,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .background(Color(0x66000000))
                        .padding(8.dp)
                )
            }
        }
        ControlPanel(
            isRunning = isRunning,
            sceneMessage = sceneMessage,
            lastError = lastError,
            statusMessage = statusMessage,
            onStart = onStart,
            onStop = onStop,
            blindViewPreview = blindViewPreview,
            onOpenSettings = onOpenSettings,
            onOpenDiagnostics = onOpenDiagnostics,
            onOpenVlm = onOpenVlm,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

@Composable
fun CameraPreview(
    cameraFrameSource: CameraFrameSource,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).also { previewView ->
                cameraFrameSource.bindPreviewSurface(previewView.surfaceProvider)
            }
        },
        update = { previewView ->
            cameraFrameSource.bindPreviewSurface(previewView.surfaceProvider)
        }
    )
}

@Composable
fun SceneOverlay(
    hazardLevel: String,
    detectionsCount: Int,
    message: String?,
    rotationText: String?,
    trafficLights: List<TrafficLightObservation>,
    blindViewPreview: String?,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = "Hazard: $hazardLevel", color = Color.White)
        Text(text = "Detections: $detectionsCount", color = Color.White)
        rotationText?.let { Text(text = "Rot: $it", color = Color.White) }
        val primaryTl = trafficLights.maxByOrNull { it.confidence }
        primaryTl?.let {
            Text(text = "TL: ${it.phase} (conf=${"%.2f".format(it.confidence)})", color = Color.White)
        }
        message?.let {
            Text(text = it, color = Color.White)
        }
        blindViewPreview?.let {
            Text(text = "BV: $it", color = Color.White)
        }
    }
}

@Composable
fun DetectionOverlay(
    detections: List<Detection>,
    showLabels: Boolean,
    modifier: Modifier = Modifier
) {
    val maxLabels = 10
    val minLabelConfidence = 0.30f
    val labelPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 40f
            style = android.graphics.Paint.Style.FILL
        }
    }
    val bgPaint = remember {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(160, 0, 0, 0)
            style = android.graphics.Paint.Style.FILL
        }
    }
    val sorted = remember(detections) {
        detections.filter { it.confidence >= minLabelConfidence }
            .sortedByDescending { it.confidence }
            .take(maxLabels)
    }
    androidx.compose.foundation.Canvas(
        modifier = modifier
    ) {
        sorted.forEach { detection ->
            val left = detection.bbox.xMin * size.width
            val top = detection.bbox.yMin * size.height
            val right = detection.bbox.xMax * size.width
            val bottom = detection.bbox.yMax * size.height
            drawRect(
                color = Color.Red,
                topLeft = androidx.compose.ui.geometry.Offset(left, top),
                size = androidx.compose.ui.geometry.Size(right - left, bottom - top),
                style = Stroke(width = 3f)
            )
            if (showLabels) {
                val text = "${detection.label} ${"%.2f".format(detection.confidence)}"
                val fm = labelPaint.fontMetrics
                val textWidth = labelPaint.measureText(text)
                val textHeight = fm.bottom - fm.top
                val padding = 8f
                var textLeft = left
                var textTop = top - padding
                if (textTop < padding) {
                    textTop = top + padding
                }
                if (textLeft + textWidth + padding > size.width) {
                    textLeft = size.width - textWidth - padding
                }
                val rectLeft = textLeft - padding
                val rectTop = textTop - padding
                val rectRight = textLeft + textWidth + padding
                val rectBottom = textTop + textHeight + padding
                drawContext.canvas.nativeCanvas.drawRect(
                    rectLeft,
                    rectTop,
                    rectRight,
                    rectBottom,
                    bgPaint
                )
                val textBaseline = textTop - fm.top
                drawContext.canvas.nativeCanvas.drawText(
                    text,
                    textLeft,
                    textBaseline,
                    labelPaint
                )
            }
        }
    }
}

@Composable
fun ControlPanel(
    isRunning: Boolean,
    sceneMessage: String?,
    lastError: String?,
    statusMessage: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    blindViewPreview: String?,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenVlm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onStart, enabled = !isRunning, modifier = Modifier.weight(1f)) {
                Text(text = "Start")
            }
            Button(onClick = onStop, enabled = isRunning, modifier = Modifier.weight(1f)) {
                Text(text = "Stop")
            }
            Button(onClick = onOpenSettings, modifier = Modifier.weight(1f)) {
                Text(text = "Settings")
            }
            Button(onClick = onOpenDiagnostics, modifier = Modifier.weight(1f)) {
                Text(text = "Diag")
            }
            Button(onClick = onOpenVlm, modifier = Modifier.weight(1f)) {
                Text(text = "VLM")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Status: ${if (isRunning) "Läuft" else "Gestoppt"}")
        Text(text = "Detector: $statusMessage")
        sceneMessage?.let { Text(text = "Letzte Meldung: $it") }
        lastError?.let { Text(text = "Fehler: $it", color = MaterialTheme.colorScheme.error) }
        blindViewPreview?.let { Text(text = "BlindView: $it") }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewControlPanel() {
    BikeBuddyTheme {
        ControlPanel(
            isRunning = true,
            sceneMessage = "Achtung, Person voraus",
            lastError = null,
            statusMessage = "Preview (FakeDetector)",
            onStart = {},
            onStop = {},
            blindViewPreview = "2 Personen, 11 Uhr.",
            onOpenSettings = {},
            onOpenDiagnostics = {},
            onOpenVlm = {}
        )
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    activeVlmProfileLabel: String,
    onOpenVlmProfiles: () -> Unit,
    onUpdate: (((AppSettings) -> AppSettings)) -> Unit,
    onClose: () -> Unit,
    onReset: () -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Einstellungen", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onReset) { Text("Reset") }
                    Button(onClick = onClose) { Text("Schliessen") }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(12.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingSlider(
                label = "Detector minConfidence",
                value = settings.detectorMinConfidence,
                valueRange = 0.05f..0.95f,
                steps = 9,
                onValueChange = { v -> onUpdate { it.copy(detectorMinConfidence = v) } },
                helper = "Score-Threshold"
            )
            SettingIntSlider(
                label = "Detector maxResults",
                value = settings.detectorMaxResults,
                valueRange = 1..10,
                onValueChange = { v -> onUpdate { it.copy(detectorMaxResults = v) } },
                helper = "Top-N Ergebnisse"
            )
            SettingIntSlider(
                label = "Threads",
                value = settings.detectorNumThreads,
                valueRange = 1..4,
                onValueChange = { v -> onUpdate { it.copy(detectorNumThreads = v) } },
                helper = "Interpreter Threads"
            )
            SettingSwitch(
                label = "NNAPI",
                checked = settings.detectorUseNnapi,
                onCheckedChange = { v -> onUpdate { it.copy(detectorUseNnapi = v) } },
                helper = "Beschleuniger (wenn verfügbar)"
            )
            SettingSlider(
                label = "Track minConfidence",
                value = settings.minConfidenceTrack,
                valueRange = 0.1f..0.95f,
                steps = 8,
                onValueChange = { v -> onUpdate { it.copy(minConfidenceTrack = v) } },
                helper = "Filter gegen False Positives"
            )
            SettingSlider(
                label = "IoU Threshold",
                value = settings.iouThreshold,
                valueRange = 0.1f..0.9f,
                steps = 8,
                onValueChange = { v -> onUpdate { it.copy(iouThreshold = v) } },
                helper = "Matching-Schwelle Tracker"
            )
            SettingIntSlider(
                label = "Min Hits",
                value = settings.minConsecutiveHits,
                valueRange = 1..5,
                onValueChange = { v -> onUpdate { it.copy(minConsecutiveHits = v) } },
                helper = "Stabilität Tracker"
            )
            SettingSlider(
                label = "BBox Glättung",
                value = settings.bboxSmoothingAlpha,
                valueRange = 0f..1f,
                steps = 9,
                onValueChange = { v -> onUpdate { it.copy(bboxSmoothingAlpha = v) } },
                helper = "EMA-Anteil (höher = glatter)"
            )
            SettingIntSlider(
                label = "BlindView max Items",
                value = settings.maxItemsSpoken,
                valueRange = 1..12,
                onValueChange = { v -> onUpdate { it.copy(maxItemsSpoken = v) } },
                helper = "Anzahl Objekte pro Ansage"
            )
            SettingSlider(
                label = "TTS Speech Rate",
                value = settings.ttsSpeechRate,
                valueRange = 0.5f..3.0f,
                steps = 10,
                onValueChange = { v -> onUpdate { it.copy(ttsSpeechRate = v) } },
                helper = "Sprechgeschwindigkeit"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("VLM Profil", style = MaterialTheme.typography.titleSmall)
                    Text(activeVlmProfileLabel, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onOpenVlmProfiles) { Text("Auswaehlen") }
            }
            SettingIntSlider(
                label = "Analysis Interval (ms)",
                value = settings.analysisIntervalMs.toInt(),
                valueRange = 150..1000,
                onValueChange = { v -> onUpdate { it.copy(analysisIntervalMs = v.toLong()) } },
                helper = "Min Abstand zwischen Frames"
            )
            SettingSwitch(
                label = "Overlay anzeigen",
                checked = settings.showOverlay,
                onCheckedChange = { v -> onUpdate { it.copy(showOverlay = v) } }
            )
            SettingSwitch(
                label = "Overlay Labels",
                checked = settings.showOverlayLabels,
                onCheckedChange = { v -> onUpdate { it.copy(showOverlayLabels = v) } },
                helper = "BBox-Beschriftung (Label + Confidence)"
            )
            SettingSwitch(
                label = "BlindView Preview",
                checked = settings.showBlindViewPreview,
                onCheckedChange = { v -> onUpdate { it.copy(showBlindViewPreview = v) } }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(onClick = onReset) { Text("Reset Defaults") }
            }
        }
    }
}

@Composable
fun VlmProfileScreen(
    profiles: List<VlmProfile>,
    activeProfileId: String,
    onSelect: (VlmProfile) -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("VLM Profile", style = MaterialTheme.typography.titleMedium)
                Button(onClick = onClose) { Text("Schliessen") }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(profiles, key = { it.id }) { profile ->
                val isActive = profile.id == activeProfileId
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(profile) }
                ) {
                    SectionCard(
                        title = if (isActive) "${profile.label} (Aktiv)" else profile.label
                    ) {
                        profile.description?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                        val tempText = profile.temperature?.let { "%.2f".format(it) } ?: "n/a"
                        val effortText = profile.thinkingEffort?.let { "  Reasoning: $it" } ?: ""
                        Text(
                            text = "Model: ${profile.model}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Temp: $tempText  MaxTokens: ${profile.maxTokens}$effortText",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    helper: String? = null
) {
    Column {
        Text(text = "$label: ${"%.2f".format(value)}")
        androidx.compose.material3.Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
        helper?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun SettingIntSlider(
    label: String,
    value: Int,
    valueRange: IntRange,
    onValueChange: (Int) -> Unit,
    helper: String? = null
) {
    SettingSlider(
        label = label,
        value = value.toFloat(),
        valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
        steps = (valueRange.last - valueRange.first),
        onValueChange = { onValueChange(it.toInt()) },
        helper = helper
    )
}

@Composable
private fun SettingSwitch(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    helper: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label)
            helper?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        androidx.compose.material3.Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun DiagnosticsScreen(
    settings: AppSettings,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val diagState by DiagnosticsCollector.state.collectAsState()
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Diagnostics", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val report = DiagnosticsReportBuilder.build(diagState, settings)
                        clipboard.setText(AnnotatedString(report))
                        Toast.makeText(context, "Debug Report kopiert", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy") }
                    Button(onClick = onClose) { Text("Schließen") }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionCard(title = "App") {
                Text("Version: ${diagState.versionName} (${diagState.versionCode}) build=${diagState.buildType}")
                Text("Device: ${diagState.deviceModel} Android ${diagState.androidVersion}")
            }
            SectionCard(title = "Pipeline") {
                Text("Running: ${diagState.isRunning} fps=${"%.2f".format(diagState.fps)} intervalMs=${"%.1f".format(diagState.frameIntervalMs)}")
                Text("DetectorInfo: ${diagState.detectorInfo}")
                Text("AnalysisIntervalMs: ${diagState.analysisIntervalMs}")
            }
            SectionCard(title = "Detector") {
                Text("Threads=${diagState.detectorNumThreads} Score>=${diagState.detectorScoreThreshold} MaxResults=${diagState.detectorMaxResults}")
            }
            SectionCard(title = "BlindView/TTS") {
                Text("ttsReady=${diagState.ttsReady} speechRate=${"%.2f".format(diagState.ttsSpeechRate)}")
                Text("speakInterval=${diagState.minSpeakIntervalMs} repeatInterval=${diagState.repeatSamePlanIntervalMs} maxItems=${diagState.maxItemsSpoken}")
            }
            SectionCard(title = "Tracking") {
                Text("iou=${diagState.iouThreshold} trackMaxAgeMs=${diagState.trackMaxAgeMs} minHits=${diagState.minConsecutiveHits}")
                Text("overlay=${diagState.showOverlay} labels=${diagState.showOverlayLabels} preview=${diagState.showBlindViewPreview}")
            }
            SectionCard(title = "Scene Snapshot") {
                Text("detectionsRaw=${diagState.detectionsCountRaw} detectionsStable=${diagState.detectionsCountStable}")
                Text("topLabels=${diagState.topLabels.joinToString()}")
                Text("lastPreview=${diagState.lastUtterancePreview ?: "-"}")
            }
        }
    }
}

@Composable
fun VlmOverlay(
    state: VlmUiState,
    onNewScene: () -> Unit,
    onAsk: (String) -> Unit,
    onClose: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isBusy = state is VlmUiState.LoadingOverview || state is VlmUiState.Asking
    var question by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("VLM", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onNewScene, enabled = !isBusy) { Text("Neue Szene") }
                    Button(onClick = onClose) { Text("Schliessen") }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(12.dp)
                .fillMaxSize()
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state) {
                is VlmUiState.Inactive -> {
                    Text("Bereit. Tippe auf 'Neue Szene'.")
                }
                is VlmUiState.LoadingOverview -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(state.message ?: "Lade VLM...")
                    }
                }
                is VlmUiState.Asking -> {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text("Sende Frage...")
                    }
                }
                is VlmUiState.Error -> {
                    Text(text = "Fehler: ${state.message}", color = MaterialTheme.colorScheme.error)
                }
                is VlmUiState.OverviewReadyRaw -> {
                    Text(text = "Antwort:")
                    Text(text = state.rawText)
                }
                is VlmUiState.OverviewReady -> {
                    val desc = state.description
                    val obstaclesText = if (desc.obstacles.isEmpty()) "keine" else desc.obstacles.joinToString()
                    val landmarksText = if (desc.landmarks.isEmpty()) "keine" else desc.landmarks.joinToString()
                    Text(text = "Kurz: ${desc.ttsOneLiner}")
                    Text(text = "Empfehlung: ${desc.actionSuggestion}")
                    Text(text = "Hindernisse: $obstaclesText")
                    Text(text = "Landmarken: $landmarksText")
                    Text(text = "Details: ${desc.readableText}")
                    desc.overallConfidence?.let { Text(text = "Confidence: $it") }
                }
            }

            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Frage stellen") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy
            )
            Button(
                onClick = {
                    val text = question.trim()
                    if (text.isNotEmpty()) {
                        onAsk(text)
                        question = ""
                    }
                },
                enabled = !isBusy && question.isNotBlank()
            ) {
                Text("Senden")
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x11FFFFFF), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        content()
    }
}
