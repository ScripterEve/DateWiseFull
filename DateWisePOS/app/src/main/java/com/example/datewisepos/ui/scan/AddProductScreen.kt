package com.example.datewisepos.ui.scan

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: ScanViewModel,
    initialBarcode: String = "",
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // If we got a barcode from navigation, trigger lookup
    LaunchedEffect(initialBarcode) {
        if (initialBarcode.isNotBlank()) {
            viewModel.initWithBarcode(initialBarcode)
        }
    }

    // Navigate back when product is saved
    LaunchedEffect(uiState.savedProductId) {
        uiState.savedProductId?.let {
            viewModel.resetState()
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Product") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetState()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Barcode entry field with lookup button
            OutlinedTextField(
                value = uiState.scannedBarcode,
                onValueChange = { viewModel.updateBarcode(it) },
                label = { Text("Barcode *") },
                placeholder = { Text("Enter or scan barcode") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Search
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.lookupBarcode() },
                        enabled = uiState.scannedBarcode.isNotBlank() &&
                                uiState.lookupState != LookupState.Loading
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Lookup barcode")
                    }
                }
            )

            // Status chip
            when (uiState.lookupState) {
                LookupState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Text(
                            "Looking up product...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                LookupState.Found -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Product found online — verify details") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    )
                }
                LookupState.NotFound -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Product not found online — enter manually") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }
                LookupState.Error -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Lookup failed — enter manually") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    )
                }
                LookupState.AlreadyExists -> {
                    AssistChip(
                        onClick = {},
                        label = { Text("Product already in inventory") },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                else -> {}
            }

            // Product image
            uiState.productImageUrl?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Product image",
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .align(Alignment.CenterHorizontally),
                    contentScale = ContentScale.Crop
                )
            }

            // Name field
            OutlinedTextField(
                value = uiState.productName,
                onValueChange = { viewModel.updateName(it) },
                label = { Text("Product Name *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Brand field
            OutlinedTextField(
                value = uiState.productBrand,
                onValueChange = { viewModel.updateBrand(it) },
                label = { Text("Brand") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )

            // Quantity field
            OutlinedTextField(
                value = uiState.productQuantity,
                onValueChange = { viewModel.updateQuantity(it) },
                label = { Text("Quantity (e.g. 500g)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            Button(
                onClick = { viewModel.saveProduct() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = uiState.productName.isNotBlank() &&
                        uiState.scannedBarcode.isNotBlank() &&
                        !uiState.isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Product", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
