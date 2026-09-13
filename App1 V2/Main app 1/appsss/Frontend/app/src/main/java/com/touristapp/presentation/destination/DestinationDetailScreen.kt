package com.touristapp.presentation.destination

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.touristapp.ui.theme.*

import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.json.jsonPrimitive
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import com.touristapp.data.remote.ApiResult
import com.touristapp.presentation.bookings.BookingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationDetailScreen(
    destinationId: String,
    onBackClick: () -> Unit,
    onPlanTripClick: (String) -> Unit,
    onBookingsClick: (() -> Unit)? = null,
    viewModel: DestinationDetailViewModel = viewModel(),
    bookingViewModel: BookingViewModel = viewModel()
) {
    val destinationResult by viewModel.destination.collectAsState()
    var showBookDialog by remember { mutableStateOf(false) }
    var bookSuccessBookingId by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(destinationId) {
        viewModel.loadDestination(destinationId)
    }

    Scaffold(
        bottomBar = {
            if (destinationResult is ApiResult.Success) {
                val dest = (destinationResult as ApiResult.Success).data
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text("Est. Budget / Day", style = MaterialTheme.typography.labelSmall)
                            Text(
                                dest.budgetPerDay ?: dest.estimatedBudgetTier,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showBookDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.BookOnline, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Book", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = { onPlanTripClick(dest.name) },
                                colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Plan with AI", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            when (val res = destinationResult) {
                is ApiResult.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ApiResult.Error, is ApiResult.Exception -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Failed to load destination details", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadDestination(destinationId) }) {
                            Text("Retry")
                        }
                    }
                }
                is ApiResult.Success -> {
                    val dest = res.data
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            ) {
                                com.touristapp.ui.components.NetworkImage(
                                    url = dest.heroImageUrl,
                                    contentDescription = dest.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    IconButton(
                                        onClick = onBackClick,
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Row {
                                        IconButton(
                                            onClick = { },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                        ) {
                                            Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { },
                                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                                        ) {
                                            Icon(Icons.Filled.FavoriteBorder, contentDescription = "Save", tint = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = dest.name,
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = "Rating", tint = OrangeAccent, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${dest.rating}", style = MaterialTheme.typography.bodyMedium)
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = dest.knownFor ?: dest.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                )
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                if (dest.idealStay != null) {
                                    Text("Ideal Stay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(dest.idealStay, style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                if (dest.topAttractions.isNotEmpty()) {
                                    Text("Top Attractions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(dest.topAttractions.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                if (dest.famousFood.isNotEmpty()) {
                                    Text("Famous Food", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(dest.famousFood.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                if (dest.activities.isNotEmpty()) {
                                    Text("Activities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(dest.activities.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                
                                if (dest.localTransport.isNotEmpty()) {
                                    Text("Local Transport", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(dest.localTransport.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                if (dest.nearbyPlaces.isNotEmpty()) {
                                    Text("Nearby Destinations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(dest.nearbyPlaces.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = "Safety & Insights",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Safety Score: ${dest.safetyScore}/100",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TravelPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Best time to visit: ${dest.bestTimeToVisit}",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBookDialog && destinationResult is ApiResult.Success) {
        val dest = (destinationResult as ApiResult.Success).data
        var guestCount by remember { mutableIntStateOf(2) }
        var specialReq by remember { mutableStateOf("") }
        var isBookingSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isBookingSubmitting) showBookDialog = false },
            title = {
                Text("Book Experience • ${dest.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Reserve a verified stay & curated experience package in ${dest.name}, ${dest.state}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Guests", fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (guestCount > 1) guestCount-- },
                                enabled = guestCount > 1
                            ) {
                                Icon(Icons.Filled.RemoveCircleOutline, contentDescription = "Decrease")
                            }
                            Text("$guestCount", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(
                                onClick = { if (guestCount < 10) guestCount++ },
                                enabled = guestCount < 10
                            ) {
                                Icon(Icons.Filled.AddCircleOutline, contentDescription = "Increase")
                            }
                        }
                    }

                    val estTotal = guestCount * 4500.0
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Estimated Total:", style = MaterialTheme.typography.bodyMedium)
                            Text("₹${estTotal.toInt()}", fontWeight = FontWeight.Bold, color = TravelPrimary)
                        }
                    }

                    OutlinedTextField(
                        value = specialReq,
                        onValueChange = { specialReq = it },
                        label = { Text("Special Requests (Optional)") },
                        placeholder = { Text("e.g. Early check-in, veg meals") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isBookingSubmitting = true
                        val estTotal = guestCount * 4500.0
                        bookingViewModel.requestBooking(
                            itemType = "hotel",
                            itemId = dest.id,
                            itemTitle = "${dest.name} Heritage Stay & Guided Tour",
                            checkInDate = "2025-10-15",
                            checkOutDate = "2025-10-18",
                            guestCount = guestCount,
                            totalAmount = estTotal,
                            specialRequests = specialReq.ifBlank { null },
                            onSuccess = { model ->
                                isBookingSubmitting = false
                                showBookDialog = false
                                bookSuccessBookingId = model.bookingReference
                            }
                        )
                    },
                    enabled = !isBookingSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                ) {
                    if (isBookingSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Reserve Now", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBookDialog = false },
                    enabled = !isBookingSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (bookSuccessBookingId != null) {
        AlertDialog(
            onDismissRequest = { bookSuccessBookingId = null },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(48.dp)) },
            title = { Text("Booking Created! 🎉", fontWeight = FontWeight.Bold) },
            text = {
                Text("Your reservation (Ref: #${bookSuccessBookingId}) has been created successfully. You can pay securely with Razorpay in My Bookings.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        bookSuccessBookingId = null
                        onBookingsClick?.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                ) {
                    Text("View in My Bookings")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookSuccessBookingId = null }) {
                    Text("Done")
                }
            }
        )
    }
}
