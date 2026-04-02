package com.example.datewisepos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.datewisepos.ui.batchticket.BatchTicketScreen
import com.example.datewisepos.ui.batchticket.BatchTicketViewModel
import com.example.datewisepos.ui.inventory.InventoryScreen
import com.example.datewisepos.ui.inventory.InventoryViewModel
import com.example.datewisepos.ui.inventory.ProductDetailScreen
import com.example.datewisepos.ui.inventory.ProductDetailViewModel
import com.example.datewisepos.ui.scan.AddProductScreen
import com.example.datewisepos.ui.scan.ScanScreen
import com.example.datewisepos.ui.scan.ScanViewModel
import com.example.datewisepos.ui.ticket.TicketPreviewScreen
import com.example.datewisepos.ui.ticket.TicketViewModel
import com.example.datewisepos.ui.theme.DateWisePOSTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DateWisePOSTheme {
                DateWisePOSApp()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String) {
    object Scan : Screen("scan", "Scan")
    object Inventory : Screen("inventory", "Inventory")
    object BatchTicket : Screen("batchTicket", "Batch")
    object AddProduct : Screen("addProduct?barcode={barcode}", "Add Product") {
        fun createRoute(barcode: String = "") =
            if (barcode.isNotBlank()) "addProduct?barcode=$barcode" else "addProduct"
    }
    object ProductDetail : Screen("productDetail/{productId}", "Product") {
        fun createRoute(productId: Long) = "productDetail/$productId"
    }
    object TicketPreview : Screen("ticket/{productId}/{expiryDate}", "Ticket") {
        fun createRoute(productId: Long, expiryDate: Long) = "ticket/$productId/$expiryDate"
    }
}

@Composable
fun DateWisePOSApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Shared ScanViewModel so both ScanScreen and AddProductScreen see the same state
    val scanViewModel: ScanViewModel = viewModel()

    val bottomNavScreens = listOf(Screen.Scan, Screen.Inventory, Screen.BatchTicket)
    val showBottomNav = currentRoute in bottomNavScreens.map { it.route }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Scan.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = when (screen) {
                                        is Screen.Scan -> Icons.Default.Search
                                        is Screen.Inventory -> Icons.AutoMirrored.Filled.List
                                        is Screen.BatchTicket -> Icons.Default.Add
                                        else -> Icons.AutoMirrored.Filled.List
                                    },
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Scan.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Scan.route) {
                ScanScreen(
                    viewModel = scanViewModel,
                    onProductFound = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId)) {
                            popUpTo(Screen.Scan.route)
                        }
                    },
                    onNavigateToAdd = { barcode ->
                        navController.navigate(Screen.AddProduct.createRoute(barcode))
                    },
                    onManualEntry = {
                        navController.navigate(Screen.AddProduct.createRoute())
                    }
                )
            }

            composable(
                route = Screen.AddProduct.route,
                arguments = listOf(
                    navArgument("barcode") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
                AddProductScreen(
                    viewModel = scanViewModel,
                    initialBarcode = barcode,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Inventory.route) {
                val inventoryViewModel: InventoryViewModel = viewModel()
                InventoryScreen(
                    viewModel = inventoryViewModel,
                    onProductClick = { productId ->
                        navController.navigate(Screen.ProductDetail.createRoute(productId))
                    },
                    onScanClick = {
                        navController.navigate(Screen.Scan.route) {
                            popUpTo(Screen.Scan.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.BatchTicket.route) {
                val batchViewModel: BatchTicketViewModel = viewModel()
                BatchTicketScreen(
                    viewModel = batchViewModel
                )
            }

            composable(
                route = Screen.ProductDetail.route,
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: return@composable
                val detailViewModel: ProductDetailViewModel = viewModel()
                ProductDetailScreen(
                    productId = productId,
                    viewModel = detailViewModel,
                    onBack = { navController.popBackStack() },
                    onGenerateTicket = { prodId, expiryDate ->
                        navController.navigate(Screen.TicketPreview.createRoute(prodId, expiryDate))
                    }
                )
            }

            composable(
                route = Screen.TicketPreview.route,
                arguments = listOf(
                    navArgument("productId") { type = NavType.LongType },
                    navArgument("expiryDate") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getLong("productId") ?: return@composable
                val expiryDate = backStackEntry.arguments?.getLong("expiryDate") ?: return@composable
                val ticketViewModel: TicketViewModel = viewModel()
                TicketPreviewScreen(
                    productId = productId,
                    expiryDateMillis = expiryDate,
                    viewModel = ticketViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}