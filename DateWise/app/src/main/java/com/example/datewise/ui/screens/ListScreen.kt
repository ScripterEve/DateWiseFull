package com.example.datewise.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.datewise.data.ShoppingItem
import com.example.datewise.ui.theme.DateWiseGreen
import com.example.datewise.ui.theme.DateWiseGreenAccent
import com.example.datewise.ui.theme.DateWiseGreenLight
import com.example.datewise.ui.theme.DateWiseGreenSurface
import com.example.datewise.ui.theme.ExpiryExpired
import com.example.datewise.ui.viewmodels.BarcodeLookupState
import com.example.datewise.ui.viewmodels.SharedViewModel
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(viewModel: SharedViewModel, onAddItem: () -> Unit = {}) {
    val shoppingItems by viewModel.shoppingItems.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val uncheckedItems = shoppingItems.filter { !it.isChecked }
    val checkedItems = shoppingItems.filter { it.isChecked }

    // Filter by search
    val filteredUnchecked = if (searchQuery.isBlank()) uncheckedItems
    else uncheckedItems.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val filteredChecked = if (searchQuery.isBlank()) checkedItems
    else checkedItems.filter { it.name.contains(searchQuery, ignoreCase = true) }

    // Add-item bottom sheet state
    var showAddSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header
            ListHeader()

            // Summary bar
            ShoppingListSummary(
                totalItems = shoppingItems.size,
                checkedCount = checkedItems.size,
                uncheckedCount = uncheckedItems.size
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search or quick-add item...") },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DateWiseGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )
                // Quick add from search
                if (searchQuery.isNotBlank()) {
                    IconButton(
                        onClick = {
                            viewModel.addShoppingItem(ShoppingItem(name = searchQuery.trim()))
                            searchQuery = ""
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DateWiseGreen)
                    ) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Quick Add",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (shoppingItems.isEmpty() && searchQuery.isBlank()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🛒", fontSize = 64.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your shopping list is empty",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to add items manually or scan a barcode",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Surface(
                        onClick = { showAddSheet = true },
                        shape = RoundedCornerShape(14.dp),
                        color = DateWiseGreen
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White)
                            Text(
                                text = "Add First Item",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // TO PURCHASE section
                    if (filteredUnchecked.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "TO PURCHASE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(DateWiseGreen.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${filteredUnchecked.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DateWiseGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        items(filteredUnchecked, key = { it.id }) { item ->
                            ShoppingListItem(
                                item = item,
                                onToggle = { viewModel.toggleShoppingItemChecked(item.id) },
                                onDelete = { viewModel.removeShoppingItem(item.id) }
                            )
                        }
                    }

                    // CHECKED section
                    if (filteredChecked.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "DONE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                    alpha = 0.12f
                                                )
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${filteredChecked.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Surface(
                                    onClick = { viewModel.clearCheckedShoppingItems() },
                                    shape = RoundedCornerShape(8.dp),
                                    color = ExpiryExpired.copy(alpha = 0.1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 4.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = null,
                                            tint = ExpiryExpired,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Clear All",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = ExpiryExpired,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        items(filteredChecked, key = { it.id }) { item ->
                            ShoppingListItem(
                                item = item,
                                onToggle = { viewModel.toggleShoppingItemChecked(item.id) },
                                onDelete = { viewModel.removeShoppingItem(item.id) },
                                isChecked = true
                            )
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = DateWiseGreen,
            contentColor = Color.White
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add item")
        }
    }

    // Add Item Bottom Sheet
    if (showAddSheet) {
        AddShoppingItemSheet(
            viewModel = viewModel,
            onDismiss = { showAddSheet = false }
        )
    }
}

@Composable
private fun ShoppingListSummary(totalItems: Int, checkedCount: Int, uncheckedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // To buy card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DateWiseGreenSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.ShoppingCart,
                        contentDescription = null,
                        tint = DateWiseGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TO BUY",
                        style = MaterialTheme.typography.labelSmall,
                        color = DateWiseGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uncheckedCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = DateWiseGreen
                )
            }
        }

        // Done card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "DONE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = checkedCount.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Total card
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "TOTAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = totalItems.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddShoppingItemSheet(
    viewModel: SharedViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Manual", "Barcode", "Scan")

    // Manual add state
    var itemName by remember { mutableStateOf("") }
    var itemDescription by remember { mutableStateOf("") }

    // Barcode state
    var barcodeInput by remember { mutableStateOf("") }
    val barcodeLookupState by viewModel.shoppingBarcodeLookupState.collectAsState()

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetShoppingBarcodeLookup()
            onDismiss()
        },
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                text = "Add to Shopping List",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tabs: Manual | Barcode | Scan
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = DateWiseGreen,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = DateWiseGreen
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Filled.ShoppingCart
                                        1 -> Icons.Filled.QrCode
                                        else -> Icons.Filled.CameraAlt
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = tab,
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        },
                        selectedContentColor = DateWiseGreen,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (selectedTab) {
                0 -> ManualAddContent(
                    itemName = itemName,
                    onItemNameChange = { itemName = it },
                    itemDescription = itemDescription,
                    onItemDescriptionChange = { itemDescription = it },
                    onAdd = {
                        if (itemName.isNotBlank()) {
                            viewModel.addShoppingItem(
                                ShoppingItem(
                                    name = itemName.trim(),
                                    description = itemDescription.trim()
                                )
                            )
                            itemName = ""
                            itemDescription = ""
                            onDismiss()
                        }
                    }
                )

                1 -> BarcodeAddContent(
                    barcodeInput = barcodeInput,
                    onBarcodeChange = { barcodeInput = it },
                    barcodeLookupState = barcodeLookupState,
                    onLookup = {
                        if (barcodeInput.isNotBlank()) {
                            viewModel.lookupBarcodeForShopping(barcodeInput.trim())
                        }
                    },
                    onAddFound = { name, desc ->
                        viewModel.addShoppingItem(
                            ShoppingItem(name = name, description = desc)
                        )
                        viewModel.resetShoppingBarcodeLookup()
                        barcodeInput = ""
                        onDismiss()
                    },
                    onReset = {
                        viewModel.resetShoppingBarcodeLookup()
                        barcodeInput = ""
                    }
                )

                2 -> CameraScanContent(
                    viewModel = viewModel,
                    barcodeLookupState = barcodeLookupState,
                    onAddFound = { name, desc ->
                        viewModel.addShoppingItem(
                            ShoppingItem(name = name, description = desc)
                        )
                        viewModel.resetShoppingBarcodeLookup()
                        onDismiss()
                    },
                    onReset = {
                        viewModel.resetShoppingBarcodeLookup()
                    }
                )
            }
        }
    }
}

