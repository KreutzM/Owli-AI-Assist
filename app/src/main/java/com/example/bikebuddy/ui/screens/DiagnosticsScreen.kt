package com.example.bikebuddy.ui.screens

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.unit.dp
import com.example.bikeassist.diagnostics.DiagnosticsCollector
import com.example.bikeassist.diagnostics.DiagnosticsReportBuilder
import com.example.bikeassist.settings.AppSettings
import com.example.bikebuddy.ui.components.SectionCard
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(
    settings: AppSettings
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val diagState by DiagnosticsCollector.state.collectAsState()
    Column(
        modifier = Modifier
            .padding(12.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = {
            val report = DiagnosticsReportBuilder.build(diagState, settings)
            coroutineScope.launch {
                clipboard.setClipEntry(
                    ClipData.newPlainText("Debug Report", report).toClipEntry()
                )
            }
            Toast.makeText(context, "Debug Report kopiert", Toast.LENGTH_SHORT).show()
        }) { Text("Copy") }

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
