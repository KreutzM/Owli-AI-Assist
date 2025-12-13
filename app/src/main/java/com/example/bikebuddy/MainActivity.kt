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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.bikeassist.audio.AudioFeedbackEngine
import com.example.bikeassist.camera.CameraFrameSource
import com.example.bikeassist.ml.Detection
import com.example.bikeassist.pipeline.VisionPipelineModule
import com.example.bikeassist.pipeline.VisionPipelineModule.create
import com.example.bikeassist.ui.MainViewModel
import com.example.bikeassist.util.AppLogger
import com.example.bikebuddy.ui.theme.BikeBuddyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val cameraFrameSource by lazy { CameraFrameSource(this, this) }
    private val audioFeedbackEngine by lazy { AudioFeedbackEngine(this) }

    private val viewModel: MainViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.start()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("Activity onCreate")
        enableEdgeToEdge()
        bindPipeline()
        observeSceneState()
        setContent {
            BikeBuddyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val sceneState by viewModel.sceneState.collectAsState()
                    val isRunning by viewModel.isRunning.collectAsState()
                    val lastError by viewModel.lastError.collectAsState()
                    val status by viewModel.status.collectAsState()
                    DemoScreen(
                        isRunning = isRunning,
                        sceneMessage = sceneState?.primaryMessage,
                        detections = sceneState?.detections.orEmpty(),
                        lastError = lastError,
                        statusMessage = status,
                        detectionsCount = sceneState?.detections?.size ?: 0,
                        hazardLevel = sceneState?.overallHazardLevel?.name ?: "NONE",
                        trafficLights = sceneState?.trafficLights.orEmpty(),
                        onStart = { ensureCameraPermissionAndStart() },
                        onStop = { viewModel.stop() },
                        cameraFrameSource = cameraFrameSource,
                        rotationDegrees = cameraFrameSource.lastRotationDegrees,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        AppLogger.d("Activity onStop -> stop pipeline")
        viewModel.stop()
    }

    override fun onDestroy() {
        AppLogger.d("Activity onDestroy")
        audioFeedbackEngine.close()
        super.onDestroy()
    }

    private fun bindPipeline() {
        val handle = create(
            context = this,
            lifecycleOwner = this,
            scope = lifecycleScope,
            cameraFrameSource = cameraFrameSource,
            useFake = false
        )
        viewModel.setPipeline(handle)
    }

    private fun ensureCameraPermissionAndStart() {
        when (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> viewModel.start()
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun observeSceneState() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.sceneState.collect { state ->
                    state?.let { audioFeedbackEngine.onSceneUpdated(it) }
                }
            }
        }
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
    onStart: () -> Unit,
    onStop: () -> Unit,
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
            DetectionOverlay(
                detections = detections,
                modifier = Modifier
                    .fillMaxSize()
            )
            SceneOverlay(
                hazardLevel = hazardLevel,
                detectionsCount = detectionsCount,
                message = sceneMessage,
                rotationText = rotationDegrees?.let { "${it}°" },
                trafficLights = trafficLights,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(Color(0x66000000))
                    .padding(8.dp)
            )
        }
            ControlPanel(
                isRunning = isRunning,
                sceneMessage = sceneMessage,
                lastError = lastError,
                statusMessage = statusMessage,
                onStart = onStart,
                onStop = onStop,
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
    trafficLights: List<com.example.bikeassist.domain.TrafficLightObservation>,
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
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Status: ${if (isRunning) "Läuft" else "Gestoppt"}")
        Text(text = "Detector: $statusMessage")
        sceneMessage?.let { Text(text = "Letzte Meldung: $it") }
        lastError?.let { Text(text = "Fehler: $it", color = MaterialTheme.colorScheme.error) }
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
            onStop = {}
        )
    }
}
