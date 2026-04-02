package com.example.datewisepos.ui.ticket

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.datewisepos.util.BarcodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketPreviewScreen(
    productId: Long,
    expiryDateMillis: Long,
    viewModel: TicketViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(productId, expiryDateMillis) {
        viewModel.generateTicket(productId, expiryDateMillis)
    }

    // Show snackbar for print result
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.printResult) {
        when (val result = uiState.printResult) {
            is PrintResult.Success -> {
                snackbarHostState.showSnackbar("Ticket printed successfully!")
                viewModel.clearPrintResult()
            }
            is PrintResult.Error -> {
                snackbarHostState.showSnackbar("Print error: ${result.message}")
                viewModel.clearPrintResult()
            }
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ticket Preview") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (uiState.isGenerating) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Generating ticket...")
                    }
                }
            } else if (uiState.errorMessage != null) {
                val errorMsg = uiState.errorMessage!!
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "⚠️",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            errorMsg,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        OutlinedButton(onClick = onBack) {
                            Text("Go Back")
                        }
                    }
                }
            } else {
                // Scrollable content
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Product info
                    uiState.product?.let { product ->
                        Text(
                            product.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Expires: ${BarcodeGenerator.formatExpiryDate(expiryDateMillis)}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Ticket preview
                    uiState.ticketBitmap?.let { bitmap ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(8.dp))
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Ticket preview",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    }
                }

                // Print button — always visible at bottom
                Button(
                    onClick = { viewModel.printTicket() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .height(56.dp),
                    enabled = !uiState.isPrinting && uiState.ticketBitmap != null,
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
                            "Print Ticket",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}
