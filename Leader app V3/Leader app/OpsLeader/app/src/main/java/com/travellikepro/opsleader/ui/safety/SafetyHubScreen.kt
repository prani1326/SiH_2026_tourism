package com.travellikepro.opsleader.ui.safety

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.travellikepro.opsleader.data.api.IncidentDto
import com.travellikepro.opsleader.data.api.SosAlertDto
import com.travellikepro.opsleader.data.api.WeatherForecastDto
import androidx.compose.ui.text.style.TextOverflow
import com.travellikepro.opsleader.ui.components.StatusBadge
import com.travellikepro.opsleader.ui.components.StatusLevel
import com.travellikepro.opsleader.ui.theme.StatusCritical
import com.travellikepro.opsleader.ui.theme.StatusWarning

@Composable
fun SafetyHubScreen(
    viewModel: SafetyViewModel = hiltViewModel()
) {
    val uiData by viewModel.uiData.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Active SOS (${uiData.sosAlerts.count { (it.status ?: "").uppercase() != "RESOLVED" }})", "Incidents Log", "Safety Alerts")

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when (selectedTab) {
            0 -> SosEmergencySection(
                alerts = uiData.sosAlerts,
                onDispatch = { alertId, name, phone, eta ->
                    viewModel.dispatchResponder(alertId, name, phone, eta)
                },
                onResolve = { alertId ->
                    viewModel.resolveSos(alertId)
                },
                onRefresh = { viewModel.loadData() }
            )
            1 -> IncidentManagementSection(
                incidents = uiData.incidents,
                onReportIncident = { name, title, desc, loc ->
                    viewModel.reportIncident(name, title, desc, loc)
                },
                onUpdateStatus = { id, status ->
                    viewModel.updateIncidentStatus(id, status)
                },
                onResolveIncident = { id, summary ->
                    viewModel.resolveIncident(id, summary)
                },
                onRefresh = { viewModel.loadData() }
            )
            2 -> WeatherAndSafetySection()
        }
    }
}

// --------------------------------------------------
// 1. SOS Emergency Section (V3)
// --------------------------------------------------
@Composable
fun SosEmergencySection(
    alerts: List<SosAlertDto>,
    onDispatch: (alertId: String, name: String, phone: String, eta: Int) -> Unit,
    onResolve: (alertId: String) -> Unit,
    onRefresh: () -> Unit
) {
    var selectedAlertForDispatch by remember { mutableStateOf<SosAlertDto?>(null) }
    var selectedAlertForDetail by remember { mutableStateOf<SosAlertDto?>(null) }
    val activeCount = alerts.count { (it.status ?: "").uppercase() != "RESOLVED" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // High-priority emergency broadcast alert bar
        if (activeCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = StatusCritical.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                            "EMERGENCY DISPATCH PROTOCOL ACTIVE",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = StatusCritical
                        )
                        Text(
                            "$activeCount SOS alert(s) requiring operational response.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = StatusCritical)
                    }
                }
            }
        }

        if (alerts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No SOS emergency alerts registered.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(alerts) { alert ->
                    SosAlertCard(
                        alert = alert,
                        onCardClicked = { selectedAlertForDetail = alert },
                        onDispatchClicked = { selectedAlertForDispatch = alert },
                        onResolveClicked = { onResolve(alert.id) }
                    )
                }
            }
        }
    }

    if (selectedAlertForDetail != null) {
        SosDetailDialog(
            alert = selectedAlertForDetail!!,
            onDismiss = { selectedAlertForDetail = null },
            onDispatchClicked = {
                selectedAlertForDispatch = selectedAlertForDetail
                selectedAlertForDetail = null
            },
            onResolveClicked = {
                onResolve(selectedAlertForDetail!!.id)
                selectedAlertForDetail = null
            }
        )
    }

    if (selectedAlertForDispatch != null) {
        DispatchResponderDialog(
            alert = selectedAlertForDispatch!!,
            onDismiss = { selectedAlertForDispatch = null },
            onConfirmDispatch = { responderName, phone, eta ->
                onDispatch(selectedAlertForDispatch!!.id, responderName, phone, eta)
                selectedAlertForDispatch = null
            }
        )
    }
}

