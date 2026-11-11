package com.presenterremote.ui.screens

import android.Manifest
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.presenterremote.MainViewModel
import com.presenterremote.network.ConnectionStatus

@Composable
fun ConnectionScreen(
    viewModel: MainViewModel,
    connectionStatus: ConnectionStatus
) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("8765") }
    var pin by remember { mutableStateOf("") }
    var showScanner by remember { mutableStateOf(false) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (granted) showScanner = true
    }

    LaunchedEffect(Unit) {
        ip = viewModel.getLastIp()
        port = viewModel.getLastPort()
        pin = viewModel.getLastPin()
    }

    if (showScanner && cameraPermissionGranted) {
        QrScannerView(
            onScanned = { url ->
                showScanner = false
                parseWsUrl(url)?.let { (h, p, pn) ->
                    ip = h
                    port = p
                    pin = pn
                }
            },
            onCancel = { showScanner = false }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Presenter Remote", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = ip,
            onValueChange = { ip = it },
            label = { Text("IP Address") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it },
                label = { Text("PIN") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    val p = port.toIntOrNull() ?: 8765
                    viewModel.connect(ip.trim(), p, pin.trim())
                },
                enabled = ip.isNotBlank() && pin.isNotBlank() &&
                    connectionStatus != ConnectionStatus.CONNECTING,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    when (connectionStatus) {
                        ConnectionStatus.CONNECTING -> "Connecting..."
                        else -> "Connect"
                    }
                )
            }
            OutlinedButton(
                onClick = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
            }
        }

        if (connectionStatus == ConnectionStatus.AUTH_FAILED) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("Wrong PIN", color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun parseWsUrl(url: String): Triple<String, String, String>? {
    val regex = Regex("""ws://([^:]+):(\d+)\?pin=(\d+)""")
    val match = regex.find(url) ?: return null
    return Triple(match.groupValues[1], match.groupValues[2], match.groupValues[3])
}

@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun QrScannerView(onScanned: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val scanner = BarcodeScanning.getClient()
                    val analysis = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                    analysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(
                                mediaImage, imageProxy.imageInfo.rotationDegrees
                            )
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        if (barcode.valueType == Barcode.TYPE_TEXT ||
                                            barcode.valueType == Barcode.TYPE_URL
                                        ) {
                                            barcode.rawValue?.let { value ->
                                                if (value.startsWith("ws://")) {
                                                    cameraProvider.unbindAll()
                                                    onScanned(value)
                                                }
                                            }
                                        }
                                    }
                                }
                                .addOnCompleteListener { imageProxy.close() }
                        } else {
                            imageProxy.close()
                        }
                    }
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        ctx as androidx.lifecycle.LifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, analysis
                    )
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
        Button(
            onClick = onCancel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Cancel")
        }
    }
}
