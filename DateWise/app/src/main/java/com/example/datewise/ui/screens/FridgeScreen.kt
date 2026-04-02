package com.example.datewise.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.datewise.data.Product
import com.example.datewise.data.ProductCategory
import com.example.datewise.ui.theme.DateWiseGreen
import com.example.datewise.ui.theme.DateWiseGreenLight
import com.example.datewise.ui.theme.DateWiseGreenSurface
import com.example.datewise.ui.theme.ExpiryExpired
import com.example.datewise.ui.theme.ExpiryGreen
import com.example.datewise.ui.theme.ExpirySoon
import com.example.datewise.ui.viewmodels.SharedViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun FridgeScreen(
    viewModel: SharedViewModel,
    onAddProduct: () -> Unit = {},
    onNavigateToDonationSuccess: () -> Unit = {},
    onEditProduct: (Int) -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val expiringProducts by viewModel.expiringProducts.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isDonationsEnabled by viewModel.isDonationsEnabled.collectAsState()
    val filteredProducts = products.filter { it.category == selectedCategory }

    val totalCount = filteredProducts.size
    val soonCount = filteredProducts.count {
        val days = ChronoUnit.DAYS.between(LocalDate.now(), it.expiryDate)
        days in 1..7
    }
    val expiredCount = filteredProducts.count { it.expiryDate.isBefore(LocalDate.now()) }

    // State for the product detail modal
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    var showNotificationMenu by remember { mutableStateOf(false) }

    if (showNotificationMenu) {
        NotificationMenu(
            expiringProducts = expiringProducts,
            onDismissRequest = { showNotificationMenu = false },
            onDeleteProduct = { viewModel.removeProduct(it) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            FridgeHeader(
                hasNotifications = expiringProducts.isNotEmpty(),
                onNotificationClick = { showNotificationMenu = true }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Category tabs
            CategoryTabs(selectedCategory) { viewModel.selectCategory(it) }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats row
            StatsRow(totalCount, soonCount, expiredCount)

            Spacer(modifier = Modifier.height(20.dp))

            if (filteredProducts.isEmpty()) {
                // Empty state
                EmptyFridgeState(selectedCategory, onAddProduct)
            } else {
                // Section Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My ${selectedCategory.name.lowercase().replaceFirstChar { it.uppercase() }} Items",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "See All",
                        style = MaterialTheme.typography.labelLarge,
                        color = DateWiseGreenLight,
                        modifier = Modifier.clickable { }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Product grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts) { product ->
                        ProductCard(
                            product = product,
                            onClick = { selectedProduct = product }
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = onAddProduct,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = DateWiseGreen,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add product")
        }
    }

    // Product Detail Modal (shared component)
    selectedProduct?.let { product ->
        ProductDetailSheet(
            product = product,
            onDismiss = { selectedProduct = null },
            onDelete = {
                viewModel.removeProduct(product.id)
                selectedProduct = null
            },
            onEdit = {
                onEditProduct(product.id)
                selectedProduct = null
            },
            isDonationsEnabled = isDonationsEnabled,
            onDonate = {
                viewModel.donateProduct(product)
                selectedProduct = null
                onNavigateToDonationSuccess()
            }
        )
    }
}

@Composable
private fun EmptyFridgeState(category: ProductCategory, onAddProduct: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = when (category) {
                ProductCategory.FRIDGE -> "🧊"
                ProductCategory.PANTRY -> "🗄️"
                ProductCategory.FREEZER -> "❄️"
            },
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your ${category.name.lowercase()} is empty",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add your first product by scanning a barcode or entering it manually",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp),
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            onClick = onAddProduct,
            shape = RoundedCornerShape(12.dp),
            color = DateWiseGreen
        ) {
            Text(
                text = "  + Add Product  ",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FridgeHeader(
    hasNotifications: Boolean,
    onNotificationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DateWiseGreen),
                contentAlignment = Alignment.Center
            ) {
                Text("🥬", fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "DateWise",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Fresh & Organized",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Box {
            IconButton(onClick = onNotificationClick) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            if (hasNotifications) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    selectedCategory: ProductCategory,
    onCategorySelected: (ProductCategory) -> Unit
) {
    val categories = ProductCategory.entries.toList()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        categories.forEach { category ->
            val isSelected = category == selectedCategory
            val bgColor by animateColorAsState(
                if (isSelected) DateWiseGreen else Color.Transparent,
                label = "tabColor"
            )
            val textColor by animateColorAsState(
                if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tabTextColor"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor)
                    .clickable { onCategorySelected(category) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun StatsRow(total: Int, soon: Int, expired: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "TOTAL",
            count = total,
            icon = Icons.Filled.Inventory2,
            iconTint = DateWiseGreenLight,
            countColor = MaterialTheme.colorScheme.onBackground
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "SOON",
            count = soon,
            icon = Icons.Filled.Warning,
            iconTint = ExpirySoon,
            countColor = ExpirySoon
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "EXPIRED",
            count = expired,
            icon = Icons.Outlined.ErrorOutline,
            iconTint = ExpiryExpired,
            countColor = ExpiryExpired
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    icon: ImageVector,
    iconTint: Color,
    countColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = countColor
            )
        }
    }
}

@Composable
private fun ProductCard(product: Product, onClick: () -> Unit) {
    val today = LocalDate.now()
    val daysUntilExpiry = ChronoUnit.DAYS.between(today, product.expiryDate)

    val (badgeText, badgeColor) = when {
        daysUntilExpiry < 0 -> "Expired ${-daysUntilExpiry}d ago" to ExpiryExpired
        daysUntilExpiry == 0L -> "Expires today" to ExpiryExpired
        daysUntilExpiry == 1L -> "Expires tomorrow" to ExpirySoon
        daysUntilExpiry <= 7 -> "Expires in $daysUntilExpiry days" to ExpirySoon
        else -> "Expires in $daysUntilExpiry days" to ExpiryGreen
    }

    val isExpired = daysUntilExpiry < 0

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(
                width = 1.dp,
                color = if (isExpired) ExpiryExpired.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Emoji placeholder for product image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isExpired) ExpiryExpired.copy(alpha = 0.08f)
                        else DateWiseGreenSurface.copy(alpha = 0.5f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getProductEmoji(product.name),
                    fontSize = 36.sp
                )
                if (isExpired) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExpiryExpired)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "EXPIRED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }
                if (product.isOpened) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ExpirySoon)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "OPENED",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (product.description.isNotEmpty()) {
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(badgeColor)
                )
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = badgeColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
