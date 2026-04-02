package com.example.datewisepos.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.datewisepos.data.local.ExpiryRecord
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    viewModel: ProductDetailViewModel,
    onBack: () -> Unit,
    onGenerateTicket: (Long, Long) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.product?.name ?: "Product") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete product")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showDatePicker = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Expiry Date") },
                containerColor = MaterialTheme.colorScheme.primary
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (state.product != null) {
            val product = state.product!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Product info card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            product.imageUrl?.let { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = product.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            Text(
                                product.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (product.brand.isNotBlank()) {
                                Text(
                                    product.brand,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Text(
                                    "Barcode: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    product.barcode,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (product.quantity.isNotBlank()) {
                                Row {
                                    Text(
                                        "Quantity: ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        product.quantity,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Expiry records header
                item {
                    Text(
                        "Expiry Dates",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (state.expiryRecords.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "No expiry dates added yet.\nTap \"Add Expiry Date\" to create a ticket.",
                                modifier = Modifier.padding(20.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                items(state.expiryRecords, key = { it.id }) { record ->
                    ExpiryRecordCard(
                        record = record,
                        dateFormat = dateFormat,
                        onPrint = { onGenerateTicket(productId, record.expiryDate) },
                        onDelete = { viewModel.deleteExpiryRecord(record.id) }
                    )
                }

                // Bottom spacer for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            viewModel.addExpiryRecord(productId, millis)
                            // Auto-navigate to ticket after adding
                            onGenerateTicket(productId, millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Generate Ticket")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Product?") },
            text = { Text("This will remove the product and all its expiry records.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProduct(productId)
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExpiryRecordCard(
    record: ExpiryRecord,
    dateFormat: SimpleDateFormat,
    onPrint: () -> Unit,
    onDelete: () -> Unit
) {
    val now = System.currentTimeMillis()
    val daysUntil = ((record.expiryDate - now) / (1000 * 60 * 60 * 24)).toInt()
    val statusColor = when {
        daysUntil < 0 -> MaterialTheme.colorScheme.error
        daysUntil <= 30 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val statusText = when {
        daysUntil < 0 -> "Expired"
        daysUntil == 0 -> "Expires today"
        daysUntil <= 30 -> "Expires in $daysUntil days"
        else -> "Expires in $daysUntil days"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    dateFormat.format(Date(record.expiryDate)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = statusColor
                )
            }

            IconButton(onClick = onPrint) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Print ticket",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete record",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
