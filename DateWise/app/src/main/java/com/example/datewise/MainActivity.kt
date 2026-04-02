package com.example.datewise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.datewise.ui.screens.AddProductScreen
import com.example.datewise.ui.screens.CalendarScreen
import com.example.datewise.ui.screens.DonatedItemsScreen
import com.example.datewise.ui.screens.DonationSuccessScreen
import com.example.datewise.ui.screens.FridgeScreen
import com.example.datewise.ui.screens.ListScreen
import com.example.datewise.ui.screens.ScanScreen
import com.example.datewise.ui.screens.SettingsScreen
import com.example.datewise.ui.theme.DateWiseGreen
import com.example.datewise.ui.theme.DateWiseTheme
import com.example.datewise.ui.viewmodels.SharedViewModel
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.datewise.workers.DailyExpiryWorker
import com.example.datewise.workers.NotificationHelper
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val viewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        NotificationHelper.createNotificationChannel(this)
        requestNotificationPermission()
        scheduleDailyExpiryWorker()

        setContent {
            DateWiseTheme {
                MainScreen(viewModel)
            }
        }
    }

    private fun requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    private fun scheduleDailyExpiryWorker() {
        val workRequest = PeriodicWorkRequestBuilder<DailyExpiryWorker>(1, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "DailyExpiryWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}

@Composable
fun MainScreen(viewModel: SharedViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide bottom bar on add_product screen, edit_product screen, and full-screen modals
    val showBottomBar = currentRoute?.startsWith("add_product") != true &&
            currentRoute?.startsWith("edit_product") != true &&
            currentRoute != "donated_items" &&
            currentRoute != "donation_success"

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                DateWiseBottomBar(navController, currentRoute)
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            Navigation(navController, viewModel)
        }
    }
}

@Composable
fun Navigation(navController: NavHostController, viewModel: SharedViewModel) {
    NavHost(navController, startDestination = "fridge") {
        composable("fridge") {
            FridgeScreen(
                viewModel = viewModel,
                onAddProduct = { navController.navigate("add_product") },
                onNavigateToDonationSuccess = { navController.navigate("donation_success") },
                onEditProduct = { productId -> navController.navigate("edit_product/$productId") }
            )
        }
        composable("calendar") {
            CalendarScreen(
                viewModel = viewModel,
                onNavigateToDonationSuccess = { navController.navigate("donation_success") },
                onEditProduct = { productId -> navController.navigate("edit_product/$productId") }
            )
        }
        composable("scan") {
            ScanScreen(
                viewModel = viewModel,
                onEnterManually = { navController.navigate("add_product") },
                onNavigateToAdd = { barcode, name, description, expiryDate ->
                    val encodedName = URLEncoder.encode(name, "UTF-8")
                    val encodedDesc = URLEncoder.encode(description, "UTF-8")
                    val encodedExpiry = expiryDate?.let { URLEncoder.encode(it, "UTF-8") } ?: ""
                    navController.navigate("add_product?barcode=$barcode&name=$encodedName&description=$encodedDesc&expiryDate=$encodedExpiry")
                }
            )
        }
        composable("list") {
            ListScreen(
                viewModel = viewModel,
                onAddItem = { navController.navigate("add_product") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onNavigateToDonations = { navController.navigate("donated_items") }
            )
        }
        composable("donated_items") {
            DonatedItemsScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("donation_success") {
            DonationSuccessScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "add_product?barcode={barcode}&name={name}&description={description}&expiryDate={expiryDate}",
            arguments = listOf(
                navArgument("barcode") { type = NavType.StringType; defaultValue = "" },
                navArgument("name") { type = NavType.StringType; defaultValue = "" },
                navArgument("description") { type = NavType.StringType; defaultValue = "" },
                navArgument("expiryDate") { type = NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
            val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", "UTF-8")
            val description = URLDecoder.decode(backStackEntry.arguments?.getString("description") ?: "", "UTF-8")
            val expiryDate = URLDecoder.decode(backStackEntry.arguments?.getString("expiryDate") ?: "", "UTF-8")

            AddProductScreen(
                viewModel = viewModel,
                prefilledBarcode = barcode,
                prefilledName = name,
                prefilledDescription = description,
                prefilledExpiryDate = expiryDate,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "edit_product/{productId}",
            arguments = listOf(
                navArgument("productId") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getInt("productId")
            if (productId != null) {
                AddProductScreen(
                    viewModel = viewModel,
                    productId = productId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun DateWiseBottomBar(navController: NavHostController, currentRoute: String?) {
    val items = listOf(
        NavItem("fridge", Icons.Outlined.Kitchen, "Inventory"),
        NavItem("calendar", Icons.Filled.CalendarMonth, "Calendar"),
        NavItem("scan", Icons.Filled.QrCodeScanner, "Scan"),
        NavItem("list", Icons.Filled.ShoppingCart, "Shop List"),
        NavItem("settings", Icons.Filled.Settings, "Settings"),
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                val isScan = item.route == "scan"

                if (isScan) {
                    // Elevated center scan button
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .offset(y = (-8).dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(DateWiseGreen)
                                .clickable {
                                    navController.navigate(item.route) {
                                        navController.graph.startDestinationRoute?.let { route ->
                                            popUpTo(route) { saveState = true }
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) DateWiseGreen
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp
                        )
                    }
                } else {
                    // Regular tab
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                navController.navigate(item.route) {
                                    navController.graph.startDestinationRoute?.let { route ->
                                        popUpTo(route) { saveState = true }
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            item.icon,
                            contentDescription = item.label,
                            tint = if (isSelected) DateWiseGreen
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) DateWiseGreen
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

data class NavItem(val route: String, val icon: ImageVector, val label: String)
