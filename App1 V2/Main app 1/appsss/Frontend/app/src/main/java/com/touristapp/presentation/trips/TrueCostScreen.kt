package com.touristapp.presentation.trips

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.touristapp.ui.theme.EmeraldPrimary
import com.touristapp.ui.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrueCostScreen(
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("True Trip Cost") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Total Budget", style = MaterialTheme.typography.labelMedium)
                    Text("₹45,000", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = EmeraldPrimary)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { 0.85f }, // Mock 85% spent
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = ErrorRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Spent: ₹38,200", style = MaterialTheme.typography.bodySmall)
                        Text("Remaining: ₹6,800", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Alert Mock
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f))
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(Icons.Filled.Warning, contentDescription = "Alert", tint = ErrorRed)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Budget Drift Alert", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = ErrorRed)
                        Text("You are trending 18% above budget on Dining. Consider booking the prepaid meal package to save ₹2,000.", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            val expenses = listOf("Flights" to "₹12,000", "Hotels" to "₹15,000", "Dining" to "₹8,500", "Activities" to "₹2,700")
            
            expenses.forEach { (category, amount) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(category, style = MaterialTheme.typography.bodyLarge)
                    Text(amount, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }
        }
    }
}
