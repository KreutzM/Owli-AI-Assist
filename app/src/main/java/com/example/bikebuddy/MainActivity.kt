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
import com.example.bikeassist.ui.MainViewModel
import com.example.bikebuddy.ui.theme.BikeBuddyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val cameraFrameSource by lazy { CameraFrameSource(this, this) }
    private val audioFeedbackEngine by lazy { AudioFeedbackEngine(this) }

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val pipeline = VisionPipelineModule.create(
                    context = this@MainActivity,
                    lifecycleOwner = this@MainActivity,
                    scope = lifecycleScope,
                    cameraFrameSource = cameraFrameSource
                )
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(pipeline) as T
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                viewModel.start()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        observeSceneState()
        setContent {
            BikeBuddyTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val sceneState by viewModel.sceneState.collectAsState()
                    val isRunning by viewModel.isRunning.collectAsState()
                    val lastError by viewModel.lastError.collectAsState()
                    DemoScreen(
                        isRunning = isRunning,
                        sceneMessage = sceneState?.primaryMessage,
                        detections = sceneState?.detections.orEmpty(),
                        lastError = lastError,
                        detectionsCount = sceneState?.detections?.size ?: 0,
                        hazardLevel = sceneState?.overallHazardLevel?.name ?: "NONE",
                        onStart = { ensureCameraPermissionAndStart() },
                        onStop = { viewModel.stop() },
                        cameraFrameSource = cameraFrameSource,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        viewModel.stop()
    }

    override fun onDestroy() {
        audioFeedbackEngine.close()
        super.onDestroy()
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
    detectionsCount: Int,
    hazardLevel: String,
    onStart: () -> Unit,
    onStop: () -> Unit,
    cameraFrameSource: CameraFrameSource,
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(text = "Hazard: $hazardLevel", color = Color.White)
        Text(text = "Detections: $detectionsCount", color = Color.White)
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
            onStart = {},
            onStop = {}
        )
    }
}
