package com.touristapp.presentation.safety

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.touristapp.data.models.EmergencyAlert
import com.touristapp.data.models.SosTimelineEvent
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveSosScreen(
    alertId: String,
    onBackClick: () -> Unit,
    onEmergencyResolved: () -> Unit,
    onOpenNearbyHelp: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val safetyRepo = remember { ServiceLocator.safetyRepository }
    val locationService = remember { ServiceLocator.locationService }

    var alert by remember { mutableStateOf<EmergencyAlert?>(null) }
    var showResolveDialog by remember { mutableStateOf(false) }
    var isResolving by remember { mutableStateOf(false) }
    var isLocationSharingActive by remember { mutableStateOf(true) }

    val activeAlertFromFlow by (safetyRepo.activeAlertFlow ?: kotlinx.coroutines.flow.emptyFlow()).collectAsState(initial = null)

    LaunchedEffect(activeAlertFromFlow) {
        if (activeAlertFromFlow != null) {
            alert = activeAlertFromFlow
        }
    }

    // Periodic live location update while SOS is active
    LaunchedEffect(isLocationSharingActive) {
        while (isLocationSharingActive) {
            try {
                val loc = locationService.getCurrentLocation()
                if (loc != null && alert != null) {
                    safetyRepo.updateLiveLocation(
                        alertId = alert!!.alertId,
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracy = loc.accuracy
                    )
                }
            } catch (e: Exception) {
                // Ignore
            }
            delay(15000L) // updates every 15s
        }
    }

    // Flashing emergency banner animation
    val infiniteTransition = rememberInfiniteTransition(label = "emergencyFlash")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val timeFormatter = remember { SimpleDateFormat("hh:mm:ss a", Locale.US) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(TravelEmergency)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Active Emergency Incident", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onOpenNearbyHelp,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TravelPrimary)
                    ) {
                        Icon(Icons.Filled.LocalHospital, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Nearby Help", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showResolveDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1.2f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TravelSuccess)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("I AM SAFE / RESOLVE", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // High Visibility SOS ACTIVE Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = TravelEmergency.copy(alpha = alphaAnim)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    "PRIORITY: EMERGENCY",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                "STATUS: ACTIVE",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Icon(
                            Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            "SOS ACTIVE",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )

                        Text(
                            alert?.emergencyType ?: "General Emergency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.95f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Alert ID: ${alert?.alertId?.take(8) ?: alertId.take(8)} • Dispatched at ${timeFormatter.format(Date(alert?.createdAt ?: System.currentTimeMillis()))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Quick Call Actions (112 National Police, Emergency Contacts SMS, Trip Leader)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Call 112
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TravelEmergency),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CALL 112", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    // SMS Contacts
                    Button(
                        onClick = {
                            val phone = alert?.emergencyContacts?.firstOrNull()?.primaryPhone ?: "112"
                            val msg = "EMERGENCY ALERT: I have triggered SOS. Location: Lat ${alert?.latitude}, Lon ${alert?.longitude}. Please assist immediately."
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("smsto:$phone")).apply {
                                putExtra("sms_body", msg)
                            }
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Filled.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS CONTACTS", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Live Location Indicator Card
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MyLocation, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Live Location Tracking", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Switch(
                                checked = isLocationSharingActive,
                                onCheckedChange = { isLocationSharingActive = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = TravelPrimary)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "Coordinates: ${String.format(Locale.US, "%.5f", alert?.latitude ?: 15.2993)}°N, ${String.format(Locale.US, "%.5f", alert?.longitude ?: 74.1240)}°E",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Accuracy: ±${(alert?.accuracy ?: 12f).roundToInt()}m • Last update: ${timeFormatter.format(Date(alert?.lastLocationUpdate ?: System.currentTimeMillis()))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Real Delivery Status Timeline
            item {
                Text(
                    "INCIDENT RESPONSE TIMELINE",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            val events = alert?.timelineEvents ?: emptyList()
            itemsIndexed(events) { index, event ->
                TimelineEventCard(
                    event = event,
                    isLast = index == events.lastIndex,
                    timeFormatter = timeFormatter
                )
            }
        }
    }

    if (showResolveDialog) {
        AlertDialog(
            onDismissRequest = { showResolveDialog = false },
            title = { Text("Resolve Emergency SOS", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you safe and ready to conclude this emergency alert? This will inform responders and stop active location sharing.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        isResolving = true
                        coroutineScope.launch {
                            safetyRepo.resolveSos(alert?.alertId ?: alertId)
                            isResolving = false
                            showResolveDialog = false
                            onEmergencyResolved()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelSuccess)
                ) {
                    if (isResolving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Confirm I Am Safe")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showResolveDialog = false }) {
                    Text("Keep Active")
                }
            }
        )
    }
}

@Composable
private fun TimelineEventCard(
    event: SosTimelineEvent,
    isLast: Boolean,
    timeFormatter: SimpleDateFormat
) {
    val statusColor = when (event.status) {
        "SUCCESS" -> TravelSuccess
        "OFFLINE" -> TravelOrange
        "PENDING" -> TravelPrimary
        "FAILED" -> TravelEmergency
        else -> Color.Gray
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(statusColor))
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = if (isLast) 0.dp else 10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(event.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        timeFormatter.format(Date(event.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    event.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                )

                if (!event.detail.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = event.detail,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
