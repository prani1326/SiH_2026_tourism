package com.touristapp.presentation.trips

import android.content.Intent
import android.net.Uri
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.data.models.TripModel
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.components.EmptyState
import com.touristapp.ui.components.SkeletonLoader
import com.touristapp.ui.theme.*
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    tripId: String,
    onBackClick: () -> Unit,
    onBookTripClick: ((String) -> Unit)? = null,
    onViewTripCard: ((String) -> Unit)? = null,
    onOpenMapClick: ((tripId: String, location: String) -> Unit)? = null
) {
    var trip by remember { mutableStateOf<TripModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(tripId) {
        isLoading = true
        val loaded = ServiceLocator.tripRepository.getTripById(tripId)
        trip = loaded
        isLoading = false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = trip?.title ?: "Trip Dashboard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1
                        )
                        trip?.let {
                            Text(
                                text = "${it.destinationName} • ${it.daysCount} Days • ${it.travelersCount} Traveler(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TravelPrimary
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (onViewTripCard != null) {
                        IconButton(onClick = { onViewTripCard(tripId) }) {
                            Icon(Icons.Filled.QrCode, contentDescription = "Trip Pass", tint = TravelPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            trip?.let { nonNullTrip ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Button(
                            onClick = {
                                onBookTripClick?.invoke(tripId)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                        ) {
                            Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "BOOK MY TRIP (₹${nonNullTrip.budgetTotal.toInt()})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
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
                SkeletonLoader(modifier = Modifier.fillMaxWidth().height(140.dp))
                SkeletonLoader(modifier = Modifier.fillMaxWidth().height(80.dp))
                repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(100.dp)) }
            }
        } else if (trip == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                EmptyState(
                    title = "Trip not found",
                    message = "Could not locate itinerary details for this trip."
                )
            }
        } else {
            val nonNullTrip = trip!!
            val (parsedDays, budgetBreakdown) = remember(nonNullTrip.itineraryJson) {
                parseItineraryData(nonNullTrip.itineraryJson, nonNullTrip.daysCount, nonNullTrip.destinationName, nonNullTrip.budgetTotal)
            }

            // Find upcoming / up next item
            val upNextItem = remember(parsedDays) {
                parsedDays.firstOrNull()?.items?.firstOrNull { it.type.equals("transport", ignoreCase = true) }
                    ?: parsedDays.firstOrNull()?.items?.firstOrNull()
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    // 1. Trip Overview Card
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = TravelNavy)
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
                                            text = nonNullTrip.status.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    Text(
                                        text = "Budget: ₹${nonNullTrip.budgetTotal.toInt()}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = nonNullTrip.destinationName,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.DateRange, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (nonNullTrip.startDate.isNotBlank()) "${nonNullTrip.startDate} to ${nonNullTrip.endDate}" else "${nonNullTrip.daysCount} Days Curated Journey",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Icon(Icons.Filled.People, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${nonNullTrip.travelersCount} Traveler(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    // 2. UP NEXT Section
                    if (upNextItem != null) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.brandPillBackground),
                                border = BorderStroke(1.5.dp, TravelPrimary.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(TravelPrimary),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "UP NEXT",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TravelDarkPrimary,
                                                letterSpacing = 1.sp
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = TravelPrimary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = upNextItem.status.uppercase(),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TravelPrimary
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = upNextItem.startTime,
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = upNextItem.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val loc = upNextItem.dropLocation.ifBlank { upNextItem.location.ifBlank { nonNullTrip.destinationName } }
                                                if (onOpenMapClick != null) {
                                                    onOpenMapClick(nonNullTrip.id, loc)
                                                } else {
                                                    openMapIntent(context, loc)
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("OPEN MAP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    if (upNextItem.pickupLocation.isNotBlank() || upNextItem.dropLocation.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = TravelPrimary.copy(alpha = 0.2f), thickness = 1.dp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (upNextItem.pickupLocation.isNotBlank()) {
                                            Text(
                                                "Pickup: ${upNextItem.pickupLocation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                            )
                                        }
                                        if (upNextItem.dropLocation.isNotBlank()) {
                                            Text(
                                                "Destination: ${upNextItem.dropLocation}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Trip Summary / Budget Allocation
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("TRIP COST ESTIMATION", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    BudgetItem(label = "Hotel", amount = budgetBreakdown.hotel)
                                    BudgetItem(label = "Transport", amount = budgetBreakdown.transport)
                                    BudgetItem(label = "Activities", amount = budgetBreakdown.activities)
                                    BudgetItem(label = "Dining", amount = budgetBreakdown.food)
                                    BudgetItem(label = "Taxes/Misc", amount = budgetBreakdown.miscellaneous)
                                }
                            }
                        }
                    }

                    // 4. Days Row Selector
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(parsedDays.indices.toList()) { index ->
                                val isSelected = selectedDayIndex == index
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedDayIndex = index },
                                    label = {
                                        Text(
                                            "Day ${index + 1}",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TravelPrimary,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }
                    }

                    // 5. Selected Day Timeline Content
                    if (parsedDays.isNotEmpty() && selectedDayIndex in parsedDays.indices) {
                        val currentDay = parsedDays[selectedDayIndex]

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.WbSunny, contentDescription = null, tint = TravelWarning, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = currentDay.theme,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = currentDay.weather,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }

                        // Chronological Schedule Items
                        items(currentDay.items) { item ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                when (item.type.lowercase()) {
                                    "transport" -> TransportTimelineCard(
                                        item = item,
                                        destination = nonNullTrip.destinationName,
                                        tripId = nonNullTrip.id,
                                        onOpenMapClick = onOpenMapClick
                                    )
                                    "hotel" -> HotelTimelineCard(
                                        item = item,
                                        destination = nonNullTrip.destinationName,
                                        tripId = nonNullTrip.id,
                                        onOpenMapClick = onOpenMapClick
                                    )
                                    "food", "dining", "meal", "restaurant" -> MealTimelineCard(
                                        item = item,
                                        destination = nonNullTrip.destinationName,
                                        tripId = nonNullTrip.id,
                                        onOpenMapClick = onOpenMapClick
                                    )
                                    else -> ActivityTimelineCard(
                                        item = item,
                                        destination = nonNullTrip.destinationName,
                                        tripId = nonNullTrip.id,
                                        onOpenMapClick = onOpenMapClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------- Timeline Cards ----------------

@Composable
private fun TransportTimelineCard(
    item: TimelineItem,
    destination: String,
    tripId: String = "",
    onOpenMapClick: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TravelWarning.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(8.dp), color = TravelWarning.copy(alpha = 0.15f)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = TravelDarkPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(item.transportType.ifBlank { "Cab / Transport" }, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TravelDarkPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(item.startTime, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }

                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.brandPillBackground) {
                    Text(item.status.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TravelPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (item.pickupLocation.isNotBlank()) {
                Text("📍 From: ${item.pickupLocation}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
            if (item.dropLocation.isNotBlank()) {
                Text("🏁 To: ${item.dropLocation}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text("${item.durationMinutes} min transfer", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(12.dp)) }
                    )
                }

                OutlinedButton(
                    onClick = {
                        val target = item.dropLocation.ifBlank { item.location.ifBlank { destination } }
                        if (onOpenMapClick != null) {
                            onOpenMapClick(tripId, target)
                        } else {
                            openMapIntent(context, target)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Route", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun HotelTimelineCard(
    item: TimelineItem,
    destination: String,
    tripId: String = "",
    onOpenMapClick: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, TravelPrimary.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.brandPillBackground) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Hotel, contentDescription = null, tint = TravelDarkPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Hotel Stay", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TravelDarkPrimary)
                    }
                }

                Text(item.startTime, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (item.location.isNotBlank()) {
                Text(item.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("₹${item.estimatedCost.toInt()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TravelPrimary)

                OutlinedButton(
                    onClick = {
                        val target = item.location.ifBlank { destination }
                        if (onOpenMapClick != null) {
                            onOpenMapClick(tripId, target)
                        } else {
                            openMapIntent(context, target)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Hotel Map", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun MealTimelineCard(
    item: TimelineItem,
    destination: String,
    tripId: String = "",
    onOpenMapClick: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.brandPillBackground) {
                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Restaurant, contentDescription = null, tint = TravelDarkPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dining / Meal", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TravelDarkPrimary)
                    }
                }

                Text("${item.startTime} - ${item.endTime}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (item.location.isNotBlank()) {
                Text(item.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("₹${item.estimatedCost.toInt()} est.", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TravelPrimary)

                OutlinedButton(
                    onClick = {
                        val target = item.location.ifBlank { destination }
                        if (onOpenMapClick != null) {
                            onOpenMapClick(tripId, target)
                        } else {
                            openMapIntent(context, target)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Directions", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun ActivityTimelineCard(
    item: TimelineItem,
    destination: String,
    tripId: String = "",
    onOpenMapClick: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.brandPillBackground) {
                    Text("Activity", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TravelDarkPrimary, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }

                Text("${item.startTime} - ${item.endTime}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            if (item.location.isNotBlank()) {
                Text(item.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
            if (item.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("₹${item.estimatedCost.toInt()}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TravelPrimary)

                OutlinedButton(
                    onClick = {
                        val target = item.location.ifBlank { destination }
                        if (onOpenMapClick != null) {
                            onOpenMapClick(tripId, target)
                        } else {
                            openMapIntent(context, target)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Explore Map", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun BudgetItem(label: String, amount: Double) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            "₹${amount.toInt()}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun openMapIntent(context: android.content.Context, query: String) {
    try {
        val uri = android.net.Uri.parse("geo:0,0?q=" + android.net.Uri.encode(query))
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            val webIntent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + android.net.Uri.encode(query))
            )
            context.startActivity(webIntent)
        }
    } catch (_: Exception) {
        val fallback = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=" + android.net.Uri.encode(query))
        )
        context.startActivity(fallback)
    }
}

// ---------------- Data Models & Parser ----------------

data class ItineraryBudget(
    val hotel: Double = 0.0,
    val food: Double = 0.0,
    val transport: Double = 0.0,
    val activities: Double = 0.0,
    val miscellaneous: Double = 0.0
)

data class TimelineItem(
    val sequence: Int = 1,
    val startTime: String = "09:00 AM",
    val endTime: String = "10:30 AM",
    val type: String = "activity",
    val title: String = "Explore Place",
    val location: String = "",
    val description: String = "",
    val durationMinutes: Int = 90,
    val transportType: String = "Cab",
    val pickupLocation: String = "",
    val dropLocation: String = "",
    val bookingRequired: Boolean = true,
    val bookingId: String = "",
    val vendorId: String = "",
    val status: String = "recommended",
    val estimatedCost: Double = 500.0
)

data class ParsedDay(
    val dayNumber: Int,
    val theme: String,
    val weather: String,
    val items: List<TimelineItem>
)

private fun parseItineraryData(
    jsonStr: String,
    daysCount: Int,
    destination: String,
    totalBudget: Double
): Pair<List<ParsedDay>, ItineraryBudget> {
    var budget = ItineraryBudget(
        hotel = totalBudget * 0.40,
        food = totalBudget * 0.25,
        transport = totalBudget * 0.15,
        activities = totalBudget * 0.15,
        miscellaneous = totalBudget * 0.05
    )

    if (jsonStr.isNotBlank()) {
        try {
            val root = JSONObject(jsonStr)

            val bbObj = root.optJSONObject("budgetBreakdown") ?: root.optJSONObject("budget_breakdown")
            if (bbObj != null) {
                budget = ItineraryBudget(
                    hotel = bbObj.optDouble("hotel", budget.hotel),
                    food = bbObj.optDouble("food", budget.food),
                    transport = bbObj.optDouble("transport", budget.transport),
                    activities = bbObj.optDouble("activities", budget.activities),
                    miscellaneous = bbObj.optDouble("miscellaneous", budget.miscellaneous)
                )
            }

            val daysArray = root.optJSONArray("days")
            if (daysArray != null && daysArray.length() > 0) {
                val list = mutableListOf<ParsedDay>()
                for (i in 0 until daysArray.length()) {
                    val dObj = daysArray.getJSONObject(i)
                    val timelineList = mutableListOf<TimelineItem>()

                    // Check for structured "items" first, else "activities"
                    val itemsArray = dObj.optJSONArray("items") ?: dObj.optJSONArray("activities")
                    if (itemsArray != null) {
                        for (j in 0 until itemsArray.length()) {
                            val aObj = itemsArray.getJSONObject(j)
                            val title = aObj.optString("title", "Explore $destination")
                            val type = aObj.optString("type", aObj.optString("activity_type", aObj.optString("time_slot", "activity")))
                            val startTime = aObj.optString("startTime", aObj.optString("start_time", "09:00 AM"))
                            val endTime = aObj.optString("endTime", aObj.optString("end_time", "11:00 AM"))
                            val duration = aObj.optInt("durationMinutes", aObj.optInt("travel_time_minutes", 60))
                            val pickup = aObj.optString("pickupLocation", aObj.optString("pickup_location", ""))
                            val drop = aObj.optString("dropLocation", aObj.optString("drop_location", ""))
                            val transType = aObj.optString("transportType", aObj.optString("transport_mode", "Cab"))
                            val cost = aObj.optDouble("estimatedCost", aObj.optDouble("estimated_cost", 500.0))
                            val status = aObj.optString("status", "recommended")
                            val rawLoc = aObj.optString("place_name", aObj.optString("placeName", aObj.optString("location", "")))
                            val loc = if (rawLoc.isNotBlank()) rawLoc else destination

                            timelineList.add(
                                TimelineItem(
                                    sequence = aObj.optInt("sequence", j + 1),
                                    startTime = startTime,
                                    endTime = endTime,
                                    type = type,
                                    title = title,
                                    location = loc,
                                    description = aObj.optString("description", "Enjoy cultural experience and sights."),
                                    durationMinutes = duration,
                                    transportType = transType,
                                    pickupLocation = pickup,
                                    dropLocation = drop,
                                    bookingRequired = aObj.optBoolean("bookingRequired", true),
                                    bookingId = aObj.optString("bookingId", ""),
                                    vendorId = aObj.optString("vendorId", ""),
                                    status = status,
                                    estimatedCost = cost
                                )
                            )
                        }
                    }

                    val dayNumber = dObj.optInt("day", dObj.optInt("day_number", i + 1))
                    val dayTitle = dObj.optString("title", "").ifBlank {
                        dObj.optString("theme", "").ifBlank { "Day $dayNumber: Exploring $destination" }
                    }

                    list.add(
                        ParsedDay(
                            dayNumber = dayNumber,
                            theme = dayTitle,
                            weather = dObj.optString("weather_summary", "Pleasant & Clear (26°C)"),
                            items = timelineList
                        )
                    )
                }
                return Pair(list, budget)
            }
        } catch (_: Exception) {
            // Fallback below
        }
    }

    // Dynamic Fallback with varied daily schedules and distinct locations & times
    val fallbackDays = createDynamicFallbackDays(destination, daysCount, budget)
    return Pair(fallbackDays, budget)
}

private fun createDynamicFallbackDays(
    destination: String,
    daysCount: Int,
    budget: ItineraryBudget
): List<ParsedDay> {
    val destLower = destination.lowercase(Locale.US)
    val isGoa = destLower.contains("goa")
    val isJaipur = destLower.contains("jaipur")
    val isAgra = destLower.contains("agra")

    data class DaySpec(
        val theme: String,
        val items: List<TimelineItem>
    )

    val specs = mutableListOf<DaySpec>()

    for (d in 1..daysCount.coerceAtLeast(1)) {
        val cycle = (d - 1) % 5
        val spec = when {
            isGoa -> when (cycle) {
                0 -> DaySpec(
                    theme = "North Goa Heritage & Coastal Arrival",
                    items = listOf(
                        TimelineItem(1, "08:30 AM", "09:30 AM", "hotel", "Hotel Check-in & Breakfast", "Taj Fort Aguada / Candolim Hotel, Goa", "Morning breakfast and room orientation.", 60, "Hotel", status = "confirmed", estimatedCost = budget.hotel / daysCount),
                        TimelineItem(2, "09:30 AM", "12:00 PM", "activity", "Fort Aguada & Lighthouse Tour", "Fort Aguada, Candolim", "17th-century Portuguese fortress and Arabian sea views.", 150, "Cab", status = "recommended", estimatedCost = 300.0),
                        TimelineItem(3, "12:45 PM", "02:15 PM", "food", "Coastal Seafood & Goan Curry Lunch", "Britto's Beach Shack, Baga", "Taste authentic Goan thali and fresh coastal delicacies.", 90, "Cab", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(4, "03:45 PM", "06:30 PM", "activity", "Calangute & Baga Water Sports", "Calangute Beach Watersports Hub", "Parasailing, jet ski & speed boat rides.", 165, "Walking", status = "recommended", estimatedCost = 1200.0),
                        TimelineItem(5, "07:30 PM", "09:30 PM", "activity", "Tito's Lane Night Bazaar & Dinner", "Tito's Lane, Baga Beachfront", "Vibrant souvenir shopping, music and dinner.", 120, "Cab", status = "recommended", estimatedCost = 600.0)
                    )
                )
                1 -> DaySpec(
                    theme = "Old Goa Cathedrals & Latin Quarter",
                    items = listOf(
                        TimelineItem(1, "08:30 AM", "09:30 AM", "food", "Latin Quarter Bakery Breakfast", "Fontainhas Heritage Bakery Cafe, Panaji", "Fresh bebinca, pastries and hot espresso.", 60, "Walking", status = "recommended", estimatedCost = 350.0),
                        TimelineItem(2, "10:00 AM", "01:00 PM", "activity", "Basilica of Bom Jesus & Se Cathedral", "Old Goa Heritage Complex", "UNESCO World Heritage baroque architecture and relics.", 180, "Cab", status = "recommended", estimatedCost = 200.0),
                        TimelineItem(3, "01:15 PM", "02:45 PM", "food", "Traditional Indo-Portuguese Lunch", "Viva Panjim Heritage Kitchen", "Regional pork vindaloo, fish recheado and vegetarian curry.", 90, "Cab", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(4, "04:00 PM", "06:30 PM", "activity", "Mandovi River Sunset Cruise & Folk Dance", "Mandovi River Promenade, Panaji", "Scenic evening boat cruise with live Goan folk music.", 150, "Cab", status = "recommended", estimatedCost = 800.0),
                        TimelineItem(5, "07:45 PM", "09:45 PM", "hotel", "Panaji Waterfront Dinner & Leisure", "Waterfront Promenade Bistro, Panaji", "Relaxed riverfront dining and stroll.", 120, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
                2 -> DaySpec(
                    theme = "Island Snorkeling & Vagator Sunset Cliffs",
                    items = listOf(
                        TimelineItem(1, "08:30 AM", "01:00 PM", "activity", "Grand Island Snorkeling & Dolphin Tour", "Grand Island Boat Dock, Vasco", "Speedboat transfer, dolphin watching & coral snorkeling.", 270, "Boat", status = "recommended", estimatedCost = 1800.0),
                        TimelineItem(2, "01:30 PM", "03:00 PM", "food", "Cliffside Bistro & Coconut Drinks", "Thalassa & Curlies Cliff Bistro, Vagator", "Delicious Greek and Mediterranean snacks with ocean view.", 90, "Cab", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "04:30 PM", "07:00 PM", "activity", "Chapora Fort Panoramic Sunset View", "Chapora Fort Cliff, Vagator", "Iconic cliffside ramparts overlooking Vagator coastline.", 150, "Walking", status = "recommended", estimatedCost = 100.0),
                        TimelineItem(4, "08:00 PM", "10:00 PM", "hotel", "Candlelight Beach Dinner & Waves", "Vagator Oceanfront Resort", "Ambient seaside dinner under the stars.", 120, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
                3 -> DaySpec(
                    theme = "Dudhsagar Waterfalls & Spice Plantation",
                    items = listOf(
                        TimelineItem(1, "08:00 AM", "01:00 PM", "activity", "Dudhsagar Waterfalls 4x4 Jeep Safari", "Dudhsagar Wildlife Park, Kulem", "Exhilarating jungle drive through streams to the milky falls.", 300, "Cab", status = "recommended", estimatedCost = 1500.0),
                        TimelineItem(2, "01:30 PM", "03:30 PM", "food", "Sahakari Spice Farm Traditional Buffet", "Sahakari Spice Plantation, Ponda", "Fresh organic buffet lunch on banana leaves amidst vanilla trees.", 120, "Cab", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "04:45 PM", "07:00 PM", "activity", "Miramar Beach Sunset Promenade", "Miramar Coastal Beach Promenade", "Golden hour stroll where the Mandovi River meets the sea.", 135, "Walking", status = "recommended", estimatedCost = 200.0),
                        TimelineItem(4, "08:00 PM", "10:00 PM", "hotel", "Candolim Bistro Dinner & Live Band", "Candolim Main Street Bistro", "Comfortable dinner and relaxing live music session.", 120, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
                else -> DaySpec(
                    theme = "South Goa Serenity & Souvenir Shopping",
                    items = listOf(
                        TimelineItem(1, "09:00 AM", "12:30 PM", "activity", "Palolem & Butterfly Beach Speedboat", "Palolem Beach Harbor, South Goa", "Peaceful crescent bay kayaking and boat excursion.", 210, "Boat", status = "recommended", estimatedCost = 1100.0),
                        TimelineItem(2, "01:00 PM", "02:30 PM", "food", "Dropadi Oceanfront Lunch", "Dropadi Beach Shack, Palolem", "Fresh smoothies and coastal curries overlooking the water.", 90, "Cab", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "03:45 PM", "06:45 PM", "activity", "Anjuna Flea Market Handcrafted Souvenirs", "Anjuna Traditional Flea Market", "Bohemian artifacts, spices, cashews, and handicrafts.", 180, "Walking", status = "recommended", estimatedCost = 500.0),
                        TimelineItem(4, "07:30 PM", "09:45 PM", "hotel", "Farewell Beach Bonfire Celebration Dinner", "South Goa Coastal Resort", "Memorable farewell multicourse dinner by the waves.", 135, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
            }
            isJaipur -> when (cycle) {
                0 -> DaySpec(
                    theme = "Royal Amer Fort & Rajput Heritage",
                    items = listOf(
                        TimelineItem(1, "09:00 AM", "12:30 PM", "activity", "Amber Fort & Sheesh Mahal Exploration", "Amber Fort, Amer, Jaipur", "Hilltop palace of mirrors and ornate Rajput gateways.", 210, "Cab", status = "recommended", estimatedCost = 500.0),
                        TimelineItem(2, "01:00 PM", "02:30 PM", "food", "1135 AD Royal Rajasthani Lunch", "1135 AD Restaurant, Amer Fort", "Dal baati churma and royal thali in palace courtyard.", 90, "Walking", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "03:45 PM", "06:00 PM", "activity", "Hawa Mahal & Jal Mahal Photography Stop", "Hawa Mahal & Jal Mahal Lake", "Capture the iconic honeycomb facade and lake palace.", 135, "Cab", status = "recommended", estimatedCost = 200.0),
                        TimelineItem(4, "07:15 PM", "10:00 PM", "activity", "Chokhi Dhani Ethnic Cultural Village", "Chokhi Dhani Resort, Tonk Road", "Live folk music, puppet show, camel ride and traditional feast.", 165, "Cab", status = "confirmed", estimatedCost = 1000.0)
                    )
                )
                1 -> DaySpec(
                    theme = "City Palace, Jantar Mantar & Pink City Bazaars",
                    items = listOf(
                        TimelineItem(1, "09:30 AM", "12:00 PM", "activity", "City Palace & Royal Armory Museum", "City Palace Complex, Jaipur", "Courtyards, peacock gates and royal textile museum.", 150, "Cab", status = "recommended", estimatedCost = 700.0),
                        TimelineItem(2, "12:30 PM", "02:00 PM", "food", "LMB Heritage Johari Bazaar Lunch", "LMB Johari Bazaar", "Legendary kachoris, ghewar and vegetarian feast.", 90, "Walking", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "02:45 PM", "05:30 PM", "activity", "Jantar Mantar UNESCO Observatory & Bapu Bazaar", "Jantar Mantar & Bapu Bazaar", "Giant stone sundials and authentic mojari leather footwear.", 165, "Walking", status = "recommended", estimatedCost = 400.0),
                        TimelineItem(4, "06:30 PM", "09:30 PM", "hotel", "Nahargarh Fort Sunset Panorama Dinner", "Padao Restaurant, Nahargarh Fort", "Cliffside twilight view overlooking the glowing Pink City.", 180, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
                else -> DaySpec(
                    theme = "Jaigarh Fort & Albert Hall Artisans",
                    items = listOf(
                        TimelineItem(1, "09:00 AM", "12:00 PM", "activity", "Jaigarh Fort & Jaivana Cannon Tour", "Jaigarh Fort Hilltop, Jaipur", "World's largest wheeled cannon and military ramparts.", 180, "Cab", status = "recommended", estimatedCost = 300.0),
                        TimelineItem(2, "12:30 PM", "02:00 PM", "food", "Tapri Central Rooftop Lunch", "Tapri Central, Central Park", "Refreshing chai, gourmet street food and garden views.", 90, "Cab", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "03:00 PM", "05:30 PM", "activity", "Albert Hall Museum & Blue Pottery Walk", "Albert Hall State Museum, Jaipur", "Indo-Saracenic museum holding rare miniature paintings.", 150, "Cab", status = "recommended", estimatedCost = 300.0),
                        TimelineItem(4, "06:30 PM", "09:30 PM", "hotel", "Heritage Haveli Farewell Dinner", "Heritage Haveli, Jaipur", "Live sitar music and celebratory multicourse meal.", 180, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
            }
            isAgra -> when (cycle) {
                0 -> DaySpec(
                    theme = "Taj Mahal Sunrise & Mughal Grandeur",
                    items = listOf(
                        TimelineItem(1, "06:00 AM", "09:30 AM", "activity", "Taj Mahal Sunrise Guided Exploration", "Taj Mahal East Gate Complex, Agra", "Watch the marble wonder glow during morning sunrise.", 210, "Cab", status = "recommended", estimatedCost = 1100.0),
                        TimelineItem(2, "12:30 PM", "02:00 PM", "food", "Pinch of Spice Authentic Mughlai Lunch", "Pinch of Spice, Fatehabad Road, Agra", "Fragrant biryanis, tandoori specialties and curries.", 90, "Cab", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "03:00 PM", "05:30 PM", "activity", "Agra Fort Royal Sandstone Palaces", "Agra Fort Monument", "Jahangir Palace and Diwan-i-Khas overlooking river.", 150, "Cab", status = "recommended", estimatedCost = 650.0),
                        TimelineItem(4, "06:00 PM", "09:00 PM", "hotel", "Mehtab Bagh Moonlight Garden Sunset & Dinner", "Mehtab Bagh & Hotel Dining, Agra", "River reflection sunset and Mughlai dinner.", 180, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
                else -> DaySpec(
                    theme = "Fatehpur Sikri & Artisan Petha Trail",
                    items = listOf(
                        TimelineItem(1, "08:30 AM", "01:00 PM", "activity", "Fatehpur Sikri & Buland Darwaza Excursion", "Fatehpur Sikri Royal Complex", "Akbar's ghost capital and grand 54-meter triumphal arch.", 270, "Cab", status = "recommended", estimatedCost = 600.0),
                        TimelineItem(2, "01:30 PM", "03:00 PM", "food", "Dasaprakash Heritage Feast", "Dasaprakash, Sadar Bazaar, Agra", "Crisp dosas, thalis, and desserts at iconic eatery.", 90, "Cab", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "04:00 PM", "06:00 PM", "activity", "Tomb of I'timad-ud-Daulah (Baby Taj)", "Baby Taj Heritage Site, Agra", "Exquisite pietra dura marble inlay on riverbank.", 120, "Cab", status = "recommended", estimatedCost = 300.0),
                        TimelineItem(4, "07:00 PM", "09:30 PM", "hotel", "Sadar Bazaar Petha Souvenir Trail & Dinner", "Sadar Bazaar Main Street, Agra", "Shop for authentic Agra petha, handicrafts and dinner.", 150, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
            }
            else -> when (cycle) {
                0 -> DaySpec(
                    theme = "Arrival, Landmark Orientation & Evening Bazaars",
                    items = listOf(
                        TimelineItem(1, "08:30 AM", "09:30 AM", "hotel", "Hotel Check-in & Orientation", "Central Landmark Hotel, $destination", "Welcome refreshments and travel check-in.", 60, "Hotel", status = "confirmed", estimatedCost = budget.hotel / daysCount),
                        TimelineItem(2, "10:00 AM", "01:00 PM", "activity", "Historic Center & Iconic Heritage Walk", "$destination Historic Center", "Guided discovery of key monuments and architecture.", 180, "Cab", status = "recommended", estimatedCost = 400.0),
                        TimelineItem(3, "01:15 PM", "02:30 PM", "food", "Regional Specialty Lunch", "$destination Traditional Dining Hub", "Authentic regional lunch featuring chef specialties.", 75, "Walking", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(4, "04:00 PM", "06:30 PM", "activity", "Sunset Viewpoint & Cultural Street Market", "$destination Sunset Promenade", "Panoramic viewpoints, evening breeze and shopping.", 150, "Cab", status = "recommended", estimatedCost = 300.0),
                        TimelineItem(5, "07:30 PM", "09:30 PM", "hotel", "Hotel Leisure Dinner", "Central Landmark Hotel, $destination", "Recharge with a relaxing dinner at your base.", 120, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
                1 -> DaySpec(
                    theme = "Ancient Heritage, Art Museums & Craft Workshops",
                    items = listOf(
                        TimelineItem(1, "09:00 AM", "12:00 PM", "activity", "Royal Fort & Heritage Museum Guided Tour", "$destination Royal Heritage Complex", "Historic royal artifacts, armory and art galleries.", 180, "Cab", status = "recommended", estimatedCost = 500.0),
                        TimelineItem(2, "12:30 PM", "02:00 PM", "food", "Heritage Garden Kitchen Lunch", "$destination Heritage Garden Kitchen", "Open-air garden dining featuring fresh regional recipes.", 90, "Cab", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "03:00 PM", "05:30 PM", "activity", "Master Artisan Workshop & Souvenir Bazaar", "$destination Central Artisan Bazaar", "Watch master craftsmen and browse certified crafts.", 150, "Walking", status = "recommended", estimatedCost = 400.0),
                        TimelineItem(4, "06:30 PM", "09:30 PM", "hotel", "Cultural Folk Dance & Grand Buffet Dinner", "$destination Cultural Amphitheater", "Live regional music and banquet under the stars.", 180, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
                2 -> DaySpec(
                    theme = "Nature Escapes, Lakefront & Outdoor Trails",
                    items = listOf(
                        TimelineItem(1, "08:30 AM", "12:00 PM", "activity", "Panoramic Nature Reserve & Forest Trail", "$destination Scenic Nature Reserve", "Morning nature walk with scenic mountain/lake lookout.", 210, "Cab", status = "recommended", estimatedCost = 600.0),
                        TimelineItem(2, "12:30 PM", "02:00 PM", "food", "Hillside Panorama Cafe & Refreshments", "$destination Panorama Cafe", "Sweeping landscape views and farm-fresh lunch.", 90, "Cab", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "03:30 PM", "06:30 PM", "activity", "Waterfront Promenade & Boat Ride", "$destination Waterfront Harbor", "Gentle boat ride and peaceful golden hour sunset stroll.", 180, "Boat", status = "recommended", estimatedCost = 700.0),
                        TimelineItem(4, "07:30 PM", "09:45 PM", "hotel", "Lakeside Lantern Dinner", "$destination Lakeside Restaurant", "Cozy dinner with ambient string lighting and acoustic vibes.", 135, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
                3 -> DaySpec(
                    theme = "Spiritual Shrines, Hidden Gems & Food Trails",
                    items = listOf(
                        TimelineItem(1, "09:00 AM", "12:00 PM", "activity", "Sacred Shrines & Architectural Temples", "$destination Sacred Heritage Shrines", "Intricate stone carvings and sacred spiritual courtyards.", 180, "Cab", status = "recommended", estimatedCost = 200.0),
                        TimelineItem(2, "12:30 PM", "02:00 PM", "food", "Old Town Street Food & Sweet Trail", "$destination Old Town Food Trail", "Sample iconic street snacks, sweet delicacies and chai.", 90, "Walking", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "03:30 PM", "06:00 PM", "activity", "Botanical Conservatory & Sculpture Park", "$destination Botanical Gardens", "Lush flora, exotic trees, fountains and sculpture walks.", 150, "Cab", status = "recommended", estimatedCost = 300.0),
                        TimelineItem(4, "07:00 PM", "09:30 PM", "hotel", "Fine Dining & Chef's Special Evening", "Rooftop Grand Bistro, $destination", "Multicourse tasting menu crafted with local spices.", 150, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
                else -> DaySpec(
                    theme = "Artisan Souvenirs & Farewell Celebration",
                    items = listOf(
                        TimelineItem(1, "09:30 AM", "12:30 PM", "activity", "Local Craft Guilds & Shopping District", "$destination Main Commercial Quarter", "Last chance to pick up authenticated souvenirs and apparel.", 180, "Walking", status = "recommended", estimatedCost = 500.0),
                        TimelineItem(2, "01:00 PM", "02:30 PM", "food", "Celebration Farewell Lunch", "$destination Classic Bistro", "Comfort food, desserts, and celebration toasts.", 90, "Cab", status = "recommended", estimatedCost = budget.food / daysCount),
                        TimelineItem(3, "03:30 PM", "06:00 PM", "activity", "City Lookout Vantage Point", "$destination City Viewpoint Peak", "Panoramic 360-degree photography of the entire city.", 150, "Cab", status = "recommended", estimatedCost = 200.0),
                        TimelineItem(4, "07:30 PM", "10:00 PM", "hotel", "Grand Farewell Banquet", "Hotel Grand Ballroom, $destination", "Celebrate the conclusion of a wonderful trip.", 150, "Cab", status = "confirmed", estimatedCost = budget.hotel / daysCount)
                    )
                )
            }
        }
        specs.add(spec)
    }

    return specs.mapIndexed { index, spec ->
        ParsedDay(
            dayNumber = index + 1,
            theme = "Day ${index + 1}: ${spec.theme}",
            weather = if (index % 2 == 0) "Sunny & Pleasant (27°C)" else "Clear Skies & Breezy (25°C)",
            items = spec.items
        )
    }
}
