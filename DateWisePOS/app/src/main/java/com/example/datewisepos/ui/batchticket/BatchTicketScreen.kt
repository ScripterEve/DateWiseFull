package com.example.datewisepos.ui.batchticket

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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
fun BatchTicketScreen(
    viewModel: BatchTicketViewModel
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

    // Snackbar for print result
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.printResult) {
        when (val result = uiState.printResult) {
            is BatchPrintResult.Success -> {
                snackbarHostState.showSnackbar("Batch ticket printed successfully!")
                viewModel.clearPrintResult()
            }
            is BatchPrintResult.Error -> {
                snackbarHostState.showSnackbar("Print error: ${result.message}")
                viewModel.clearPrintResult()
            }
            null -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->

        if (uiState.showPreview && uiState.combinedTicketBitmap != null) {
            // ── PREVIEW MODE ──
            BatchTicketPreview(
                uiState = uiState,
                onPrint = { viewModel.printTicket() },
                onBack = { viewModel.dismissPreview() },
                modifier = Modifier.padding(padding)
            )
        } else {
            // ── SCANNER MODE ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Camera area (top half)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    if (hasCameraPermission) {
                        BatchCameraScanner(
                            onCodeDetected = { rawValue, format ->
                                viewModel.onCodeScanned(rawValue, format)
                            }
                        )
                        BatchScanOverlay()

                        // Scan status indicator
                        val statusText = when {
                            uiState.lastScanInfo != null -> uiState.lastScanInfo!!
                            uiState.pendingBarcode != null ->
                                "Barcode: ${uiState.pendingBarcode}\nNow scan the DataMatrix…"
                            else -> "Scan a ticket's DataMatrix to add it"
                        }

                        Text(
                            text = statusText,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 16.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        )

                        // Items count badge (top right)
                        if (uiState.scannedItems.isNotEmpty()) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                Text(
                                    "${uiState.scannedItems.size}",
                                    modifier = Modifier.padding(4.dp),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
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
                            Button(onClick = {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }

                // Scanned items list + Generate button (bottom half)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Scanned Items (${uiState.scannedItems.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.scannedItems.isNotEmpty()) {
                            TextButton(onClick = { viewModel.clearAll() }) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "Clear all",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear")
                            }
                        }
                    }

                    HorizontalDivider()

                    if (uiState.scannedItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No items scanned yet.\nScan barcode + DataMatrix pairs.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(uiState.scannedItems) { index, item ->
                                ScannedItemCard(
                                    index = index,
                                    item = item,
                                    onDelete = { viewModel.removeItem(index) }
                                )
                            }
                        }
                    }

                    // Generate button
                    Button(
                        onClick = { viewModel.generateCombinedTicket() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(52.dp),
                        enabled = uiState.scannedItems.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Generate Ticket",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScannedItemCard(
    index: Int,
    item: ScannedTicketItem,
    onDelete: () -> Unit
) {
    val formattedExpiry = if (item.expiryDate.length == 8) {
        "${item.expiryDate.substring(0, 2)}-${item.expiryDate.substring(2, 4)}-${item.expiryDate.substring(4, 8)}"
    } else {
        item.expiryDate
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${index + 1}.",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                if (item.name.isNotEmpty()) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (item.brand.isNotEmpty()) {
                    Text(
                        item.brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    item.barcode + if (item.quantity.isNotEmpty()) " • ${item.quantity}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "Expires: $formattedExpiry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BatchTicketPreview(
    uiState: BatchTicketUiState,
    onPrint: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Batch Ticket Preview") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable content area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Text(
                    "${uiState.scannedItems.size} items combined",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                // Ticket preview
                uiState.combinedTicketBitmap?.let { bitmap ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(8.dp))
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Batch ticket preview",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }

            // Print button — always visible at bottom
            Button(
                onClick = onPrint,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(56.dp),
                enabled = !uiState.isPrinting && uiState.combinedTicketBitmap != null,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isPrinting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Printing...")
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Print Batch Ticket",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
fun BatchScanOverlay() {
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

        drawRect(color = Color.Black.copy(alpha = 0.4f))

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(left, top),
            size = Size(scanBoxWidth, scanBoxHeight),
            cornerRadius = CornerRadius(16f, 16f),
            blendMode = BlendMode.Clear
        )

        drawRoundRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(scanBoxWidth, scanBoxHeight),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 3f)
        )

        // Corner accents (orange for batch mode)
        val cornerLen = 40f
        val accentColor = Color(0xFFFF9800)
        val strokeWidth = 6f

        drawLine(accentColor, Offset(left, top + cornerLen), Offset(left, top), strokeWidth)
        drawLine(accentColor, Offset(left, top), Offset(left + cornerLen, top), strokeWidth)
        drawLine(accentColor, Offset(left + scanBoxWidth - cornerLen, top), Offset(left + scanBoxWidth, top), strokeWidth)
        drawLine(accentColor, Offset(left + scanBoxWidth, top), Offset(left + scanBoxWidth, top + cornerLen), strokeWidth)
        drawLine(accentColor, Offset(left, top + scanBoxHeight - cornerLen), Offset(left, top + scanBoxHeight), strokeWidth)
        drawLine(accentColor, Offset(left, top + scanBoxHeight), Offset(left + cornerLen, top + scanBoxHeight), strokeWidth)
        drawLine(accentColor, Offset(left + scanBoxWidth - cornerLen, top + scanBoxHeight), Offset(left + scanBoxWidth, top + scanBoxHeight), strokeWidth)
        drawLine(accentColor, Offset(left + scanBoxWidth, top + scanBoxHeight - cornerLen), Offset(left + scanBoxWidth, top + scanBoxHeight), strokeWidth)
    }
}

@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
fun BatchCameraScanner(
    onCodeDetected: (String, Int) -> Unit
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
                                if (currentTime - lastDetectedTime < 500) {
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
                                                if (rawValue != null) {
                                                    when (barcode.format) {
                                                        Barcode.FORMAT_EAN_13,
                                                        Barcode.FORMAT_EAN_8,
                                                        Barcode.FORMAT_UPC_A,
                                                        Barcode.FORMAT_UPC_E,
                                                        Barcode.FORMAT_CODE_128,
                                                        Barcode.FORMAT_CODE_39,
                                                        Barcode.FORMAT_DATA_MATRIX -> {
                                                            lastDetectedTime = currentTime
                                                            onCodeDetected(rawValue, barcode.format)
                                                            return@addOnSuccessListener
                                                        }
                                                    }
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
                        Log.e("BatchScan", "Camera binding failed", e)
                    }
                }, ContextCompat.getMainExecutor(ctx))
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
