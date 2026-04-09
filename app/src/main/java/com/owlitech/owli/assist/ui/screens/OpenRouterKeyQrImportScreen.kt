package com.owlitech.owli.assist.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.common.InputImage
import com.owlitech.owli.assist.R
import com.owlitech.owli.assist.settings.OpenRouterKeyQrPayloadParser
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun OpenRouterKeyQrImportScreen(
    onConfirmKey: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scannedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var scanError by rememberSaveable { mutableStateOf<String?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.openrouter_qr_import_title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.openrouter_qr_import_helper), style = MaterialTheme.typography.bodySmall)

        when {
            scannedKey != null -> ConfirmScannedKey(
                onUse = {
                    scannedKey?.let(onConfirmKey)
                    scannedKey = null
                },
                onScanAgain = {
                    scannedKey = null
                    scanError = null
                },
                onCancel = {
                    scannedKey = null
                    onCancel()
                }
            )

            !hasCameraPermission -> CameraPermissionPrompt(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onCancel = onCancel
            )

            else -> {
                QrScannerCamera(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp),
                    onQrPayload = { payload ->
                        val key = OpenRouterKeyQrPayloadParser.extractKey(payload)
                        if (key != null) {
                            scannedKey = key
                            scanError = null
                            true
                        } else {
                            scanError = context.getString(R.string.openrouter_qr_import_invalid)
                            false
                        }
                    }
                )
                scanError?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.openrouter_qr_import_cancel))
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionPrompt(
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.openrouter_qr_import_permission))
        Button(onClick = onRequestPermission) {
            Text(stringResource(R.string.openrouter_qr_import_request_permission))
        }
        OutlinedButton(onClick = onCancel) {
            Text(stringResource(R.string.openrouter_qr_import_cancel))
        }
    }
}

@Composable
private fun ConfirmScannedKey(
    onUse: () -> Unit,
    onScanAgain: () -> Unit,
    onCancel: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.openrouter_qr_import_found), style = MaterialTheme.typography.bodyMedium)
        Text(stringResource(R.string.openrouter_qr_import_found_helper), style = MaterialTheme.typography.bodySmall)
        Button(onClick = onUse) {
            Text(stringResource(R.string.openrouter_qr_import_use_key))
        }
        OutlinedButton(onClick = onScanAgain) {
            Text(stringResource(R.string.openrouter_qr_import_scan_again))
        }
        OutlinedButton(onClick = onCancel) {
            Text(stringResource(R.string.openrouter_qr_import_cancel))
        }
    }
}

@Composable
private fun QrScannerCamera(
    modifier: Modifier = Modifier,
    onQrPayload: (String) -> Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    val acceptedScan = remember { AtomicBoolean(false) }

    DisposableEffect(lifecycleOwner) {
        var provider: ProcessCameraProvider? = null
        var isBound = false

        fun bindIfNeeded() {
            if (isBound) return
            val cameraProvider = provider ?: return
            val rotation = previewView.display?.rotation ?: Surface.ROTATION_0
            val preview = Preview.Builder().build().also {
                it.targetRotation = rotation
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(rotation)
                .build()
                .also {
                    it.setAnalyzer(analysisExecutor) { imageProxy ->
                        scanQrImage(scanner, mainExecutor, acceptedScan, imageProxy, onQrPayload)
                    }
                }
            runCatching { cameraProvider.unbindAll() }
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                analysis
            )
            isBound = true
        }

        fun unbindIfNeeded() {
            if (!isBound) return
            runCatching { provider?.unbindAll() }
            isBound = false
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> bindIfNeeded()
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> unbindIfNeeded()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        val listener = Runnable {
            provider = cameraProviderFuture.get()
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                bindIfNeeded()
            }
        }
        cameraProviderFuture.addListener(listener, mainExecutor)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            unbindIfNeeded()
            scanner.close()
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )
        Text(
            text = stringResource(R.string.openrouter_qr_import_scanning),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun scanQrImage(
    scanner: BarcodeScanner,
    callbackExecutor: Executor,
    acceptedScan: AtomicBoolean,
    imageProxy: ImageProxy,
    onQrPayload: (String) -> Boolean
) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener(callbackExecutor) { barcodes ->
            val rawValue = barcodes.firstNotNullOfOrNull { it.rawValue?.takeIf(String::isNotBlank) }
            if (rawValue != null && !acceptedScan.get()) {
                val accepted = onQrPayload(rawValue)
                if (accepted) {
                    acceptedScan.set(true)
                }
            }
        }
        .addOnCompleteListener(callbackExecutor) {
            imageProxy.close()
        }
}
