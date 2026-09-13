package com.touristapp.presentation.bookings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.data.models.TripModel
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.components.EmptyState
import com.touristapp.ui.components.SkeletonLoader
import com.touristapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingReviewScreen(
    tripId: String,
    onBackClick: () -> Unit,
    onProceedToPayment: (String, Double) -> Unit
) {
    var trip by remember { mutableStateOf<TripModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var travelerName by remember { mutableStateOf("Traveler") }
    var travelerPhone by remember { mutableStateOf("+91 9876543210") }
    var travelerEmail by remember { mutableStateOf("user@touristapp.com") }
    var specialRequests by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tripId) {
        isLoading = true
        val loaded = ServiceLocator.tripRepository.getTripById(tripId)
        trip = loaded
        val sessionUser = ServiceLocator.sessionManager.currentUser.value
        val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (sessionUser != null) {
            travelerName = sessionUser.full_name.ifBlank { "Traveler" }
            travelerPhone = sessionUser.phone ?: (fbUser?.phoneNumber ?: "+91 9876543210")
            travelerEmail = sessionUser.email ?: (fbUser?.email ?: "user@touristapp.com")
        } else if (fbUser != null) {
            travelerName = fbUser.displayName ?: "Traveler"
            travelerPhone = fbUser.phoneNumber ?: "+91 9876543210"
            travelerEmail = fbUser.email ?: "user@touristapp.com"
        }
        isLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Booking Review",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Verify itinerary and pricing before payment",
                            style = MaterialTheme.typography.bodySmall,
                            color = TravelPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SkeletonLoader(modifier = Modifier.fillMaxWidth().height(160.dp))
                SkeletonLoader(modifier = Modifier.fillMaxWidth().height(120.dp))
                SkeletonLoader(modifier = Modifier.fillMaxWidth().height(200.dp))
            }
        } else if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                EmptyState(
                    title = "Trip not found",
                    message = "Could not locate trip details for review."
                )
            }
        } else {
            val nonNullTrip = trip!!
            val rawBudget = if (nonNullTrip.budgetTotal > 0) nonNullTrip.budgetTotal else 18000.0
            val hotelCost = rawBudget * 0.40
            val transportCost = rawBudget * 0.25
            val activitiesCost = rawBudget * 0.15
            val foodEstimate = rawBudget * 0.20
            val subtotal = hotelCost + transportCost + activitiesCost + foodEstimate
            val gst = subtotal * 0.18
            val platformFee = 99.0
            val finalTotal = subtotal + gst + platformFee

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 1. Trip Summary Header Card
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = TravelNavy),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = TravelPrimary
                                    ) {
                                        Text(
                                            "COMPLETE PACKAGE",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        "${nonNullTrip.daysCount} Days",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    nonNullTrip.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${nonNullTrip.destinationName} • ${nonNullTrip.travelersCount} Traveler(s) • ${nonNullTrip.style}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }

                    // 2. Package Inclusions Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Verified, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Package Inclusions", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                InclusionRow(icon = Icons.Filled.Hotel, title = "Hotel Accommodation", desc = "Verified properties with daily breakfast included")
                                InclusionRow(icon = Icons.Filled.DirectionsCar, title = "Dedicated Private Transport", desc = "AC cab with verified driver for all itinerary transfers")
                                InclusionRow(icon = Icons.Filled.ConfirmationNumber, title = "Attraction Passes & Entry", desc = "Priority entry tickets to key monuments and tours")
                                InclusionRow(icon = Icons.Filled.Restaurant, title = "Curated Dining Table Holds", desc = "Pre-arranged table bookings at authentic local venues")
                            }
                        }
                    }

                    // 3. Traveler Information Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Lead Traveler Information", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = travelerName,
                                    onValueChange = { travelerName = it },
                                    label = { Text("Full Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = travelerPhone,
                                    onValueChange = { travelerPhone = it },
                                    label = { Text("Contact Phone") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = travelerEmail,
                                    onValueChange = { travelerEmail = it },
                                    label = { Text("Email for Invoice & Tickets") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // 4. Itemized Price Breakdown Card
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.brandPillBackground),
                            border = BorderStroke(1.5.dp, TravelPrimary.copy(alpha = 0.3f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.ReceiptLong, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Price Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TravelPrimary)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                PriceRow(label = "Hotel Stay & Lodging", amount = hotelCost)
                                PriceRow(label = "Transport & Cab Transfers", amount = transportCost)
                                PriceRow(label = "Activities & Monument Passes", amount = activitiesCost)
                                PriceRow(label = "Curated Dining Allocation", amount = foodEstimate)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = TravelPrimary.copy(alpha = 0.2f))
                                PriceRow(label = "Subtotal", amount = subtotal, isBold = true)
                                PriceRow(label = "Applicable GST (18%)", amount = gst)
                                PriceRow(label = "Platform & Concierge Fee", amount = platformFee)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = TravelPrimary.copy(alpha = 0.3f), thickness = 1.5.dp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Final Total Payable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text("₹${finalTotal.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TravelPrimary)
                                }
                            }
                        }
                    }

                    // 5. Guarantee Banner
                    item {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, TravelSuccess.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Shield, contentDescription = null, tint = TravelSuccess, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("100% Booking Guarantee", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Instant refund on cancellations up to 24h prior to travel date. Fully secured transaction.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }

                    // Spacer for bottom button
                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }

                // Fixed Bottom Floating "PROCEED TO PAYMENT" Button
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background,
                    shadowElevation = 16.dp
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Button(
                            onClick = {
                                onProceedToPayment(tripId, finalTotal)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "PROCEED TO PAYMENT • ₹${finalTotal.toInt()}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InclusionRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.brandPillBackground),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
    }
}

@Composable
private fun PriceRow(label: String, amount: Double, isBold: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )
        Text(
            "₹${amount.toInt()}",
            style = if (isBold) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isBold) TravelPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}
