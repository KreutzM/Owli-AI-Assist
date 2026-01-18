package com.owlitech.owli.assist.ui.screens

import android.content.ClipData
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.owlitech.owli.assist.R
import com.owlitech.owli.assist.diagnostics.DiagnosticsCollector
import com.owlitech.owli.assist.diagnostics.DiagnosticsReportBuilder
import com.owlitech.owli.assist.settings.AppSettings
import com.owlitech.owli.assist.ui.components.SectionCard
import kotlinx.coroutines.launch

@Composable
fun DiagnosticsScreen(
    settings: AppSettings
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    val diagState by DiagnosticsCollector.state.collectAsState()
    val reportLabel = stringResource(R.string.diagnostics_report_label)
    val reportCopiedText = stringResource(R.string.diagnostics_report_copied)
    val missingPreview = stringResource(R.string.diagnostics_last_preview_missing)
    val motionLevelText = diagState.motionLevel?.name ?: missingPreview
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
                    ClipData.newPlainText(reportLabel, report).toClipEntry()
                )
            }
            Toast.makeText(context, reportCopiedText, Toast.LENGTH_SHORT).show()
        }) { Text(stringResource(R.string.diagnostics_copy)) }

        SectionCard(title = stringResource(R.string.diagnostics_section_app)) {
            Text(
                stringResource(
                    R.string.diagnostics_version_format,
                    diagState.versionName,
                    diagState.versionCode,
                    diagState.buildType
                )
            )
            Text(
                stringResource(
                    R.string.diagnostics_device_format,
                    diagState.deviceModel,
                    diagState.androidVersion
                )
            )
        }
        SectionCard(title = stringResource(R.string.diagnostics_section_pipeline)) {
            Text(
                stringResource(
                    R.string.diagnostics_running_format,
                    diagState.isRunning,
                    diagState.fps,
                    diagState.frameIntervalMs
                )
            )
            Text(stringResource(R.string.diagnostics_detector_info_format, diagState.detectorInfo))
            Text(
                stringResource(
                    R.string.diagnostics_analysis_interval_format,
                    diagState.analysisIntervalMs
                )
            )
        }
        SectionCard(title = stringResource(R.string.diagnostics_section_detector)) {
            Text(
                stringResource(
                    R.string.diagnostics_threads_format,
                    diagState.detectorNumThreads,
                    diagState.detectorScoreThreshold,
                    diagState.detectorMaxResults
                )
            )
        }
        SectionCard(title = stringResource(R.string.diagnostics_section_blindview_tts)) {
            Text(
                stringResource(
                    R.string.diagnostics_tts_ready_format,
                    diagState.ttsReady,
                    diagState.ttsSpeechRate
                )
            )
            Text(
                stringResource(
                    R.string.diagnostics_speak_interval_format,
                    diagState.minSpeakIntervalMs,
                    diagState.repeatSamePlanIntervalMs,
                    diagState.maxItemsSpoken
                )
            )
        }
        SectionCard(title = stringResource(R.string.diagnostics_section_tracking)) {
            Text(
                stringResource(
                    R.string.diagnostics_tracking_format,
                    diagState.iouThreshold,
                    diagState.trackMaxAgeMs,
                    diagState.minConsecutiveHits
                )
            )
            Text(
                stringResource(
                    R.string.diagnostics_overlay_format,
                    diagState.showOverlay,
                    diagState.showOverlayLabels,
                    diagState.showBlindViewPreview
                )
            )
        }
        SectionCard(title = stringResource(R.string.diagnostics_section_motion)) {
            Text(
                stringResource(
                    R.string.diagnostics_motion_format,
                    motionLevelText,
                    diagState.gyroMagRadS,
                    diagState.rollDeg,
                    diagState.pitchDeg,
                    diagState.motionQuality
                )
            )
            Text(
                stringResource(
                    R.string.diagnostics_stabilization_format,
                    diagState.stabilizationEnabled,
                    diagState.appliedRollDeg,
                    diagState.mappingActive
                )
            )
        }
        SectionCard(title = stringResource(R.string.diagnostics_section_scene_snapshot)) {
            Text(
                stringResource(
                    R.string.diagnostics_detections_format,
                    diagState.detectionsCountRaw,
                    diagState.detectionsCountStable
                )
            )
            Text(
                stringResource(
                    R.string.diagnostics_top_labels_format,
                    diagState.topLabels.joinToString()
                )
            )
            Text(
                stringResource(
                    R.string.diagnostics_last_preview_format,
                    diagState.lastUtterancePreview ?: missingPreview
                )
            )
        }
    }
}
