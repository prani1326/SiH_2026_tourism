package com.travellikepro.opsleader.ui.main.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.travellikepro.opsleader.data.api.TripDto
import com.travellikepro.opsleader.ui.components.MetricTile
import androidx.compose.ui.text.style.TextOverflow
import com.travellikepro.opsleader.ui.components.StatusBadge
import com.travellikepro.opsleader.ui.components.StatusLevel
import com.travellikepro.opsleader.ui.theme.StatusCritical
import com.travellikepro.opsleader.ui.theme.StatusWarning

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToProfile: () -> Unit,
    onNavigateToRequests: () -> Unit,
    onNavigateToTripDetail: (String) -> Unit = {},
    onNavigateToSafety: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToVendors: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is DashboardUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Button(onClick = { viewModel.loadDashboardData(isRefresh = true) }) {
                            Text("Retry")
                        }
                    }
                }
            }

            is DashboardUiState.Success -> {
                val data = state.data
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Active SOS Emergency Alert Card (Dynamic)
                    if (data.activeSosAlert != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToSafety() },
                            colors = CardDefaults.cardColors(containerColor = StatusCritical.copy(alpha = 0.12f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(StatusCritical)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "ACTIVE ${(data.activeSosAlert.severity ?: "CRITICAL").uppercase()} SOS ALERT",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = StatusCritical
                                    )
                                    Text(
                                        "${data.activeSosAlert.tourist_name ?: "Tourist"} • ${data.activeSosAlert.location_name ?: data.activeSosAlert.location ?: "Field Area"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "View SOS",
                                    tint = StatusCritical
                                )
                            }
                        }
                    }

                    // Profile Header Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToProfile() },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = data.userName.split(" ").mapNotNull { it.firstOrNull()?.uppercase() }.take(2).joinToString(""),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(data.userName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("Ops Leader • ${data.userRegion}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                IconButton(onClick = { viewModel.loadDashboardData(isRefresh = true) }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Data")
                                }
                            }
                        }
                    }

                    // Quick Command Center Shortcuts (V1-V4)
                    Text(
                        text = "Operations Command Center",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            title = "Safety & SOS",
                            subtitle = if (data.activeSosAlert != null) "1 Active Alert" else "All Clear",
                            icon = Icons.Default.Emergency,
                            color = if (data.activeSosAlert != null) StatusCritical else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToSafety() }
                        )
                        ActionCard(
                            title = "Support Desk",
                            subtitle = "${data.kpi.openSupportTickets.coerceAtLeast(data.kpi.open_support_tickets)} open tickets",
                            icon = Icons.Default.SupportAgent,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToSupport() }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionCard(
                            title = "Analytics & Reports",
                            subtitle = "Field KPI reports",
                            icon = Icons.Default.Analytics,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToAnalytics() }
                        )
                        ActionCard(
                            title = "Partners / Vendors",
                            subtitle = "Vendor directory",
                            icon = Icons.Default.Storefront,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToVendors() }
                        )
                    }

                    // Live Operational KPIs
                    Text(
                        text = "Real-Time Operational KPIs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    val activeTripsCount = (data.kpi.activeTrips.takeIf { it > 0 } ?: data.kpi.active_trips.takeIf { it > 0 } ?: data.recentTrips.size).toString()
                    val activeTouristsCount = (data.kpi.activeTourists.takeIf { it > 0 } ?: data.kpi.total_active_tourists).toString()
                    val openIncidentsCount = (data.kpi.openIncidents.takeIf { it > 0 } ?: data.kpi.open_incidents).toString()
                    val criticalAlertsCount = (data.kpi.criticalAlerts.takeIf { it > 0 } ?: data.kpi.critical_alerts).toString()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricTile(
                            title = "Active Trips",
                            value = activeTripsCount,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToRequests() },
                            statusLevel = StatusLevel.NORMAL
                        )
                        MetricTile(
                            title = "Active Tourists",
                            value = activeTouristsCount,
                            modifier = Modifier.weight(1f),
                            statusLevel = StatusLevel.RESOLVED
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricTile(
                            title = "Open Incidents",
                            value = openIncidentsCount,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToSafety() },
                            statusLevel = if (openIncidentsCount != "0") StatusLevel.WARNING else StatusLevel.RESOLVED
                        )
                        MetricTile(
                            title = "Critical Alerts",
                            value = criticalAlertsCount,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onNavigateToSafety() },
                            statusLevel = if (criticalAlertsCount != "0") StatusLevel.CRITICAL else StatusLevel.RESOLVED
                        )
                    }

                    // Recent Requests Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Trip Requests",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = onNavigateToRequests) {
                            Text("View All")
                        }
                    }

                    if (data.recentTrips.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(
                                text = "No active trip requests at this time.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        data.recentTrips.forEach { trip ->
                            RecentTripCard(
                                trip = trip,
                                onClick = { onNavigateToTripDetail(trip.id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RecentTripCard(trip: TripDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val destinationTitle = trip.destination ?: trip.destination_name ?: trip.title ?: "Tour Plan #${trip.id.take(8)}"
                Text(
                    text = destinationTitle,
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                val statusStr = trip.status ?: "pending"
                val statusLevel = when (statusStr.lowercase()) {
                    "pending", "new" -> StatusLevel.ATTENTION
                    "accepted", "in_progress", "active" -> StatusLevel.NORMAL
                    "completed" -> StatusLevel.RESOLVED
                    else -> StatusLevel.CRITICAL
                }
                StatusBadge(label = statusStr.uppercase(), status = statusLevel)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Tourist: ${trip.tourist_name ?: trip.requester_name ?: "Guest"} • ${trip.start_date ?: trip.preferred_dates ?: "Scheduled Dates"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!trip.assigned_vendor_name.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assigned: ${trip.assigned_vendor_name}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
