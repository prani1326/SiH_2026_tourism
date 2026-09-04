package com.travellikepro.opsleader.ui.bookings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.travellikepro.opsleader.ui.triprequests.TripRequestListScreen

@Composable
fun BookingsScreen(
    onNavigateToTripRequestDetail: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Trip Requests", "Confirmed Bookings")

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> {
                TripRequestListScreen(onNavigateToDetail = onNavigateToTripRequestDetail)
            }
            1 -> {
                // Placeholder for Confirmed Bookings
                Text(
                    text = "Confirmed Bookings List (To Be Implemented)",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
