package com.example.datewise.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.datewise.data.Product
import com.example.datewise.ui.theme.DateWiseGreen
import com.example.datewise.ui.theme.DateWiseGreenLight
import com.example.datewise.ui.theme.DateWiseGreenSurface
import com.example.datewise.ui.theme.ExpiryExpired
import com.example.datewise.ui.theme.ExpiryGreen
import com.example.datewise.ui.theme.ExpirySoon
import com.example.datewise.ui.viewmodels.SharedViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: SharedViewModel,
    onNavigateToDonationSuccess: () -> Unit = {},
    onEditProduct: (Int) -> Unit = {}
) {
    val products by viewModel.products.collectAsState()
    val expiringProducts by viewModel.expiringProducts.collectAsState()
    val isDonationsEnabled by viewModel.isDonationsEnabled.collectAsState()
    val today = LocalDate.now()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(today) }

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

    // Build a map of date -> list of products expiring on that date
    val expiryMap: Map<LocalDate, List<Product>> = remember(products) {
        products.groupBy { it.expiryDate }
    }

    // Products expiring on selected date
    val productsOnSelectedDate = expiryMap[selectedDate] ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        CalendarHeader(
            hasNotifications = expiringProducts.isNotEmpty(),
            onNotificationClick = { showNotificationMenu = true }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Month navigation + Today button
        MonthNavigation(
            currentMonth = currentMonth,
            selectedDate = selectedDate,
            today = today,
            onMonthChanged = { currentMonth = it },
            onTodayClick = {
                currentMonth = YearMonth.now()
                selectedDate = today
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Calendar grid — clickable days
        CalendarGrid(
            currentMonth = currentMonth,
            today = today,
            selectedDate = selectedDate,
            expiryMap = expiryMap,
            onDateSelected = { date ->
                selectedDate = date
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Selected-date products section
        val sectionTitle = when (selectedDate) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            today.minusDays(1) -> "Yesterday"
            else -> selectedDate.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = sectionTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (productsOnSelectedDate.isEmpty())
                        "No products expiring"
                    else
                        "${productsOnSelectedDate.size} product${if (productsOnSelectedDate.size != 1) "s" else ""} expiring",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Count badge
            if (productsOnSelectedDate.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DateWiseGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = productsOnSelectedDate.size.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (productsOnSelectedDate.isEmpty()) {
            // Empty state for selected date
            EmptyDateState(selectedDate, today)
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(productsOnSelectedDate) { product ->
                    ExpiringProductCard(
                        product = product,
                        onClick = { selectedProduct = product }
                    )
                }
            }
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
private fun EmptyDateState(selectedDate: LocalDate, today: LocalDate) {
    val daysFromToday = ChronoUnit.DAYS.between(today, selectedDate)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✅", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = when {
                daysFromToday < 0 -> "Nothing expired on this day"
                daysFromToday == 0L -> "Nothing expiring today"
                else -> "Nothing expiring on this day"
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Tap another date to check",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun CalendarHeader(
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
            Text(
                text = "DateWise",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        Box {
            IconButton(onClick = onNotificationClick) {
                Icon(
                    Icons.Filled.Notifications,
                    contentDescription = "Notifications",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
private fun MonthNavigation(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onMonthChanged: (YearMonth) -> Unit,
    onTodayClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "${currentMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${currentMonth.year}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // "Today" chip — show when not viewing current month or today isn't selected
            if (currentMonth != YearMonth.now() || selectedDate != today) {
                Surface(
                    onClick = onTodayClick,
                    shape = RoundedCornerShape(8.dp),
                    color = DateWiseGreenSurface
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = DateWiseGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Today",
                            style = MaterialTheme.typography.labelMedium,
                            color = DateWiseGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            IconButton(onClick = { onMonthChanged(currentMonth.minusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onMonthChanged(currentMonth.plusMonths(1)) }) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CalendarGrid(
    currentMonth: YearMonth,
    today: LocalDate,
    selectedDate: LocalDate,
    expiryMap: Map<LocalDate, List<Product>>,
    onDateSelected: (LocalDate) -> Unit
) {
    val daysOfWeek = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val firstDayOfMonth = currentMonth.atDay(1)
    val firstDayOffset = (firstDayOfMonth.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = currentMonth.lengthOfMonth()

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar days
        val totalCells = firstDayOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until 7) {
                    val dayIndex = row * 7 + col - firstDayOffset + 1
                    if (dayIndex in 1..daysInMonth) {
                        val date = currentMonth.atDay(dayIndex)
                        val isToday = date == today
                        val isSelected = date == selectedDate
                        val productsOnDate = expiryMap[date] ?: emptyList()
                        val hasExpiry = productsOnDate.isNotEmpty()

                        // Determine dot color based on how close the expiry is relative to today
                        val dotColor = if (hasExpiry) {
                            val daysUntil = ChronoUnit.DAYS.between(today, date)
                            when {
                                daysUntil < 0 -> ExpiryExpired
                                daysUntil <= 3 -> ExpirySoon
                                daysUntil <= 7 -> ExpirySoon
                                else -> ExpiryGreen
                            }
                        } else null

                        // Animated selection
                        val bgColor by animateColorAsState(
                            targetValue = when {
                                isSelected && isToday -> DateWiseGreen
                                isSelected -> DateWiseGreen.copy(alpha = 0.15f)
                                isToday -> DateWiseGreen.copy(alpha = 0.08f)
                                else -> Color.Transparent
                            },
                            label = "dayBg"
                        )
                        val textColor by animateColorAsState(
                            targetValue = when {
                                isSelected && isToday -> Color.White
                                isSelected -> DateWiseGreen
                                isToday -> DateWiseGreen
                                else -> MaterialTheme.colorScheme.onBackground
                            },
                            label = "dayText"
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.1f else 1f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "dayScale"
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                                .scale(scale),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(bgColor)
                                        .then(
                                            if (isSelected && !isToday)
                                                Modifier.border(
                                                    1.5.dp,
                                                    DateWiseGreen,
                                                    CircleShape
                                                )
                                            else Modifier
                                        )
                                        .clickable { onDateSelected(date) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayIndex.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = textColor,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (hasExpiry && dotColor != null) {
                                    // Show count dot for multiple, plain dot for single
                                    if (productsOnDate.size > 1) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 1.dp)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(dotColor)
                                                .padding(horizontal = 3.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = productsOnDate.size.toString(),
                                                fontSize = 6.sp,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 6.sp
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 1.dp)
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(dotColor)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiringProductCard(product: Product, onClick: () -> Unit) {
    val today = LocalDate.now()
    val daysUntilExpiry = ChronoUnit.DAYS.between(today, product.expiryDate)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    val (statusText, statusColor) = when {
        daysUntilExpiry < 0 -> "Expired ${-daysUntilExpiry}d ago" to ExpiryExpired
        daysUntilExpiry == 0L -> "Expires today" to ExpiryExpired
        daysUntilExpiry == 1L -> "Tomorrow" to ExpirySoon
        daysUntilExpiry <= 3 -> "$daysUntilExpiry Days" to ExpirySoon
        daysUntilExpiry <= 7 -> "$daysUntilExpiry Days" to ExpirySoon
        else -> "$daysUntilExpiry Days" to ExpiryGreen
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product emoji
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(getProductEmoji(product.name), fontSize = 26.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = product.category.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (product.description.isNotEmpty()) {
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = product.expiryDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}