@Composable
fun SosAlertCard(
    alert: SosAlertDto,
    onCardClicked: () -> Unit,
    onDispatchClicked: () -> Unit,
    onResolveClicked: () -> Unit
) {
    val isCritical = (alert.severity ?: "").uppercase() == "CRITICAL"
    val isDispatched = (alert.status ?: "").uppercase() == "RESPONDER_DISPATCHED"
    val isResolved = (alert.status ?: "").uppercase() == "RESOLVED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onCardClicked,
        colors = CardDefaults.cardColors(
            containerColor = if (isResolved) MaterialTheme.colorScheme.surfaceVariant else if (isCritical) StatusCritical.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if ((alert.emergency_type ?: "").uppercase() == "MEDICAL") Icons.Default.LocalHospital else Icons.Default.Warning,
                        contentDescription = "Emergency Icon",
                        tint = if (isCritical) StatusCritical else StatusWarning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${alert.emergency_type ?: "EMERGENCY"} • ${alert.id}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isCritical) StatusCritical else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Reported ${alert.reported_at ?: alert.created_at ?: "Recently"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))

                val statusLevel = when ((alert.status ?: "").uppercase()) {
                    "ACTIVE" -> StatusLevel.CRITICAL
                    "RESPONDER_DISPATCHED" -> StatusLevel.WARNING
                    else -> StatusLevel.RESOLVED
                }
                StatusBadge(label = alert.status ?: "ACTIVE", status = statusLevel)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Tourist: ${alert.tourist_name ?: "Guest"} (${alert.tourist_phone ?: "N/A"})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text("Location: ${alert.location_name ?: alert.location ?: "Field Location"}", style = MaterialTheme.typography.bodyMedium)
            if (alert.latitude != 0.0 || alert.longitude != 0.0) {
                Text("Coordinates: ${alert.latitude}, ${alert.longitude}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }

            if (alert.notes != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Notes: ${alert.notes}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (alert.dispatched_responder != null || alert.responder != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Dispatched: ${alert.dispatched_responder ?: alert.responder}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isResolved) {
                    OutlinedButton(
                        onClick = onResolveClicked,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Mark Resolved")
                    }
                }

                if (!isDispatched && !isResolved) {
                    Button(
                        onClick = onDispatchClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Dispatch Unit")
                    }
                }
            }
        }
    }
}

