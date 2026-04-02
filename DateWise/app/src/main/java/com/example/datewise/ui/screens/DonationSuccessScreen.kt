package com.example.datewise.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.datewise.ui.theme.DateWiseGreen

@Composable
fun DonationSuccessScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "✅",
            fontSize = 120.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        Text(
            text = "You have successfuly donated to charity",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DateWiseGreen)
        ) {
            Text(
                text = "Return",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}
