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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bikeassist.audio.AudioFeedbackEngine
import com.example.bikeassist.camera.CameraFrameSource
import com.example.bikeassist.domain.TrafficLightObservation
import com.example.bikeassist.ml.Detection
import com.example.bikeassist.pipeline.VisionPipelineModule
import com.example.bikeassist.settings.AppSettings
import com.example.bikeassist.settings.SettingsRepository
import com.example.bikeassist.settings.SettingsViewModel
import com.example.bikeassist.ui.MainViewModel
import com.example.bikeassist.util.AppLogger
import com.example.bikebuddy.ui.theme.BikeBuddyTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val cameraFrameSource by lazy { CameraFrameSource(this, this) }
    private val audioFeedbackEngine by lazy { AudioFeedbackEngine(this) }

    private val mainViewModel: MainViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(SettingsRepository(applicationContext))
    }
    private var settingsCollectJob: Job? = null
    private val showSettings = mutableStateOf(false)

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
        enableEdgeToEdge()
        observeSceneState()
        observeSettings()
        setContent {
            BikeBuddyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val sceneState by mainViewModel.sceneState.collectAsState()
                    val isRunning by mainViewModel.isRunning.collectAsState()
                    val lastError by mainViewModel.lastError.collectAsState()
                    val status by mainViewModel.status.collectAsState()
                    val settings by settingsViewModel.settings.collectAsState()
                    val showSettingsState = remember { showSettings }

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
                        onStart = { onUserStart() },
                        onStop = { onUserStop() },
                        onOpenSettings = { showSettingsState.value = true },
                        cameraFrameSource = cameraFrameSource,
                        rotationDegrees = cameraFrameSource.lastRotationDegrees,
                        modifier = Modifier.padding(innerPadding)
                    )

                    if (showSettingsState.value) {
                        SettingsScreen(
                            settings = settings,
                            onUpdate = { update -> settingsViewModel.update { update(it) } },
                            onClose = { showSettingsState.value = false },
                            onReset = { settingsViewModel.reset() }
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
        audioFeedbackEngine.updateSpeechRate(settings.ttsSpeechRate)
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
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
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
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
    ) {
        detections.forEach { detection ->
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onStart, enabled = !isRunning) {
                Text(text = "Start")
            }
            Button(onClick = onStop, enabled = isRunning) {
                Text(text = "Stop")
            }
            Button(onClick = onOpenSettings) {
                Text(text = "Settings")
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
            onOpenSettings = {}
        )
    }
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
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
