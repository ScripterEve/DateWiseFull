package com.example.datewisepos.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onProductFound: (Long) -> Unit,
    onNavigateToAdd: (String) -> Unit,
    onManualEntry: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Navigate when product already exists
    LaunchedEffect(uiState.existingProduct) {
        uiState.existingProduct?.let { product ->
            onProductFound(product.id)
            viewModel.resetState()
        }
    }

    // Note: savedProductId navigation is handled by AddProductScreen

    // Navigate to add screen when barcode found but product not in DB
    LaunchedEffect(uiState.lookupState) {
        if (uiState.lookupState == LookupState.Found ||
            uiState.lookupState == LookupState.NotFound ||
            uiState.lookupState == LookupState.Error
        ) {
            val barcode = uiState.scannedBarcode
            // Don't reset state here — the shared ViewModel should preserve
            // lookup data so AddProductScreen can display it
            onNavigateToAdd(barcode)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            CameraPreviewWithScanner(
                onBarcodeDetected = { barcode ->
                    if (uiState.lookupState == LookupState.Idle) {
                        viewModel.onBarcodeScanned(barcode)
                    }
                }
            )

            // Scanning overlay
            ScanOverlay()

            // Status indicator
            AnimatedVisibility(
                visible = uiState.lookupState == LookupState.Loading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Black.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Looking up product...",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        } else {
            // No camera permission state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Camera permission required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Grant Permission")
                }
            }
        }

        // Manual entry FAB
        FloatingActionButton(
            onClick = {
                viewModel.resetState()
                onManualEntry()
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Manual Entry")
        }

        // Bottom instruction text
        Text(
            text = "Point camera at product barcode",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .background(
                    Color.Black.copy(alpha = 0.5f),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 24.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun ScanOverlay() {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
    ) {
        val scanBoxWidth = size.width * 0.7f
        val scanBoxHeight = scanBoxWidth * 0.5f
        val left = (size.width - scanBoxWidth) / 2
        val top = (size.height - scanBoxHeight) / 2

        // Semi-transparent background
        drawRect(color = Color.Black.copy(alpha = 0.4f))

        // Clear the scan area
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(scanBoxWidth, scanBoxHeight),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = BlendMode.Clear
        )

        // Scan box border
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(scanBoxWidth, scanBoxHeight),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 3f)
        )

        // Corner accents
        val cornerLen = 40f
        val accentColor = Color(0xFF4CAF50)
        val strokeWidth = 6f

        // Top-left
        drawLine(accentColor, Offset(left, top + cornerLen), Offset(left, top), strokeWidth)
        drawLine(accentColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth)
        // Top-right
        drawLine(accentColor, Offset(left + scanBoxWidth - cornerLen, top), Offset(left + scanBoxWidth, top), strokeWidth)
        drawLine(accentColor, Offset(left + scanBoxWidth, top), Offset(left + scanBoxWidth, top + cornerLen), strokeWidth)
        // Bottom-left
        drawLine(accentColor, Offset(left, top + scanBoxHeight - cornerLen), Offset(left, top + scanBoxHeight), strokeWidth)
        drawLine(accentColor, Offset(left, top + scanBoxHeight), Offset(left + cornerLen, top + scanBoxHeight), strokeWidth)
        // Bottom-right
        drawLine(accentColor, Offset(left + scanBoxWidth - cornerLen, top + scanBoxHeight), Offset(left + scanBoxWidth, top + scanBoxHeight), strokeWidth)
        drawLine(accentColor, Offset(left + scanBoxWidth, top + scanBoxHeight - cornerLen), Offset(left + scanBoxWidth, top + scanBoxHeight), strokeWidth)
    }
}

@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
fun CameraPreviewWithScanner(
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember { BarcodeScanning.getClient() }
    var lastDetectedTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).also { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastDetectedTime < 2000) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val rawValue = barcode.rawValue
                                                if (rawValue != null &&
                                                    (barcode.format == Barcode.FORMAT_EAN_13 ||
                                                     barcode.format == Barcode.FORMAT_EAN_8 ||
                                                     barcode.format == Barcode.FORMAT_UPC_A ||
                                                     barcode.format == Barcode.FORMAT_UPC_E ||
                                                     barcode.format == Barcode.FORMAT_CODE_128 ||
                                                     barcode.format == Barcode.FORMAT_CODE_39)
                                                ) {
                                                    lastDetectedTime = currentTime
                                                    onBarcodeDetected(rawValue)
                                                    break
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        Log.e("ScanScreen", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
