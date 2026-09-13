package com.travellikepro.opsleader.ui.bookings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.travellikepro.opsleader.data.api.BookingDto
import com.travellikepro.opsleader.ui.components.StatusBadge
import com.travellikepro.opsleader.ui.components.StatusLevel
import com.travellikepro.opsleader.ui.triprequests.TripRequestListScreen

@Composable
fun BookingsScreen(
    onNavigateToTripRequestDetail: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Trip Requests", "Confirmed Bookings")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTabIndex) {
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
                ConfirmedBookingsTab()
            }
        }
    }
}

@Composable
fun ConfirmedBookingsTab(
    viewModel: BookingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search by booking ID, tourist...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = { viewModel.loadBookings() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == "ALL",
                onClick = { selectedFilter = "ALL" },
                label = { Text("All Bookings") }
            )
            FilterChip(
                selected = selectedFilter == "PAID",
                onClick = { selectedFilter = "PAID" },
                label = { Text("Paid") }
            )
            FilterChip(
                selected = selectedFilter == "PARTIAL",
                onClick = { selectedFilter = "PARTIAL" },
                label = { Text("Partial") }
            )
        }

        when (val state = uiState) {
            is BookingsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is BookingsUiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("No confirmed bookings found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            is BookingsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadBookings() }) { Text("Retry") }
                    }
                }
            }

            is BookingsUiState.Success -> {
                val filtered = state.bookings.filter {
                    val nameMatch = it.tourist_name?.contains(searchQuery, ignoreCase = true) == true
                    val destMatch = it.destination?.contains(searchQuery, ignoreCase = true) == true
                    val idMatch = it.id.contains(searchQuery, ignoreCase = true)
                    val filterMatch = selectedFilter == "ALL" || it.payment_status.equals(selectedFilter, ignoreCase = true)
                    (searchQuery.isEmpty() || nameMatch || destMatch || idMatch) && filterMatch
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No bookings matching criteria.", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered) { booking ->
                            BookingCard(booking = booking)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BookingCard(booking: BookingDto) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = booking.id,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = booking.tourist_name ?: "Guest",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                val bookingStatus = booking.booking_status ?: "PENDING"
                val statusLevel = when (bookingStatus.uppercase()) {
                    "CONFIRMED" -> StatusLevel.RESOLVED
                    "COMPLETED" -> StatusLevel.NORMAL
                    else -> StatusLevel.WARNING
                }
                StatusBadge(label = bookingStatus.uppercase(), status = statusLevel)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = "Location",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = booking.destination ?: "Field Destination",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = "Dates",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = booking.dates ?: "Dates scheduled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    Icons.Default.Group,
                    contentDescription = "Guests",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${booking.guests_count} Guests",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: $${booking.total_amount}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                val paymentStatus = booking.payment_status ?: "PENDING"
                val isPaid = paymentStatus.equals("PAID", ignoreCase = true)
                AssistChip(
                    onClick = { },
                    label = { Text("Payment: ${paymentStatus.uppercase()}") },
                    leadingIcon = {
                        Icon(
                            if (isPaid) Icons.Default.CheckCircle else Icons.Default.Pending,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = if (isPaid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                        )
                    }
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Assigned Partners:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                if (booking.hotel_partner != null) {
                    Text("• Hotel: ${booking.hotel_partner}", style = MaterialTheme.typography.bodySmall)
                }
                if (booking.transport_partner != null) {
                    Text("• Transport: ${booking.transport_partner}", style = MaterialTheme.typography.bodySmall)
                }
                if (booking.guide_partner != null) {
                    Text("• Guide: ${booking.guide_partner}", style = MaterialTheme.typography.bodySmall)
                }
                if (booking.tourist_contact != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Contact: ${booking.tourist_contact}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
