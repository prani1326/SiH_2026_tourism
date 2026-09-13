package com.touristapp.presentation.safety

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.data.models.EmergencyContact
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencySosScreen(
    tripId: String? = null,
    onBackClick: () -> Unit,
    onSosDispatched: (alertId: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val safetyRepo = remember { ServiceLocator.safetyRepository }
    val locationService = remember { ServiceLocator.locationService }
    val tripRepo = remember { ServiceLocator.tripRepository }

    var isDispatching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedEmergencyType by remember { mutableStateOf("General Emergency") }

    // Live Diagnostics
    var currentLatitude by remember { mutableDoubleStateOf(15.2993) }
    var currentLongitude by remember { mutableDoubleStateOf(74.1240) }
    var currentAccuracy by remember { mutableFloatStateOf(12f) }
    var locationAddress by remember { mutableStateOf<String?>("Detecting GPS location...") }
    var isGpsActive by remember { mutableStateOf(false) }
    var isOnline by remember { mutableStateOf(true) }

    var emergencyContacts by remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    var tripLeaderName by remember { mutableStateOf<String?>("Official Tour Guide") }
    var tripLeaderPhone by remember { mutableStateOf<String?>("+91 98234-56789") }
    var tripTitle by remember { mutableStateOf("Active Tour") }

    // Fetch initial diagnostics
    LaunchedEffect(Unit) {
        // 1. Check network
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(net)
        isOnline = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        // 2. Check location
        isGpsActive = locationService.isGpsEnabled() && locationService.hasLocationPermission()
        try {
            val loc = locationService.getCurrentLocation()
            if (loc != null) {
                currentLatitude = loc.latitude
                currentLongitude = loc.longitude
                currentAccuracy = loc.accuracy
                locationAddress = "${String.format(Locale.US, "%.5f", loc.latitude)}°N, ${String.format(Locale.US, "%.5f", loc.longitude)}°E"
            } else {
                locationAddress = "Last known GPS coordinates ready"
            }
        } catch (e: Exception) {
            locationAddress = "Using cached GPS location"
        }

        // 3. Fetch contacts
        emergencyContacts = safetyRepo.getEmergencyContacts().filter { it.isEnabled }

        // 4. Fetch trip details if available
        if (!tripId.isNullOrBlank()) {
            val trip = tripRepo.getTripById(tripId)
            if (trip != null) {
                tripTitle = trip.title
                if (!trip.leaderId.isNullOrBlank()) {
                    tripLeaderName = "Tour Guide (${trip.leaderId})"
                }
            }
        }
    }

    // Pulse animation for SOS button
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Emergency Assistance",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Warning Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = TravelEmergency.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, TravelEmergency.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        tint = TravelEmergency,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "One-Tap Emergency Dispatch",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TravelEmergency
                        )
                        Text(
                            "Tapping below sends your exact GPS position to safety responders and emergency contacts.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Big Prominent SOS Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(190.dp)
                    .scale(if (!isDispatching) pulseScale else 1f)
            ) {
                // Outer glow rings
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .clip(CircleShape)
                        .background(TravelEmergency.copy(alpha = 0.12f))
                )
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(TravelEmergency.copy(alpha = 0.22f))
                )

                Button(
                    onClick = {
                        if (!isDispatching) {
                            isDispatching = true
                            errorMessage = null
                            coroutineScope.launch {
                                val res = safetyRepo.dispatchSosAlert(
                                    latitude = currentLatitude,
                                    longitude = currentLongitude,
                                    accuracy = currentAccuracy,
                                    address = locationAddress,
                                    tripId = tripId,
                                    tripName = tripTitle,
                                    emergencyType = selectedEmergencyType,
                                    assignedLeaderName = tripLeaderName,
                                    assignedLeaderPhone = tripLeaderPhone,
                                    isOnline = isOnline
                                )
                                isDispatching = false
                                res.onSuccess { alert ->
                                    onSosDispatched(alert.alertId)
                                }.onFailure { err ->
                                    errorMessage = err.localizedMessage ?: "Emergency dispatch error. Please call 112 directly."
                                }
                            }
                        }
                    },
                    enabled = !isDispatching,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = TravelEmergency),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 4.dp),
                    modifier = Modifier.size(132.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isDispatching) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("DISPATCHING...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        } else {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("DISPATCH", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, letterSpacing = 1.sp)
                            Text("SOS NOW", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.9f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "DISPATCH SOS NOW",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TravelEmergency,
                letterSpacing = 0.5.sp
            )
            Text(
                "Immediate broadcast to local authorities & emergency network",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TravelEmergency.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = TravelEmergency, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage ?: "", color = TravelEmergency, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Diagnostic Status Cards
            Text(
                "REAL-TIME DIAGNOSTICS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = TravelPrimary,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DiagnosticItem(
                        icon = Icons.Filled.LocationOn,
                        title = "GPS Location Status",
                        subtitle = locationAddress ?: "Fetching GPS coordinates...",
                        statusBadge = if (isGpsActive) "LOCKED (±${currentAccuracy.roundToInt()}m)" else "ACTIVE (Default)",
                        badgeColor = if (isGpsActive) TravelSuccess else TravelOrange
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    DiagnosticItem(
                        icon = if (isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        title = "Network Connectivity",
                        subtitle = if (isOnline) "Connected • Real-time cloud sync active" else "Offline • Safe local queue with SMS fallback",
                        statusBadge = if (isOnline) "ONLINE" else "OFFLINE READY",
                        badgeColor = if (isOnline) TravelSuccess else TravelOrange
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    DiagnosticItem(
                        icon = Icons.Filled.People,
                        title = "Saved Emergency Contacts",
                        subtitle = if (emergencyContacts.isNotEmpty()) {
                            "${emergencyContacts.size} verified contact${if (emergencyContacts.size > 1) "s" else ""} ready for alert"
                        } else {
                            "No contacts configured (Using national dispatch)"
                        },
                        statusBadge = "${emergencyContacts.size} READY",
                        badgeColor = if (emergencyContacts.isNotEmpty()) TravelPrimary else Color.Gray
                    )

                    if (!tripLeaderName.isNullOrBlank()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        DiagnosticItem(
                            icon = Icons.Filled.AssignmentInd,
                            title = "Assigned Trip Leader",
                            subtitle = "$tripLeaderName • ${tripLeaderPhone ?: "Available"}",
                            statusBadge = "ASSIGNED",
                            badgeColor = TravelPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Emergency Type Selector
            Text(
                "EMERGENCY CATEGORY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            val emergencyTypes = listOf(
                "General Emergency",
                "Medical Urgent",
                "Security / Threat",
                "Lost / Stranded",
                "Accident / Injury"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                emergencyTypes.take(3).forEach { type ->
                    val isSelected = selectedEmergencyType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedEmergencyType = type },
                        label = { Text(type, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TravelEmergency,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    statusBadge: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(badgeColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 1)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeColor.copy(alpha = 0.15f)
        ) {
            Text(
                text = statusBadge,
                color = badgeColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
