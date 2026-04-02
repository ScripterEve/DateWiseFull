package com.example.datewise.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.datewise.ui.theme.DateWiseGreen
import com.example.datewise.ui.theme.DateWiseGreenAccent
import com.example.datewise.ui.theme.DateWiseGreenLight
import com.example.datewise.ui.viewmodels.BarcodeLookupState
import com.example.datewise.ui.viewmodels.SharedViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

// Data class for parsed receipt Data Matrix or QR Code (pipe-delimited: Name|Brand|barcode|quantity|ExpiryDate)
data class ReceiptData(
    val name: String,
    val brand: String,
    val barcode: String,
    val quantity: String,
    val expiryDate: String
) {
    val description: String
        get() = if (brand.isNotEmpty() && quantity.isNotEmpty()) {
            "$brand • $quantity"
        } else brand.ifEmpty { quantity }
}

/**
 * Try to parse a pipe-delimited receipt Data Matrix or QR Code.
 * It can be a single item or multiple items separated by semicolons.
 * Returns null if the data doesn't match this format.
 */
fun parseReceiptDataMatrix(raw: String): List<ReceiptData>? {
    val items = raw.trim().split(";")
    val parsedItems = mutableListOf<ReceiptData>()
    
    for (item in items) {
        if (item.isBlank()) continue
        val parts = item.split("|")
        if (parts.size >= 5) {
            parsedItems.add(
                ReceiptData(
                    name = parts[0].trim(),
                    brand = parts[1].trim(),
                    barcode = parts[2].trim(),
                    quantity = parts[3].trim(),
                    expiryDate = parts[4].trim()
                )
            )
        }
    }
    
    return if (parsedItems.isNotEmpty()) parsedItems else null
}

/**
 * Clean a barcode string by stripping common GS1 AI prefixes like (97), (01), (02).
 */
private fun cleanBarcode(raw: String): String {
    // Strip parenthesized AI prefixes like (97), (01), (02)
    val paren = Regex("^\\(\\d{2,4}\\)").find(raw)
    if (paren != null) return raw.substring(paren.range.last + 1)
    return raw
}

private fun extractGS1Expiry(raw: String): String? {
    if (raw.length == 8 && raw.all { it.isDigit() }) {
        return raw // specifically handle pure custom DDMMYYYY or YYYYMMDD without mistakenly parsing as AI 17
    }

    var cleanRaw = raw
    if (cleanRaw.startsWith("]d2", ignoreCase = true) || cleanRaw.startsWith("]C1", ignoreCase = true)) {
        cleanRaw = cleanRaw.substring(3)
    }

    var i = 0
    while (i < cleanRaw.length) {
        val c = cleanRaw[i]
        if (c == '\u001D' || c == '\u00E8') {
            i++
            continue
        }
        if (i + 2 > cleanRaw.length) break
        val ai = cleanRaw.substring(i, i + 2)
        i += 2
        when (ai) {
            "01" -> i += 14 // GTIN
            "17" -> { // Expiry Date requires 6 characters YYMMDD
                if (i + 6 <= cleanRaw.length) return cleanRaw.substring(i, i + 6) else break
            }
            "11", "13", "15" -> i += 6 // Other fixed Dates
            "10", "21" -> { // Batch/Lot or Serial
                val nextFnc = cleanRaw.indexOf('\u001D', i)
                i = if (nextFnc != -1) nextFnc + 1 else cleanRaw.length
            }
            "31", "32", "33", "34", "35", "36" -> i += 8 // weight AIs usually 310n + 6 digits
            else -> {
                val nextFnc = cleanRaw.indexOf('\u001D', i)
                if (nextFnc != -1) i = nextFnc + 1 else break
            }
        }
    }
    return null
}

