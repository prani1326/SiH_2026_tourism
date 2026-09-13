package com.touristapp.presentation.trips

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.touristapp.data.remote.ApiResult
import com.touristapp.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPlannerScreen(
    onBackClick: () -> Unit,
    onPlanGenerated: (String) -> Unit,
    initialDestination: String? = null,
    viewModel: TripsViewModel = viewModel()
) {
    // 1. Destination Selection
    val canonicalDestinations = listOf(
        "Goa" to "Beaches & Nightlife",
        "Jaipur" to "Royal Palaces & Forts",
        "Agra" to "Taj Mahal & Heritage",
        "Delhi" to "Capital & Historical Monuments",
        "Mumbai" to "Bollywood & Marine Drive",
        "Udaipur" to "City of Lakes & Romance",
        "Manali" to "Snow Peaks & Adventure",
        "Rishikesh" to "Yoga & River Rafting",
        "Shimla" to "Colonial Charm & Hills",
        "Kochi" to "Backwaters & Spice Markets",
        "Bangalore" to "Gardens & Tech Hub",
        "Varanasi" to "Spiritual Ghats & Ganga Aarti",
        "Kolkata" to "Culture & Grand Colonial Art",
        "Chennai" to "Temples & Marina Beach",
        "Amritsar" to "Golden Temple & Punjabi Cuisine",
        "Jodhpur" to "Blue City & Mehrangarh Fort",
        "Mysuru" to "Grand Palaces & Silk Bazaars",
        "Darjeeling" to "Tea Gardens & Kanchenjunga",
        "Puducherry" to "French Quarter & Serene Coast",
        "Hyderabad" to "Charminar & Nizami Biryani"
    )
    var selectedDestination by remember(initialDestination) {
        mutableStateOf(initialDestination?.ifBlank { "Goa" } ?: "Goa")
    }
    var destinationSearch by remember { mutableStateOf("") }

    // 2. Travel Dates & Duration Sync
    var startDate by remember { mutableStateOf(LocalDate.now().plusDays(7)) }
    var endDate by remember { mutableStateOf(LocalDate.now().plusDays(11)) }
    var numDays by remember { mutableFloatStateOf(5f) }
    val displayDateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US) }
    val isoDateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var dateValidationError by remember { mutableStateOf<String?>(null) }

    // 3. Travelers
    var travelerCount by remember { mutableIntStateOf(2) }
    var travelStyle by remember { mutableStateOf("Couple") }
    val travelerTypes = listOf(
        Triple("Solo", 1, "Personal discovery"),
        Triple("Couple", 2, "Romantic & relaxed"),
        Triple("Family", 4, "Family-friendly & comfort"),
        Triple("Group", 6, "Social & high adventure")
    )

    // 4. Budget
    var selectedBudget by remember { mutableDoubleStateOf(15000.0) }
    val budgetPresets = listOf(3000.0, 7000.0, 15000.0, 25000.0, 50000.0)

    // 5. Interests (Multi-select)
    val allInterests = listOf(
        "Beaches", "Heritage & Culture", "Nature & Wildlife",
        "Adventure", "Spiritual", "Food & Dining", "Shopping", "Nightlife", "Relaxation"
    )
    val selectedInterests = remember { mutableStateListOf("Beaches", "Adventure", "Food & Dining") }

    // 6. Activities (Multi-select)
    val allActivities = listOf(
        "Water Sports", "Sightseeing", "Trekking & Hiking", "Monument Tours",
        "Culinary Trails", "Photography", "Cruise / Boat Rides", "Yoga & Wellness"
    )
    val selectedActivities = remember { mutableStateListOf("Water Sports", "Sightseeing") }

    // 7. Food Preference
    var selectedFood by remember { mutableStateOf("Local Food") }
    val foodOptions = listOf("Local Food", "Pure Vegetarian", "Non-Vegetarian", "Jain", "Street Food")

    // 8. Hotel Preference
    var selectedHotel by remember { mutableStateOf("3 Star") }
    val hotelOptions = listOf("Budget / Hostel", "3 Star", "4 Star", "5 Star Luxury", "Heritage Boutique")

    // 9. Transport Preference
    var selectedTransport by remember { mutableStateOf("Cab") }
    val transportOptions = listOf("Cab", "Self-Drive Rental", "Metro / Transit", "Auto & Walking")

    // 10. Travel Pace
    var selectedPace by remember { mutableStateOf("Balanced") }
    val paceOptions = listOf("Relaxed", "Balanced", "Fast-Paced")

    // State & Async
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationStage by viewModel.generationStage.collectAsState()
    val planPreview by viewModel.aiPlanPreview.collectAsState()
    val planState by viewModel.aiPlan.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var lockedEligibility by remember { mutableStateOf<com.touristapp.data.models.TripEligibilityResponse?>(null) }
    var showLockSheet by remember { mutableStateOf(false) }
    var showKycDialogFromLock by remember { mutableStateOf(false) }

    if (showLockSheet) {
        com.touristapp.presentation.trips.components.TripPlanningLockBottomSheet(
            eligibility = lockedEligibility,
            onDismiss = { showLockSheet = false },
            onCompleteKycClick = {
                showLockSheet = false
                showKycDialogFromLock = true
            },
            onCompleteProfileClick = {
                showLockSheet = false
                onBackClick()
            }
        )
    }

    if (showKycDialogFromLock) {
        com.touristapp.presentation.profile.components.KycVerificationDialog(
            onDismiss = { showKycDialogFromLock = false },
            onVerificationSuccess = {
                showKycDialogFromLock = false
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("KYC Verified! You can now generate your trip.")
                }
            }
        )
    }

    LaunchedEffect(planState) {
        when (planState) {
            is ApiResult.Success -> {
                val data = (planState as ApiResult.Success).data
                val tripId = data["id"]?.jsonPrimitive?.content ?: data["trip_id"]?.jsonPrimitive?.content
                if (!tripId.isNullOrEmpty()) {
                    onPlanGenerated(tripId)
                }
            }
            is ApiResult.Exception -> {
                val ex = (planState as ApiResult.Exception).e
                if (ex.localizedMessage?.contains("PROFILE_KYC_REQUIRED") == true || ex.localizedMessage?.contains("KYC") == true) {
                    lockedEligibility = com.touristapp.data.models.TripEligibilityResponse(
                        eligible = false,
                        profile_complete = false,
                        kyc_verified = false,
                        kyc_status = "NOT_STARTED",
                        missing_requirements = listOf("KYC_VERIFICATION")
                    )
                    showLockSheet = true
                } else {
                    snackbarHostState.showSnackbar(ex.localizedMessage ?: "Could not generate trip plan. Please try again.")
                }
            }
            else -> {}
        }
    }

    // Material 3 Date Picker: Start Date (FROM)
    if (showStartDatePicker) {
        val initialStartMillis = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialStartMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val todayUtc = LocalDate.now().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                    return utcTimeMillis >= todayUtc
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        startDate = selected
                        if (endDate.isBefore(startDate)) {
                            endDate = startDate.plusDays(numDays.toLong().coerceAtLeast(1L) - 1)
                        }
                        val calculated = (ChronoUnit.DAYS.between(startDate, endDate) + 1).coerceIn(1, 14)
                        numDays = calculated.toFloat()
                        dateValidationError = null
                    }
                    showStartDatePicker = false
                }) {
                    Text("OK", color = TravelPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Material 3 Date Picker: End Date (TO)
    if (showEndDatePicker) {
        val initialEndMillis = endDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialEndMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val startUtc = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                    return utcTimeMillis >= startUtc
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        if (selected.isBefore(startDate)) {
                            dateValidationError = "Return date must be after the start date."
                        } else {
                            endDate = selected
                            val calculated = (ChronoUnit.DAYS.between(startDate, endDate) + 1).coerceIn(1, 14)
                            numDays = calculated.toFloat()
                            dateValidationError = null
                        }
                    }
                    showEndDatePicker = false
                }) {
                    Text("OK", color = TravelPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            if (planPreview != null) "AI Trip Preview" else "AI Trip Planner",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            if (planPreview != null) "Review schedule before finalizing" else "100% Selection-Based • Powered by Gemini AI",
                            style = MaterialTheme.typography.bodySmall,
                            color = TravelPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (planPreview != null) {
                            viewModel.clearPreview()
                        } else {
                            onBackClick()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    if (planPreview != null) {
                        TextButton(onClick = { viewModel.clearPreview() }) {
                            Text("Edit Options", color = TravelPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 12.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    if (planPreview != null) {
                        Button(
                            onClick = {
                                viewModel.finalizeTripFromPreview(
                                    style = travelStyle,
                                    interests = selectedInterests.toList(),
                                    budget = selectedBudget,
                                    startDate = startDate.format(isoDateFormatter),
                                    endDate = endDate.format(isoDateFormatter),
                                    onSuccess = { tripId ->
                                        onPlanGenerated(tripId)
                                    }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                            enabled = !isGenerating
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "GENERATE MY TRIP",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                if (selectedDestination.isBlank()) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Please select a destination.")
                                    }
                                    return@Button
                                }
                                if (endDate.isBefore(startDate)) {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Return date must be after the start date.")
                                    }
                                    return@Button
                                }
                                viewModel.planWithAi(
                                    destination = selectedDestination.trim(),
                                    days = numDays.toInt(),
                                    travelers = travelerCount,
                                    budget = selectedBudget,
                                    interests = selectedInterests.toList(),
                                    activities = selectedActivities.toList(),
                                    foodPreference = selectedFood,
                                    hotelPreference = selectedHotel,
                                    transportPreference = selectedTransport,
                                    travelPace = selectedPace,
                                    style = travelStyle,
                                    walkingTolerance = "Moderate",
                                    startDate = startDate.format(isoDateFormatter),
                                    endDate = endDate.format(isoDateFormatter)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                            enabled = selectedDestination.isNotBlank() && !isGenerating
                        ) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "PLAN WITH AI",
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (planPreview != null) {
                // AI PLAN PREVIEW VIEW
                val preview = planPreview!!
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    // Preview Header Card
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.isDarkMode) MaterialTheme.colorScheme.surfaceVariant else TravelNavy),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
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
                                            "AI GENERATED DRAFT",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        "₹${preview.totalEstimatedCost.toInt()}",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    preview.title.ifBlank { preview.tripTitle ?: "${preview.destination} Trip Plan" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${preview.destination} • ${startDate.format(displayDateFormatter)} - ${endDate.format(displayDateFormatter)} (${preview.durationDays} Days) • $travelerCount Travelers",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    preview.summary ?: "AI generated itinerary customized to your selections.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }

                    // Preferences Summary Chips
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Hotel, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(selectedHotel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(selectedTransport, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Restaurant, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(selectedFood, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.Speed, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(selectedPace, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }

                    // Budget Breakdown
                    if (preview.budgetBreakdown != null) {
                        item {
                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, TravelPrimary.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Estimated Cost Breakdown", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val bb = preview.budgetBreakdown!!
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("🏨 Hotel / Stay:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        Text("₹${bb.hotel.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TravelDarkPrimary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("🍽️ Food & Dining:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        Text("₹${bb.food.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TravelDarkPrimary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("🚗 Transport & Cabs:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        Text("₹${bb.transport.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TravelDarkPrimary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("🎟️ Activities & Tickets:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        Text("₹${bb.activities.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TravelDarkPrimary)
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("🛍️ Misc / Shopping:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                        Text("₹${bb.miscellaneous.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TravelDarkPrimary)
                                    }
                                }
                            }
                        }
                    }

                    // Day by Day Preview
                    items(preview.days) { day ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, TravelPrimary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.brandPillBackground
                                    ) {
                                        Text(
                                            "Day ${day.dayNumber}",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TravelDarkPrimary
                                        )
                                    }
                                    if (!day.date.isNullOrBlank()) {
                                        Text(
                                            day.date!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    day.title ?: (day.theme ?: "Exploring ${preview.destination}"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Chronological Items in this day
                                day.activities.forEach { act ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            act.startTime,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TravelPrimary,
                                            modifier = Modifier.width(68.dp)
                                        )
                                        val icon = when ((act.activityType ?: act.timeSlot).lowercase()) {
                                            "transport" -> Icons.Filled.DirectionsCar
                                            "hotel" -> Icons.Filled.Hotel
                                            "food", "dining" -> Icons.Filled.Restaurant
                                            else -> Icons.Filled.Place
                                        }
                                        Icon(
                                            icon,
                                            contentDescription = null,
                                            tint = if ((act.activityType ?: "").equals("transport", ignoreCase = true)) TravelWarning else TravelPrimary,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(top = 2.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                act.title,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            val loc = act.placeName ?: ""
                                            if (loc.isNotBlank()) {
                                                Text(
                                                    loc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // PREFERENCES FORM VIEW
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                ) {
                    // Header Banner
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = if (MaterialTheme.colorScheme.isDarkMode) MaterialTheme.colorScheme.surfaceVariant else TravelNavy),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(TravelPrimary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Zero-Typing Smart Planning", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("Select your preferences below to craft a complete itinerary", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    // 1. SELECT DESTINATION
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(icon = Icons.Filled.Place, stepNumber = 1, title = "Select Destination")
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = destinationSearch,
                                    onValueChange = { destinationSearch = it },
                                    placeholder = { Text("Filter destinations (e.g. Goa, Jaipur, Agra)") },
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TravelPrimary) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                val filtered = canonicalDestinations.filter {
                                    destinationSearch.isBlank() || it.first.contains(destinationSearch, ignoreCase = true) || it.second.contains(destinationSearch, ignoreCase = true)
                                }

                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(filtered) { (city, tag) ->
                                        val isSelected = selectedDestination.equals(city, ignoreCase = true)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedDestination = city },
                                            label = {
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Text(city, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(tag, fontSize = 10.sp, color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                                }
                                            },
                                            leadingIcon = {
                                                if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TravelPrimary,
                                                selectedLabelColor = Color.White,
                                                selectedLeadingIconColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. TRAVEL DATES
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(icon = Icons.Filled.CalendarMonth, stepNumber = 2, title = "Travel Dates")
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "When are you going?",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // FROM Date Card
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showStartDatePicker = true },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, TravelPrimary.copy(alpha = 0.35f)),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.brandPillBackground.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                "FROM",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TravelPrimary,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                startDate.format(displayDateFormatter),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = TravelPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Start Date",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }

                                    // TO Date Card
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showEndDatePicker = true },
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.5.dp, TravelPrimary.copy(alpha = 0.35f)),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.brandPillBackground.copy(alpha = 0.5f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                "TO",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = TravelPrimary,
                                                letterSpacing = 1.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                endDate.format(displayDateFormatter),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = TravelPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Return Date",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (dateValidationError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        dateValidationError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // 3. TRIP DURATION & DATES SYNC
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(
                                    icon = Icons.Filled.DateRange,
                                    stepNumber = 3,
                                    title = "Trip Duration: ${numDays.toInt()} Days"
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "Optimal stay recommended for $selectedDestination: ${numDays.toInt()} Days",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Slider(
                                    value = numDays,
                                    onValueChange = { newDays ->
                                        numDays = newDays
                                        endDate = startDate.plusDays(newDays.toLong().coerceAtLeast(1L) - 1)
                                        dateValidationError = null
                                    },
                                    valueRange = 1f..14f,
                                    steps = 12,
                                    colors = SliderDefaults.colors(
                                        thumbColor = TravelPrimary,
                                        activeTrackColor = TravelPrimary
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    listOf(1, 3, 5, 7, 10, 14).forEach { d ->
                                        Text(
                                            text = "${d}D",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (numDays.toInt() == d) FontWeight.Bold else FontWeight.Normal,
                                            color = if (numDays.toInt() == d) TravelPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.clickable {
                                                numDays = d.toFloat()
                                                endDate = startDate.plusDays(d.toLong() - 1)
                                                dateValidationError = null
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 4. TRAVELERS
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(icon = Icons.Filled.People, stepNumber = 4, title = "Number of Travelers")
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    travelerTypes.forEach { (type, count, _) ->
                                        val isSelected = travelStyle == type
                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    travelStyle = type
                                                    travelerCount = count
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            border = if (isSelected) BorderStroke(2.dp, TravelPrimary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.brandPillBackground else MaterialTheme.colorScheme.surface
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    type,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = if (isSelected) TravelDarkPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    "$count Person${if (count > 1) "s" else ""}",
                                                    fontSize = 11.sp,
                                                    color = if (isSelected) TravelPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. BUDGET SELECTION
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(
                                    icon = Icons.Filled.AccountBalanceWallet,
                                    stepNumber = 5,
                                    title = "Estimated Budget: ₹${selectedBudget.toInt()}"
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                // Preset chips
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(budgetPresets) { preset ->
                                        val isSelected = selectedBudget == preset
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedBudget = preset },
                                            label = { Text("₹${preset.toInt()}") },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TravelPrimary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Slider(
                                    value = selectedBudget.toFloat().coerceIn(3000f, 50000f),
                                    onValueChange = { selectedBudget = ((it / 500).toInt() * 500.0).coerceIn(3000.0, 50000.0) },
                                    valueRange = 3000f..50000f,
                                    colors = SliderDefaults.colors(thumbColor = TravelPrimary, activeTrackColor = TravelPrimary)
                                )
                            }
                        }
                    }

                    // 6. INTERESTS
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(icon = Icons.Filled.Favorite, stepNumber = 6, title = "Select Interests (Multi-select)")
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    allInterests.chunked(2).forEach { rowList ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            rowList.forEach { interest ->
                                                val isSelected = selectedInterests.contains(interest)
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        if (isSelected) selectedInterests.remove(interest)
                                                        else selectedInterests.add(interest)
                                                    },
                                                    label = { Text(interest, fontSize = 12.sp) },
                                                    leadingIcon = {
                                                        if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.brandPillBackground,
                                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }
                                            if (rowList.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 7. ACTIVITIES
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(icon = Icons.Filled.Explore, stepNumber = 7, title = "Select Activities (Multi-select)")
                                Spacer(modifier = Modifier.height(10.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    allActivities.chunked(2).forEach { rowList ->
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            rowList.forEach { act ->
                                                val isSelected = selectedActivities.contains(act)
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        if (isSelected) selectedActivities.remove(act)
                                                        else selectedActivities.add(act)
                                                    },
                                                    label = { Text(act, fontSize = 12.sp) },
                                                    leadingIcon = {
                                                        if (isSelected) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.brandPillBackground,
                                                        selectedLabelColor = MaterialTheme.colorScheme.primary,
                                                        selectedLeadingIconColor = MaterialTheme.colorScheme.primary
                                                    )
                                                )
                                            }
                                            if (rowList.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 8. FOOD PREFERENCES
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(icon = Icons.Filled.Restaurant, stepNumber = 8, title = "Food Preferences")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(foodOptions) { food ->
                                        val isSelected = selectedFood == food
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedFood = food },
                                            label = { Text(food) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TravelPrimary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 9. HOTEL PREFERENCE
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(icon = Icons.Filled.Hotel, stepNumber = 9, title = "Hotel Preference")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(hotelOptions) { hotel ->
                                        val isSelected = selectedHotel == hotel
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedHotel = hotel },
                                            label = { Text(hotel) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TravelPrimary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 10. TRANSPORT PREFERENCE
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(icon = Icons.Filled.DirectionsCar, stepNumber = 10, title = "Transport Preference")
                                Spacer(modifier = Modifier.height(10.dp))
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(transportOptions) { transport ->
                                        val isSelected = selectedTransport == transport
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedTransport = transport },
                                            label = { Text(transport) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TravelPrimary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 11. TRAVEL PACE
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                SectionHeader(icon = Icons.Filled.Speed, stepNumber = 11, title = "Travel Pace")
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    paceOptions.forEach { pace ->
                                        val isSelected = selectedPace == pace
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedPace = pace },
                                            label = { Text(pace) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = TravelPrimary,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 12. REVIEW SELECTIONS SUMMARY CARD
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.brandPillBackground),
                            border = BorderStroke(1.5.dp, TravelPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Review Your Selections", fontWeight = FontWeight.Bold, color = TravelDarkPrimary, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(12.dp))

                                SelectionSummaryRow(label = "Destination", value = selectedDestination)
                                SelectionSummaryRow(label = "Travel Dates", value = "${startDate.format(displayDateFormatter)} → ${endDate.format(displayDateFormatter)}")
                                SelectionSummaryRow(label = "Duration", value = "${numDays.toInt()} Days")
                                SelectionSummaryRow(label = "Travelers", value = "$travelerCount ($travelStyle)")
                                SelectionSummaryRow(label = "Budget", value = "₹${selectedBudget.toInt()}")
                                SelectionSummaryRow(label = "Interests", value = selectedInterests.joinToString(", "))
                                SelectionSummaryRow(label = "Activities", value = selectedActivities.joinToString(", "))
                                SelectionSummaryRow(label = "Food", value = selectedFood)
                                SelectionSummaryRow(label = "Hotel", value = selectedHotel)
                                SelectionSummaryRow(label = "Transport", value = selectedTransport)
                                SelectionSummaryRow(label = "Pace", value = selectedPace)
                            }
                        }
                    }
                }
            }

            // Generation Status Progress Overlay
            AnimatedVisibility(
                visible = isGenerating,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = TravelPrimary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "Gemini AI Trip Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = generationStage.ifBlank { "Finalizing your trip plan in Firestore..." },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TravelPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = TravelPrimary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, stepNumber: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(TravelPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text("$stepNumber", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(icon, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SelectionSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )
        Text(
            value.ifBlank { "None" },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = TravelDarkPrimary
        )
    }
}
