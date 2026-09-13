package com.touristapp.presentation.safety

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.data.models.EmergencyServiceType
import com.touristapp.data.models.NearbyEmergencyService
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearbyHelpScreen(
    onBackClick: () -> Unit,
    onNavigateToMap: ((lat: Double, lon: Double, name: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val safetyRepo = remember { ServiceLocator.safetyRepository }
    val locationService = remember { ServiceLocator.locationService }

    var selectedFilter by remember { mutableStateOf<EmergencyServiceType?>(null) }
    var currentLat by remember { mutableDoubleStateOf(15.2993) }
    var currentLon by remember { mutableDoubleStateOf(74.1240) }
    var services by remember { mutableStateOf<List<NearbyEmergencyService>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val loc = locationService.getCurrentLocation()
            if (loc != null) {
                currentLat = loc.latitude
                currentLon = loc.longitude
            }
        } catch (e: Exception) {
            // Use defaults
        }
        services = safetyRepo.getNearbyEmergencyServices(currentLat, currentLon)
    }

    val filteredServices = if (selectedFilter == null) services else services.filter { it.type == selectedFilter }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Nearby Emergency Services", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // Location Banner & Notice
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.brandPillBackground,
                    border = BorderStroke(1.dp, TravelPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.NearMe, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Live Proximity Finder",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Showing nearest police, medical casualty, and tourist helplines relative to your GPS coordinates.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedFilter == null,
                            onClick = { selectedFilter = null },
                            label = { Text("All Help", fontWeight = if (selectedFilter == null) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TravelPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                    items(EmergencyServiceType.values()) { type ->
                        val isSelected = selectedFilter == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = type },
                            label = { Text(type.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TravelPrimary,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Services List
            items(filteredServices, key = { it.id }) { service ->
                EmergencyServiceCard(
                    service = service,
                    onCall = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${service.phoneNumber}"))
                        context.startActivity(intent)
                    },
                    onNavigate = {
                        if (onNavigateToMap != null) {
                            onNavigateToMap(service.latitude, service.longitude, service.name)
                        } else {
                            val uri = Uri.parse("geo:${service.latitude},${service.longitude}?q=${Uri.encode(service.name)}")
                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(intent)
                        }
                    }
                )
            }

            // Architectural Notice Footer
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "One-tap call directly connects via carrier dialer. Official police API integrations can connect seamlessly to this dispatch module.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyServiceCard(
    service: NearbyEmergencyService,
    onCall: () -> Unit,
    onNavigate: () -> Unit
) {
    val (icon, badgeColor) = when (service.type) {
        EmergencyServiceType.POLICE -> Icons.Filled.LocalPolice to TravelEmergency
        EmergencyServiceType.TOURIST_POLICE -> Icons.Filled.Shield to TravelPrimary
        EmergencyServiceType.HOSPITAL, EmergencyServiceType.AMBULANCE -> Icons.Filled.LocalHospital to Color(0xFFE53935)
        EmergencyServiceType.FIRE -> Icons.Filled.LocalFireDepartment to TravelOrange
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        service.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        service.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 2
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.brandPillBackground
                ) {
                    Text(
                        "${String.format(Locale.US, "%.1f", service.distanceKm)} km",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TravelPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCall,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Call (${service.phoneNumber})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onNavigate,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(44.dp)
                ) {
                    Icon(Icons.Filled.Navigation, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Directions", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
