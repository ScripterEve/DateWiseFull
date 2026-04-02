package com.example.datewise.ui.screens

import android.app.DatePickerDialog
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.datewise.data.Product
import com.example.datewise.data.ProductCategory
import com.example.datewise.ui.theme.DateWiseGreen
import com.example.datewise.ui.theme.DateWiseGreenAccent
import com.example.datewise.ui.theme.DateWiseGreenLight
import com.example.datewise.ui.viewmodels.BarcodeLookupState
import com.example.datewise.ui.viewmodels.SharedViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField

@Composable
fun AddProductScreen(
    viewModel: SharedViewModel,
    productId: Int? = null,
    prefilledBarcode: String = "",
    prefilledName: String = "",
    prefilledDescription: String = "",
    prefilledExpiryDate: String = "",
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    // Initialize state with default values
    var productName by remember { mutableStateOf(prefilledName) }
    var barcodeOrCode by remember { mutableStateOf(prefilledBarcode) }
    var description by remember { mutableStateOf(prefilledDescription) }
    var selectedCategory by remember { mutableStateOf(ProductCategory.FRIDGE) }
    var expiryDate by remember { mutableStateOf(parseExpiryDate(prefilledExpiryDate) ?: LocalDate.now().plusDays(7)) }
    var isOpened by remember { mutableStateOf(false) }
    var useWithinDays by remember { mutableStateOf("") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var lookupMessage by remember { mutableStateOf("") }

    // Load existing product if in edit mode
    val isEditMode = productId != null
    LaunchedEffect(productId) {
        if (productId != null) {
            val product = viewModel.getProductById(productId)
            if (product != null) {
                productName = product.name
                barcodeOrCode = product.barcode
                description = product.description
                selectedCategory = product.category
                expiryDate = product.expiryDate
                isOpened = product.isOpened
                useWithinDays = product.useWithinDays?.toString() ?: ""
            }
        }
    }

    val barcodeLookupState by viewModel.barcodeLookupState.collectAsState()

    // React to barcode lookup results
    LaunchedEffect(barcodeLookupState) {
        when (val state = barcodeLookupState) {
            is BarcodeLookupState.Found -> {
                val info = state.productInfo
                if (productName.isBlank()) productName = info.name
                if (description.isBlank()) description = info.description
                lookupMessage = "✓ Found: ${info.name}"
                viewModel.resetBarcodeLookup()
            }
            is BarcodeLookupState.NotFound -> {
                lookupMessage = "Product not found — enter details manually"
                viewModel.resetBarcodeLookup()
            }
            else -> {}
        }
    }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = DateWiseGreen,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        focusedLabelColor = DateWiseGreen,
        cursorColor = DateWiseGreen
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DateWiseGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🥬", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DateWise",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isEditMode) "Edit Product" else "Add Product",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (isEditMode) "Update product details" else "Enter the product details below",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Product Name
            Text(
                text = "Product Name",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = productName,
                onValueChange = { productName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Organic Whole Milk") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Barcode / Code
            Text(
                text = "Barcode / Product Code",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = barcodeOrCode,
                    onValueChange = {
                        barcodeOrCode = it
                        lookupMessage = ""
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("e.g. 1234567890123") },
                    shape = RoundedCornerShape(12.dp),
                    colors = textFieldColors,
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (barcodeOrCode.isNotBlank()) {
                            lookupMessage = ""
                            viewModel.lookupBarcode(barcodeOrCode.trim())
                        }
                    },
                    enabled = barcodeOrCode.isNotBlank() && barcodeLookupState !is BarcodeLookupState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = DateWiseGreen),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(56.dp)
                ) {
                    if (barcodeLookupState is BarcodeLookupState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Lookup",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (lookupMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = lookupMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (lookupMessage.startsWith("✓")) DateWiseGreen
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                text = "Description",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. 1L Bottle, 500g Pack") },
                shape = RoundedCornerShape(12.dp),
                colors = textFieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Category dropdown
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoryDropdown = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedCategory.name.lowercase()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Select",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                DropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false }
                ) {
                    ProductCategory.entries.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    category.name.lowercase().replaceFirstChar { it.uppercase() }
                                )
                            },
                            onClick = {
                                selectedCategory = category
                                showCategoryDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Expiration Date
            Text(
                text = "Expiration Date",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val picker = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                expiryDate = LocalDate.of(year, month + 1, dayOfMonth)
                            },
                            expiryDate.year,
                            expiryDate.monthValue - 1,
                            expiryDate.dayOfMonth
                        )
                        picker.show()
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.CalendarToday,
                            contentDescription = null,
                            tint = DateWiseGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = expiryDate.format(dateFormatter),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Select date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Add buttons
            Text(
                text = "QUICK ADD",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickAddButton("+1 Week", Modifier.weight(1f)) {
                    expiryDate = LocalDate.now().plusWeeks(1)
                }
                QuickAddButton("+2 Weeks", Modifier.weight(1f)) {
                    expiryDate = LocalDate.now().plusWeeks(2)
                }
                QuickAddButton("+1 Month", Modifier.weight(1f)) {
                    expiryDate = LocalDate.now().plusMonths(1)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Opened checkbox
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = if (isOpened) androidx.compose.foundation.BorderStroke(
                    1.dp, DateWiseGreen.copy(alpha = 0.5f)
                ) else null
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isOpened,
                            onCheckedChange = { isOpened = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = DateWiseGreen,
                                checkmarkColor = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Opened",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Product has been opened and should be used soon",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isOpened) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Use within (days)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = useWithinDays,
                            onValueChange = {
                                if (it.all { c -> c.isDigit() }) useWithinDays = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. 3") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = textFieldColors,
                            singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Add to Inventory / Save Changes button
            Button(
                onClick = {
                    if (productName.isNotBlank()) {
                        var finalExpiryDate = expiryDate
                        // Logic for "opened" dynamic expiry
                        if (isOpened && useWithinDays.isNotBlank()) {
                            val days = useWithinDays.toIntOrNull()
                            if (days != null) {
                                val openedExpiry = LocalDate.now().plusDays(days.toLong())
                                // Only shorten the expiry, don't lengthen it arbitrarily
                                if (openedExpiry.isBefore(expiryDate)) {
                                    finalExpiryDate = openedExpiry
                                }
                            }
                        }

                        val product = Product(
                            id = if (isEditMode) productId!! else 0,
                            name = productName,
                            expiryDate = finalExpiryDate,
                            category = selectedCategory,
                            barcode = barcodeOrCode,
                            description = description,
                            isOpened = isOpened,
                            useWithinDays = if (isOpened && useWithinDays.isNotEmpty())
                                useWithinDays.toIntOrNull() else null
                        )

                        if (isEditMode) {
                            viewModel.updateProduct(product)
                            onNavigateBack()
                        } else {
                            viewModel.addProduct(product)
                            
                            // Check for pending batch items
                            val nextBatchItem = viewModel.popNextBatchItem()
                            if (nextBatchItem != null) {
                                // Load next item into UI state
                                productName = nextBatchItem.name
                                barcodeOrCode = nextBatchItem.barcode
                                description = nextBatchItem.description
                                expiryDate = parseExpiryDate(nextBatchItem.expiryDate) ?: LocalDate.now().plusDays(7)
                                isOpened = false
                                useWithinDays = ""
                                lookupMessage = ""
                            } else {
                                // No more items, return to previous screen
                                onNavigateBack()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DateWiseGreen
                ),
                shape = RoundedCornerShape(16.dp),
                enabled = productName.isNotBlank()
            ) {
                Text(
                    text = if (isEditMode) "Save Changes" else "➕  Add to Inventory",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QuickAddButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = DateWiseGreenAccent.copy(alpha = 0.15f),
            contentColor = DateWiseGreenLight
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun parseExpiryDate(dateStr: String?): LocalDate? {
    if (dateStr.isNullOrBlank()) return null
    // Remove "Expires " or "EXPDT " prefixes if they exist
    val cleanStr = dateStr.replace(Regex("(?i)^(expires|expdt|exp)\\s*"), "").trim()
    
    val formats = listOf(
        "dd-MM-yyyy",
        "MM-dd-yyyy", // US standard
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "MM/dd/yyyy",
        "yyyy/MM/dd",
        "dd.MM.yyyy",
        "MM.dd.yyyy",
        "yyyy.MM.dd",
        "ddMMyyyy"
    )
    for (f in formats) {
        try {
            return LocalDate.parse(cleanStr, DateTimeFormatter.ofPattern(f))
        } catch (e: Exception) {}
    }
    
    // GS1 format yyMMdd
    if (cleanStr.length == 6 && cleanStr.all { it.isDigit() }) {
        try {
            val formatter = DateTimeFormatterBuilder()
                .appendValueReduced(ChronoField.YEAR, 2, 2, LocalDate.now().year - 5)
                .appendPattern("MMdd")
                .toFormatter()
            return LocalDate.parse(cleanStr, formatter)
        } catch (e: Exception) {}
    }
    
    return null
}
