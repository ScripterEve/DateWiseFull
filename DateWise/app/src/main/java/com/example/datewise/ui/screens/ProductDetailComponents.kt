package com.example.datewise.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.datewise.data.Product
import com.example.datewise.ui.theme.DateWiseGreen
import com.example.datewise.ui.theme.DateWiseGreenLight
import com.example.datewise.ui.theme.DateWiseGreenSurface
import com.example.datewise.ui.theme.ExpiryExpired
import com.example.datewise.ui.theme.ExpiryGreen
import com.example.datewise.ui.theme.ExpirySoon
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Reusable ModalBottomSheet that shows full product details.
 * Used from FridgeScreen and CalendarScreen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailSheet(
    product: Product,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    isDonationsEnabled: Boolean = false,
    onDonate: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    ) {
        ProductDetailContent(
            product = product,
            onDelete = onDelete,
            onDismiss = onDismiss,
            onEdit = onEdit,
            isDonationsEnabled = isDonationsEnabled,
            onDonate = onDonate
        )
    }
}

@Composable
fun ProductDetailContent(
    product: Product,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    onEdit: () -> Unit = {},
    isDonationsEnabled: Boolean = false,
    onDonate: () -> Unit = {}
) {
    val today = LocalDate.now()
    val daysUntilExpiry = ChronoUnit.DAYS.between(today, product.expiryDate)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    val (statusText, statusColor) = when {
        daysUntilExpiry < 0 -> "Expired ${-daysUntilExpiry} days ago" to ExpiryExpired
        daysUntilExpiry == 0L -> "Expires today!" to ExpiryExpired
        daysUntilExpiry == 1L -> "Expires tomorrow" to ExpirySoon
        daysUntilExpiry <= 3 -> "Expires in $daysUntilExpiry days" to ExpirySoon
        daysUntilExpiry <= 7 -> "Expires in $daysUntilExpiry days" to ExpirySoon
        else -> "Expires in $daysUntilExpiry days" to ExpiryGreen
    }

    val isExpired = daysUntilExpiry < 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // --- Hero emoji section ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (isExpired) ExpiryExpired.copy(alpha = 0.08f)
                    else DateWiseGreenSurface.copy(alpha = 0.6f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = getProductEmoji(product.name),
                fontSize = 64.sp
            )
            // Status badges
            if (isExpired) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ExpiryExpired)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "EXPIRED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (product.isOpened) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ExpirySoon)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "OPENED",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Product name ---
        Text(
            text = product.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        // --- Description ---
        if (product.description.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = product.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Expiry status banner ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                    Text(
                        text = product.expiryDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- Detail rows ---
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category
        DetailRow(
            icon = Icons.Filled.Category,
            label = "Category",
            value = product.category.name.lowercase().replaceFirstChar { it.uppercase() },
            iconTint = DateWiseGreenLight
        )

        // Added date
        DetailRow(
            icon = Icons.Filled.CalendarToday,
            label = "Added",
            value = product.addedDate.format(dateFormatter),
            iconTint = DateWiseGreenLight
        )

        // Barcode (copyable)
        if (product.barcode.isNotEmpty()) {
            CopyableBarcodeRow(
                barcode = product.barcode
            )
        }

        // Use within (if opened)
        if (product.isOpened && product.useWithinDays != null) {
            DetailRow(
                icon = Icons.Filled.Schedule,
                label = "Use within",
                value = "${product.useWithinDays} days after opening",
                iconTint = ExpirySoon
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // --- Action buttons ---
        if (isDonationsEnabled && !product.isOpened) {
            Button(
                onClick = onDonate,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DateWiseGreenLight
                )
            ) {
                Icon(
                    Icons.Filled.CardGiftcard,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Donate to charity",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ExpiryExpired
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, ExpiryExpired.copy(alpha = 0.5f)
                )
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = DateWiseGreen
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, DateWiseGreen.copy(alpha = 0.5f)
                )
            ) {
                Text("Edit", fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DateWiseGreen
                )
            ) {
                Text(
                    "Close",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun CopyableBarcodeRow(barcode: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                copyToClipboard(context, barcode)
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DateWiseGreenLight.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.QrCode,
                contentDescription = null,
                tint = DateWiseGreenLight,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Barcode",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
            Text(
                text = barcode,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(
            onClick = { copyToClipboard(context, barcode) },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = "Copy barcode",
                tint = DateWiseGreenLight,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Barcode", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Barcode copied!", Toast.LENGTH_SHORT).show()
}

fun getProductEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        "milk" in lower -> "🥛"
        "avocado" in lower -> "🥑"
        "spinach" in lower -> "🥬"
        "banana" in lower -> "🍌"
        "egg" in lower -> "🥚"
        "yogurt" in lower || "yoghurt" in lower -> "🥛"
        "carrot" in lower -> "🥕"
        "pasta" in lower -> "🍝"
        "tomato" in lower -> "🍅"
        "pea" in lower -> "🫛"
        "ice cream" in lower -> "🍨"
        "chicken" in lower -> "🍗"
        "bread" in lower -> "🍞"
        "cheese" in lower -> "🧀"
        "apple" in lower -> "🍎"
        "orange" in lower -> "🍊"
        "rice" in lower -> "🍚"
        "fish" in lower || "salmon" in lower || "tuna" in lower -> "🐟"
        "butter" in lower -> "🧈"
        "juice" in lower -> "🧃"
        "water" in lower -> "💧"
        "cereal" in lower -> "🥣"
        "meat" in lower || "beef" in lower || "pork" in lower -> "🥩"
        "pepper" in lower -> "🌶️"
        "onion" in lower -> "🧅"
        "garlic" in lower -> "🧄"
        "lemon" in lower -> "🍋"
        "berry" in lower || "strawberry" in lower || "blueberry" in lower -> "🍓"
        "grape" in lower -> "🍇"
        "potato" in lower -> "🥔"
        "corn" in lower -> "🌽"
        "mushroom" in lower -> "🍄"
        "shrimp" in lower || "prawn" in lower -> "🦐"
        "ham" in lower -> "🍖"
        "sausage" in lower -> "🌭"
        "pizza" in lower -> "🍕"
        "cake" in lower -> "🍰"
        "cookie" in lower || "biscuit" in lower -> "🍪"
        "chocolate" in lower -> "🍫"
        "honey" in lower -> "🍯"
        "coffee" in lower -> "☕"
        "tea" in lower -> "🍵"
        "wine" in lower -> "🍷"
        "beer" in lower -> "🍺"
        "sauce" in lower || "ketchup" in lower || "mustard" in lower -> "🫙"
        "oil" in lower -> "🫒"
        "cream" in lower -> "🍦"
        "soup" in lower -> "🍲"
        "salad" in lower || "lettuce" in lower -> "🥗"
        "broccoli" in lower -> "🥦"
        "cucumber" in lower -> "🥒"
        else -> "🛒"
    }
}