@Composable
fun ScanScreen(
    viewModel: SharedViewModel,
    onEnterManually: () -> Unit = {},
    onNavigateToAdd: (barcode: String, name: String, description: String, expiryDate: String?) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    if (hasCameraPermission) {
        ScannerView(viewModel, onEnterManually, onNavigateToAdd)
    } else {
        // Permission request screen
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF121212)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint = DateWiseGreenLight,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Camera permission required",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Grant camera access to scan barcodes",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DateWiseGreen
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Permission", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onEnterManually,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2C2C2C)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enter code manually")
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun ScannerView(
    viewModel: SharedViewModel,
    onEnterManually: () -> Unit,
    onNavigateToAdd: (barcode: String, name: String, description: String, expiryDate: String?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }
    var detectedBarcode by remember { mutableStateOf<Barcode?>(null) }
    var detectedExpiryDate by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val barcodeLookupState by viewModel.barcodeLookupState.collectAsState()

    // React to barcode lookup results
    LaunchedEffect(barcodeLookupState) {
        when (val state = barcodeLookupState) {
            is BarcodeLookupState.Found -> {
                val info = state.productInfo
                onNavigateToAdd(info.barcode, info.name, info.description, detectedExpiryDate)
                viewModel.resetBarcodeLookup()
                isProcessing = false
            }
            is BarcodeLookupState.NotFound -> {
                // Barcode not found in API — navigate with just the barcode
                onNavigateToAdd(state.barcode, "", "", detectedExpiryDate)
                viewModel.resetBarcodeLookup()
                isProcessing = false
            }
            else -> {}
        }
    }

    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()
    val scanner = BarcodeScanning.getClient(options)

    cameraController.setImageAnalysisAnalyzer(
        ContextCompat.getMainExecutor(context),
    ) { imageProxy ->
        if (!isProcessing) {
            imageProxy.image?.let { image ->
                val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty() && !isProcessing) {
                            barcodes.forEach { barcode ->
                                if (barcode.format == Barcode.FORMAT_DATA_MATRIX || barcode.format == Barcode.FORMAT_QR_CODE) {
                                    val raw = barcode.displayValue ?: barcode.rawValue ?: ""
                                    // First try pipe-delimited receipt format: Name|Brand|barcode|quantity|ExpiryDate
                                    val receipts = parseReceiptDataMatrix(raw)
                                    if (receipts != null && receipts.isNotEmpty()) {
                                        // All data available from Data Matrix/QR Code — navigate directly
                                        isProcessing = true
                                        val firstItem = receipts.first()
                                        if (receipts.size > 1) {
                                            viewModel.setPendingBatchItems(receipts.drop(1))
                                        }
                                        onNavigateToAdd(
                                            firstItem.barcode,
                                            firstItem.name,
                                            firstItem.description,
                                            firstItem.expiryDate
                                        )
                                        return@addOnSuccessListener
                                    }
                                    // Fallback: try GS1 expiry extraction
                                    detectedExpiryDate = extractGS1Expiry(raw) ?: raw
                                } else {
                                    detectedBarcode = barcode
                                }
                            }
                            
                            // Automatically lookup if both are now detected (fallback for non-receipt barcodes)
                            if (detectedBarcode != null && detectedExpiryDate != null) {
                                val rawCode = detectedBarcode?.displayValue ?: detectedBarcode?.rawValue
                                val code = rawCode?.let { cleanBarcode(it) }
                                if (code != null && !isProcessing) {
                                    isProcessing = true
                                    viewModel.lookupBarcode(code)
                                }
                            }
                        }
                    }
                    .addOnFailureListener { it.printStackTrace() }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            }
        } else {
            imageProxy.close()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(modifier = Modifier.weight(1f)) {
            // Camera preview
            AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PreviewView(it).apply {
                    controller = cameraController
                    cameraController.bindToLifecycle(lifecycleOwner)
                }
            }
        )

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2C).copy(alpha = 0.8f))
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
        }

        // Scanning frame + loading indicator
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .border(3.dp, DateWiseGreenAccent, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (barcodeLookupState is BarcodeLookupState.Loading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = DateWiseGreenAccent,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Looking up product…",
                            color = DateWiseGreenAccent,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else if (detectedBarcode != null || detectedExpiryDate != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (detectedBarcode != null) {
                            Text(
                                text = "Barcode: ${detectedBarcode?.displayValue ?: ""}",
                                color = DateWiseGreenAccent,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                        if (detectedExpiryDate != null) {
                            Text(
                                text = "Expiry: $detectedExpiryDate",
                                color = DateWiseGreenAccent,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Bottom section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF1A1A1A))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Scan Item",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Position the barcode inside the frame",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Enter code manually button
            Button(
                onClick = onEnterManually,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2C2C2C)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Filled.Keyboard,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Enter code manually",
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom icons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C2C))
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "Gallery", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Gallery", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }

                // Scan button — triggers barcode lookup
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(DateWiseGreen)
                            .clickable {
                                detectedBarcode?.let { barcode ->
                                    val rawCode = barcode.displayValue ?: barcode.rawValue ?: return@clickable
                                    val code = cleanBarcode(rawCode)
                                    isProcessing = true
                                    viewModel.lookupBarcode(code)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.QrCodeScanner,
                            contentDescription = "Scan",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Scan", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }

                // History
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2C2C2C))
                    ) {
                        Icon(Icons.Filled.History, contentDescription = "History", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("History", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
        }
    }
}
}