@Composable
fun SosDetailDialog(
    alert: SosAlertDto,
    onDismiss: () -> Unit,
    onDispatchClicked: () -> Unit,
    onResolveClicked: () -> Unit
) {
    val isResolved = (alert.status ?: "").uppercase() == "RESOLVED"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Emergency, contentDescription = null, tint = StatusCritical)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SOS Case: ${alert.id}")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Emergency Type: ${alert.emergency_type ?: "EMERGENCY"} (${alert.severity ?: "CRITICAL"})", fontWeight = FontWeight.Bold, color = StatusCritical)
                HorizontalDivider()
                Text("Tourist: ${alert.tourist_name ?: "Guest"} (${alert.tourist_phone ?: "N/A"})", fontWeight = FontWeight.SemiBold)
                Text("Location: ${alert.location_name ?: alert.location ?: "Field Location"}")
                if (alert.latitude != 0.0 || alert.longitude != 0.0) {
                    Text("GPS Coordinates: ${alert.latitude}, ${alert.longitude}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                if (alert.notes != null) {
                    Text("Field Notes: ${alert.notes}", style = MaterialTheme.typography.bodySmall)
                }
                if (alert.dispatched_responder != null) {
                    Text("Dispatched Unit: ${alert.dispatched_responder}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isResolved) {
                    OutlinedButton(onClick = onResolveClicked) {
                        Text("Resolve SOS")
                    }
                    Button(
                        onClick = onDispatchClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
                    ) {
                        Text("Dispatch")
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun DispatchResponderDialog(
    alert: SosAlertDto,
    onDismiss: () -> Unit,
    onConfirmDispatch: (responderName: String, phone: String, eta: Int) -> Unit
) {
    var responderName by remember { mutableStateOf("Regional Emergency Response Team") }
    var responderPhone by remember { mutableStateOf("+91 94140 12345") }
    var etaMinutes by remember { mutableStateOf("15") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dispatch Emergency Response Unit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Target Location: ${alert.location_name ?: alert.location ?: "Field Location"}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = responderName,
                    onValueChange = { responderName = it },
                    label = { Text("Responder Unit / Partner") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = responderPhone,
                    onValueChange = { responderPhone = it },
                    label = { Text("Contact Phone") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = etaMinutes,
                    onValueChange = { etaMinutes = it },
                    label = { Text("Estimated ETA (Minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirmDispatch(responderName.trim(), responderPhone.trim(), etaMinutes.toIntOrNull() ?: 15)
                },
                colors = ButtonDefaults.buttonColors(containerColor = StatusCritical)
            ) {
                Text("Confirm & Notify")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// --------------------------------------------------
// 2. Incident Management Section (V3)
// --------------------------------------------------
@Composable
fun IncidentManagementSection(
    incidents: List<IncidentDto>,
    onReportIncident: (touristName: String, title: String, description: String, location: String) -> Unit,
    onUpdateStatus: (incidentId: String, status: String) -> Unit,
    onResolveIncident: (incidentId: String, summary: String) -> Unit,
    onRefresh: () -> Unit
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var selectedIncidentForDetail by remember { mutableStateOf<IncidentDto?>(null) }
    var incidentToResolve by remember { mutableStateOf<IncidentDto?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Operational Incidents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                Button(onClick = { showReportDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Report")
                }
            }
        }

        if (incidents.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No incidents recorded.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(incidents) { incident ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { selectedIncidentForDetail = incident },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    incident.id,
                                    modifier = Modifier.weight(1f, fill = false),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val statusLevel = if ((incident.status ?: "").uppercase() == "RESOLVED") StatusLevel.RESOLVED else StatusLevel.WARNING
                                StatusBadge(label = incident.status ?: "OPEN", status = statusLevel)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(incident.title ?: "Incident Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Tourist: ${incident.tourist_name ?: "Guest"} • ${incident.location ?: incident.destination ?: "Field"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(incident.description ?: "", style = MaterialTheme.typography.bodyMedium)

                            if (incident.resolution_summary != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Resolution: ${incident.resolution_summary}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("Tap for Evidence & Actions →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    if (selectedIncidentForDetail != null) {
        val inc = selectedIncidentForDetail!!
        AlertDialog(
            onDismissRequest = { selectedIncidentForDetail = null },
            title = { Text("Incident: ${inc.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(inc.title ?: "Incident Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Severity: ${inc.severity ?: "NORMAL"} • Category: ${inc.category ?: "SAFETY"}", style = MaterialTheme.typography.labelMedium, color = StatusWarning)
                    HorizontalDivider()
                    Text("Tourist: ${inc.tourist_name ?: "Guest"}", fontWeight = FontWeight.SemiBold)
                    Text("Location / Route: ${inc.location ?: inc.destination ?: "Field"}")
                    Text("Evidence & Description:", fontWeight = FontWeight.Bold)
                    Text(inc.description ?: "", style = MaterialTheme.typography.bodyMedium)
                    if (inc.resolution_summary != null) {
                        Text("Resolution: ${inc.resolution_summary}", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if ((inc.status ?: "").uppercase() != "RESOLVED") {
                        OutlinedButton(onClick = {
                            onUpdateStatus(inc.id, "INVESTIGATING")
                            selectedIncidentForDetail = null
                        }) {
                            Text("Investigate")
                        }
                        Button(onClick = {
                            incidentToResolve = inc
                            selectedIncidentForDetail = null
                        }) {
                            Text("Resolve")
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedIncidentForDetail = null }) { Text("Close") }
            }
        )
    }

    if (incidentToResolve != null) {
        var resolutionNotes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { incidentToResolve = null },
            title = { Text("Resolve Incident ${incidentToResolve!!.id}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Please describe corrective action taken:")
                    OutlinedTextField(
                        value = resolutionNotes,
                        onValueChange = { resolutionNotes = it },
                        label = { Text("Resolution Summary") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onResolveIncident(incidentToResolve!!.id, resolutionNotes.ifBlank { "Resolved by Ops Leader on field" })
                    incidentToResolve = null
                }) {
                    Text("Confirm Resolution")
                }
            },
            dismissButton = {
                TextButton(onClick = { incidentToResolve = null }) { Text("Cancel") }
            }
        )
    }

    if (showReportDialog) {
        var touristName by remember { mutableStateOf("") }
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Log New Incident") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = touristName, onValueChange = { touristName = it }, label = { Text("Tourist Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Incident Title") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Location") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            onReportIncident(
                                touristName.ifBlank { "Registered Tourist" },
                                title.trim(),
                                description.trim(),
                                location.ifBlank { "Field Route" }
                            )
                            showReportDialog = false
                        }
                    }
                ) { Text("Save Incident") }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --------------------------------------------------
// 3. Safety Alerts & Weather Section (V3)
// --------------------------------------------------
@Composable
fun WeatherAndSafetySection() {
    var acknowledgedAlerts by remember { mutableStateOf(setOf<String>()) }
    var selectedAlertForDetail by remember { mutableStateOf<WeatherForecastDto?>(null) }

    val regionalWeather = remember {
        listOf(
            WeatherForecastDto(
                region = "Jaipur / Rajasthan",
                current_temp_celsius = 34,
                condition = "SUNNY",
                humidity_percent = 28,
                advisory_notice = "High heat advisory. Ensure adequate hydration packs in vehicles."
            ),
            WeatherForecastDto(
                region = "Goa Coastal Zone",
                current_temp_celsius = 29,
                condition = "RAIN",
                humidity_percent = 84,
                advisory_notice = "Moderate sea swells. Water sports suspended on Calangute/Baga.",
                is_alert_active = true
            ),
            WeatherForecastDto(
                region = "Munnar & Alleppey (Kerala)",
                current_temp_celsius = 22,
                condition = "THUNDERSTORM",
                humidity_percent = 90,
                advisory_notice = "Ghat road fog and intermittent rain. Restrict night driving.",
                is_alert_active = true
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Real-Time Weather & Safety Advisories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(regionalWeather) { weather ->
                val regionName = weather.region ?: "Default Region"
                val isAck = acknowledgedAlerts.contains(regionName)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { selectedAlertForDetail = weather },
                    colors = CardDefaults.cardColors(
                        containerColor = if (weather.is_alert_active && !isAck) StatusWarning.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(regionName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    when ((weather.condition ?: "").uppercase()) {
                                        "SUNNY" -> Icons.Default.WbSunny
                                        "RAIN" -> Icons.Default.WaterDrop
                                        else -> Icons.Default.Thunderstorm
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${weather.current_temp_celsius}°C", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Condition: ${weather.condition ?: "Clear"} • Humidity: ${weather.humidity_percent}%", style = MaterialTheme.typography.bodySmall)

                        if (weather.advisory_notice != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = StatusWarning)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(weather.advisory_notice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (weather.is_alert_active) {
                                if (isAck) {
                                    Text("✓ Acknowledged", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Text("⚠ Active Hazard Warning", style = MaterialTheme.typography.labelSmall, color = StatusWarning, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            Text("View Affected Area →", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }

    if (selectedAlertForDetail != null) {
        val w = selectedAlertForDetail!!
        val rName = w.region ?: "Default Region"
        val isAck = acknowledgedAlerts.contains(rName)
        AlertDialog(
            onDismissRequest = { selectedAlertForDetail = null },
            title = { Text("Safety Advisory: $rName") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Weather Condition: ${w.condition ?: "Clear"} (${w.current_temp_celsius}°C)", fontWeight = FontWeight.Bold)
                    Text("Humidity: ${w.humidity_percent}%")
                    HorizontalDivider()
                    Text("Affected Area & Routes:", fontWeight = FontWeight.Bold)
                    Text("All ongoing tours and road movements in $rName zone.")
                    if (w.advisory_notice != null) {
                        Text("Advisory Notice:", fontWeight = FontWeight.Bold, color = StatusWarning)
                        Text(w.advisory_notice)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    acknowledgedAlerts = acknowledgedAlerts + rName
                    selectedAlertForDetail = null
                }) {
                    Text(if (isAck) "Already Acknowledged" else "Acknowledge Alert")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAlertForDetail = null }) { Text("Close") }
            }
        )
    }
}
