package com.touristapp.presentation.trips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.touristapp.data.remote.ApiResult
import com.touristapp.ui.components.EmptyState
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    onTripClick: (String) -> Unit,
    onPlanNewTripClick: () -> Unit,
    viewModel: TripsViewModel = viewModel()
) {
    val tripsResult = viewModel.trips.collectAsState().value

    LaunchedEffect(Unit) {
        viewModel.loadTrips()
    }

    var selectedTabIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(0) }
    val tabs = listOf("Active", "Upcoming", "Past")

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("My Trips", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onPlanNewTripClick) {
                Icon(Icons.Filled.Add, contentDescription = "Plan Trip")
            }
        }
    ) { paddingValues ->
        when (tripsResult) {
            is ApiResult.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ApiResult.Success -> {
                val allTrips = tripsResult.data.map { it as JsonObject }
                val filteredTrips = when (selectedTabIndex) {
                    0 -> allTrips.filter {
                        val s = it["status"]?.jsonPrimitive?.content?.lowercase() ?: ""
                        s == "active" || s == "planning" || s == "ongoing"
                    }
                    1 -> allTrips.filter {
                        val s = it["status"]?.jsonPrimitive?.content?.lowercase() ?: ""
                        s == "upcoming" || s == "confirmed"
                    }
                    else -> allTrips.filter {
                        val s = it["status"]?.jsonPrimitive?.content?.lowercase() ?: ""
                        s == "completed" || s == "past"
                    }
                }.sortedWith(
                    compareBy(
                        { if (it["destination_name"]?.jsonPrimitive?.content?.contains("Kerala", ignoreCase = true) == true) 0 else if (it["destination_name"]?.jsonPrimitive?.content?.contains("Agra", ignoreCase = true) == true) 1 else 2 },
                        { it["start_date"]?.jsonPrimitive?.content ?: "" }
                    )
                )

                Column(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
                    PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(title) }
                            )
                        }
                    }

                    if (filteredTrips.isEmpty()) {
                        EmptyState(
                            title = "No ${tabs[selectedTabIndex].lowercase()} trips",
                            message = "Start your next adventure by creating an AI itinerary.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredTrips.size) { index ->
                                val trip = filteredTrips[index]
                                val tripId = trip["id"]?.jsonPrimitive?.content ?: ""
                                val title = trip["title"]?.jsonPrimitive?.content ?: "My Trip"
                                val status = trip["status"]?.jsonPrimitive?.content ?: "planning"
                                val destination = trip["destination_name"]?.jsonPrimitive?.content ?: "Destination"
                                val startDate = trip["start_date"]?.jsonPrimitive?.content ?: ""
                                val endDate = trip["end_date"]?.jsonPrimitive?.content ?: ""
                                val travelers = trip["traveler_count"]?.jsonPrimitive?.content ?: "1"
                                val budget = trip["budget_total"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 25000.0

                                Card(
                                    onClick = { onTripClick(tripId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                                                color = if (status.equals("active", true) || status.equals("confirmed", true)) com.touristapp.ui.theme.TravelSuccess.copy(alpha = 0.15f) else com.touristapp.ui.theme.TravelPrimary.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = status.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (status.equals("active", true) || status.equals("confirmed", true)) com.touristapp.ui.theme.TravelSuccess else com.touristapp.ui.theme.TravelPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }

                                            Text(
                                                text = "₹${budget.toInt()}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = destination,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = com.touristapp.ui.theme.TravelPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = if (startDate.isNotBlank()) "$startDate - $endDate" else "Curated AI Itinerary",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                            )
                                            Text(
                                                text = "$travelers Traveler(s)",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            is ApiResult.Error, is ApiResult.Exception -> {
                EmptyState(
                    title = "Failed to load trips",
                    message = "Please try again later.",
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}
