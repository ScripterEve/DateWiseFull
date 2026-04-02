package com.example.datewise.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.datewise.ui.theme.DateWiseGreen
import com.example.datewise.ui.viewmodels.SharedViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.datewise.workers.DailyExpiryWorker

@Composable
fun SettingsScreen(
    viewModel: SharedViewModel,
    onNavigateToDonations: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isDonationsEnabled by viewModel.isDonationsEnabled.collectAsState()
    val expiringProducts by viewModel.expiringProducts.collectAsState()
    var showNotificationMenu by remember { mutableStateOf(false) }

    if (showNotificationMenu) {
        NotificationMenu(
            expiringProducts = expiringProducts,
            onDismissRequest = { showNotificationMenu = false },
            onDeleteProduct = { viewModel.removeProduct(it) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
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
                IconButton(onClick = { showNotificationMenu = true }) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (expiringProducts.isNotEmpty()) {
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

        // Settings content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Donations Feature",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Switch(
                    checked = isDonationsEnabled,
                    onCheckedChange = { viewModel.toggleDonationsEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DateWiseGreen,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
            
            if (isDonationsEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onNavigateToDonations() },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DateWiseGreen)
                ) {
                    Text("My Donations", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val workRequest = OneTimeWorkRequestBuilder<DailyExpiryWorker>().build()
                    WorkManager.getInstance(context).enqueue(workRequest)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text("Test Expiry Notifications", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        }
    }
}
