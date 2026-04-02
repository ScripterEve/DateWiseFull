package com.example.datewisepos.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    onProductClick: (Long) -> Unit,
    onScanClick: () -> Unit
) {
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Inventory",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onScanClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Scan product")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearch(it) },
                placeholder = { Text("Search by name or barcode") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (products.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No products yet",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Tap + to scan a product barcode",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(products, key = { it.product.id }) { item ->
                        ProductCard(
                            item = item,
                            onClick = { onProductClick(item.product.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    item: ProductWithExpiry,
    onClick: () -> Unit
) {
    val product = item.product
    val expiryDate = item.latestExpiry?.expiryDate
    val dateFormat = remember { SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) }

    val now = System.currentTimeMillis()
    val daysUntilExpiry = expiryDate?.let {
        ((it - now) / (1000 * 60 * 60 * 24)).toInt()
    }

    val expiryColor = when {
        daysUntilExpiry == null -> Color.Gray
        daysUntilExpiry < 0 -> Color(0xFFE53935)     // Red - expired
        daysUntilExpiry <= 30 -> Color(0xFFFF9800)    // Orange - expiring soon
        else -> Color(0xFF4CAF50)                      // Green - OK
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product image or placeholder
            if (product.imageUrl != null) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        product.name.take(2).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Product details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (product.brand.isNotBlank()) {
                    Text(
                        product.brand,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    product.barcode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expiry indicator
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(expiryColor)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (expiryDate != null) {
                    Text(
                        dateFormat.format(Date(expiryDate)),
                        style = MaterialTheme.typography.labelSmall,
                        color = expiryColor,
                        fontWeight = FontWeight.Medium
                    )
                } else {
                    Text(
                        "No expiry",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