// ==================== Tab 1: Manual ====================

@Composable
private fun ManualAddContent(
    itemName: String,
    onItemNameChange: (String) -> Unit,
    itemDescription: String,
    onItemDescriptionChange: (String) -> Unit,
    onAdd: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Preview
        if (itemName.isNotBlank()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DateWiseGreenSurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DateWiseGreen.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(getShoppingEmoji(itemName), fontSize = 22.sp)
                    }
                    Column {
                        Text(
                            text = itemName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (itemDescription.isNotBlank()) {
                            Text(
                                text = itemDescription,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = itemName,
            onValueChange = onItemNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Item name *") },
            placeholder = { Text("e.g. Milk, Eggs, Bread...") },
            leadingIcon = {
                Icon(
                    Icons.Filled.ShoppingCart,
                    contentDescription = null,
                    tint = DateWiseGreen,
                    modifier = Modifier.size(20.dp)
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DateWiseGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedLabelColor = DateWiseGreen
            ),
            singleLine = true
        )

        OutlinedTextField(
            value = itemDescription,
            onValueChange = onItemDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Note (optional)") },
            placeholder = { Text("e.g. 2L, organic, brand...") },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DateWiseGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedLabelColor = DateWiseGreen
            ),
            singleLine = true
        )

        Button(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DateWiseGreen),
            enabled = itemName.isNotBlank()
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add to List", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

// ==================== Tab 2: Barcode (manual entry) ====================

@Composable
private fun BarcodeAddContent(
    barcodeInput: String,
    onBarcodeChange: (String) -> Unit,
    barcodeLookupState: BarcodeLookupState,
    onLookup: () -> Unit,
    onAddFound: (name: String, description: String) -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = barcodeInput,
            onValueChange = onBarcodeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Barcode number") },
            placeholder = { Text("Enter barcode (e.g. 5449000000996)") },
            leadingIcon = {
                Icon(
                    Icons.Filled.QrCode,
                    contentDescription = null,
                    tint = DateWiseGreen,
                    modifier = Modifier.size(20.dp)
                )
            },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DateWiseGreen,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                focusedLabelColor = DateWiseGreen
            ),
            singleLine = true
        )

        Button(
            onClick = onLookup,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DateWiseGreen),
            enabled = barcodeInput.isNotBlank() && barcodeLookupState !is BarcodeLookupState.Loading
        ) {
            if (barcodeLookupState is BarcodeLookupState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Looking up...")
            } else {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Look Up Product", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }

        BarcodeLookupResult(
            barcodeLookupState = barcodeLookupState,
            onAddFound = onAddFound,
            onReset = onReset
        )
    }
}

// ==================== Tab 3: Camera Scan ====================

@Composable
private fun CameraScanContent(
    viewModel: SharedViewModel,
    barcodeLookupState: BarcodeLookupState,
    onAddFound: (name: String, description: String) -> Unit,
    onReset: () -> Unit
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
        onResult = { granted -> hasCameraPermission = granted }
    )

    if (!hasCameraPermission) {
        // Permission needed
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.QrCodeScanner,
                contentDescription = null,
                tint = DateWiseGreenLight,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Camera permission required",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Allow camera access to scan barcodes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DateWiseGreen)
            ) {
                Text("Grant Permission", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        }
    } else {
        // Camera scanner
        CameraScannerView(
            viewModel = viewModel,
            barcodeLookupState = barcodeLookupState,
            onAddFound = onAddFound,
            onReset = onReset
        )
    }
}

@Composable
private fun CameraScannerView(
    viewModel: SharedViewModel,
    barcodeLookupState: BarcodeLookupState,
    onAddFound: (name: String, description: String) -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember { LifecycleCameraController(context) }
    var detectedBarcode by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
        .build()
    val scanner = remember { BarcodeScanning.getClient(options) }

    // Set up the barcode analyzer
    DisposableEffect(cameraController) {
        cameraController.setImageAnalysisAnalyzer(
            ContextCompat.getMainExecutor(context)
        ) { imageProxy ->
            if (!isProcessing) {
                imageProxy.image?.let { image ->
                    val inputImage = InputImage.fromMediaImage(image, imageProxy.imageInfo.rotationDegrees)
                    scanner.process(inputImage)
                        .addOnSuccessListener { barcodes ->
                            if (barcodes.isNotEmpty() && !isProcessing) {
                                val code = barcodes.first().displayValue
                                    ?: barcodes.first().rawValue
                                if (code != null) {
                                    detectedBarcode = code
                                    isProcessing = true
                                    viewModel.lookupBarcodeForShopping(code)
                                }
                            }
                        }
                        .addOnFailureListener { it.printStackTrace() }
                        .addOnCompleteListener { imageProxy.close() }
                }
            } else {
                imageProxy.close()
            }
        }
        onDispose {
            cameraController.clearImageAnalysisAnalyzer()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Camera preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(16.dp))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        controller = cameraController
                        cameraController.bindToLifecycle(lifecycleOwner)
                    }
                }
            )

            // Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            // Scan frame
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.Center)
                    .border(2.5.dp, DateWiseGreenAccent, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                when (barcodeLookupState) {
                    is BarcodeLookupState.Loading -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = DateWiseGreenAccent,
                                modifier = Modifier.size(32.dp),
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Looking up…",
                                color = DateWiseGreenAccent,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    is BarcodeLookupState.Idle -> {
                        Text(
                            text = "Point at a barcode",
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                    else -> {}
                }
            }

            // Detected barcode badge
            if (detectedBarcode != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Barcode: $detectedBarcode",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Result section
        BarcodeLookupResult(
            barcodeLookupState = barcodeLookupState,
            onAddFound = onAddFound,
            onReset = {
                isProcessing = false
                detectedBarcode = null
                onReset()
            }
        )

        // If idle, show hint
        if (barcodeLookupState is BarcodeLookupState.Idle) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = DateWiseGreenLight,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Auto-scan enabled",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Product will be looked up automatically when detected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// ==================== Shared result card ====================

@Composable
private fun BarcodeLookupResult(
    barcodeLookupState: BarcodeLookupState,
    onAddFound: (name: String, description: String) -> Unit,
    onReset: () -> Unit
) {
    when (barcodeLookupState) {
        is BarcodeLookupState.Found -> {
            val info = barcodeLookupState.productInfo
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DateWiseGreenSurface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DateWiseGreen.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(getShoppingEmoji(info.name), fontSize = 24.sp)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Product found!",
                                style = MaterialTheme.typography.labelSmall,
                                color = DateWiseGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = info.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (info.description.isNotEmpty()) {
                                Text(
                                    text = info.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = { onAddFound(info.name, info.description) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DateWiseGreen)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add to List", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        is BarcodeLookupState.NotFound -> {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ExpiryExpired.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("😕", fontSize = 36.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Product not found",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ExpiryExpired
                    )
                    Text(
                        text = "Barcode \"${barcodeLookupState.barcode}\" wasn't found.\nTry adding it manually instead!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onReset,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Try Again")
                    }
                }
            }
        }

        else -> {}
    }
}

// ==================== Shared components ====================

@Composable
private fun ListHeader() {
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
                    text = "Shopping List",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "What do you need?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = { }) {
            Icon(
                Icons.Filled.Notifications,
                contentDescription = "Notifications",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ShoppingListItem(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    isChecked: Boolean = false
) {
    val bgColor by animateColorAsState(
        targetValue = if (isChecked)
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.surface,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "itemBg"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isChecked) Icons.Filled.CheckCircle
                else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = if (isChecked) "Uncheck" else "Check",
                tint = if (isChecked) DateWiseGreen
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    getShoppingEmoji(item.name),
                    fontSize = 20.sp,
                    modifier = Modifier.alpha(if (isChecked) 0.5f else 1f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (isChecked) 0.5f else 1f)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isChecked) FontWeight.Normal else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.description.isNotEmpty()) {
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun getShoppingEmoji(name: String): String {
    val lower = name.lowercase()
    return when {
        "milk" in lower -> "🥛"
        "avocado" in lower -> "🥑"
        "spinach" in lower || "salad" in lower || "lettuce" in lower -> "🥬"
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
        "broccoli" in lower -> "🥦"
        "cucumber" in lower -> "🥒"
        "soap" in lower || "detergent" in lower || "shampoo" in lower -> "🧴"
        "paper" in lower || "tissue" in lower || "towel" in lower -> "🧻"
        else -> "🛒"
    }
}
