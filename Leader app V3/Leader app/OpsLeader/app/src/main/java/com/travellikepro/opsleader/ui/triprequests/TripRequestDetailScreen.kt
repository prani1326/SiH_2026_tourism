package com.travellikepro.opsleader.ui.triprequests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.travellikepro.opsleader.data.api.TripDto
import com.travellikepro.opsleader.data.api.VendorDto
import com.travellikepro.opsleader.ui.components.StatusBadge
import com.travellikepro.opsleader.ui.components.StatusLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripRequestDetailScreen(
    requestId: String,
    viewModel: TripRequestDetailViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAssignDialog by remember { mutableStateOf(false) }
    var showRejectDialog by remember { mutableStateOf(false) }

    LaunchedEffect(requestId) {
        viewModel.loadTripDetail(requestId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request $requestId") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadTripDetail(requestId) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState is TripDetailUiState.Success) {
                val trip = (uiState as TripDetailUiState.Success).trip
                val statusStr = trip.status ?: "pending"
                val isCompleted = statusStr.equals("completed", ignoreCase = true)
                val isCancelled = statusStr.equals("cancelled", ignoreCase = true) || statusStr.equals("rejected", ignoreCase = true)

                if (!isCompleted && !isCancelled) {
                    BottomAppBar(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showRejectDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Reject")
                            }

                            Button(
                                onClick = { showAssignDialog = true },
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (trip.assigned_vendor_name != null) "Reassign" else "Assign Partner")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is TripDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is TripDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.loadTripDetail(requestId) }) { Text("Retry") }
                    }
                }
            }

            is TripDetailUiState.Success -> {
                val trip = state.trip
                val statusStr = trip.status ?: "pending"
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Destination & Status Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val destinationTitle = trip.destination ?: trip.destination_name ?: trip.title ?: "Tour Plan #${trip.id.take(8)}"
                        Text(
                            destinationTitle,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val statusLevel = when (statusStr.lowercase()) {
                            "pending", "new" -> StatusLevel.ATTENTION
                            "accepted", "assigned", "in_progress", "active" -> StatusLevel.NORMAL
                            "completed" -> StatusLevel.RESOLVED
                            else -> StatusLevel.CRITICAL
                        }
                        StatusBadge(label = statusStr.uppercase(), status = statusLevel)
                    }

                    HorizontalDivider()

                    // Requester Information
                    Text("Tourist Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Name: ${trip.tourist_name ?: trip.requester_name ?: "Guest"}", style = MaterialTheme.typography.bodyMedium)
                    if (trip.requester_contact != null || trip.tourist_phone != null) {
                        Text("Contact: ${trip.requester_contact ?: trip.tourist_phone ?: ""}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text("Party Size: ${trip.group_size.coerceAtLeast(trip.number_of_guests)} Guests", style = MaterialTheme.typography.bodyMedium)

                    HorizontalDivider()

                    // Trip Details
                    Text("Trip Logistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Dates: ${trip.start_date ?: trip.preferred_dates ?: "Scheduled Dates"}", style = MaterialTheme.typography.bodyMedium)
                    if (trip.budget > 0 || trip.estimated_budget > 0) {
                        Text("Budget: $${trip.budget.takeIf { it > 0 } ?: trip.estimated_budget}", style = MaterialTheme.typography.bodyMedium)
                    }

                    if (trip.special_requirements != null || trip.special_notes != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Special Requirements / Dietary:", style = MaterialTheme.typography.labelMedium)
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = trip.special_requirements ?: trip.special_notes ?: "None specified",
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    HorizontalDivider()

                    // Assigned Partner Section
                    Text("Operational Assignment", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (trip.assigned_vendor_name != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Assigned Partner: ${trip.assigned_vendor_name}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    Text("Status: Active Assignment", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    } else {
                        Text("No vendor/partner assigned yet. Tap below to select an approved vendor.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(80.dp))
                }

                // Vendor Assignment Dialog
                if (showAssignDialog) {
                    var selectedVendor by remember { mutableStateOf<VendorDto?>(state.availableVendors.firstOrNull()) }
                    var assignmentNotes by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showAssignDialog = false },
                        title = { Text("Assign Partner to Trip") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Select Verified Field Vendor:", style = MaterialTheme.typography.labelMedium)
                                state.availableVendors.take(5).forEach { vendor ->
                                    val isSelected = selectedVendor?.id == vendor.id
                                    Surface(
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedVendor = vendor }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = isSelected, onClick = { selectedVendor = vendor })
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(vendor.name ?: "Verified Partner", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                                Text("Type: ${vendor.service_type ?: vendor.serviceType ?: "General"} • Score: ${vendor.quality_score}", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = assignmentNotes,
                                    onValueChange = { assignmentNotes = it },
                                    label = { Text("Assignment Instructions") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    if (selectedVendor != null) {
                                        viewModel.assignVendor(
                                            vendorId = selectedVendor!!.id,
                                            vendorName = selectedVendor!!.name,
                                            notes = assignmentNotes
                                        )
                                        showAssignDialog = false
                                    }
                                }
                            ) { Text("Confirm Assignment") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAssignDialog = false }) { Text("Cancel") }
                        }
                    )
                }

                // Reject Dialog
                if (showRejectDialog) {
                    var rejectReason by remember { mutableStateOf("No vendor availability for requested dates") }

                    AlertDialog(
                        onDismissRequest = { showRejectDialog = false },
                        title = { Text("Reject Trip Request") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Specify rejection rationale for audit log:", style = MaterialTheme.typography.bodySmall)
                                OutlinedTextField(
                                    value = rejectReason,
                                    onValueChange = { rejectReason = it },
                                    label = { Text("Rejection Reason") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.rejectTrip(rejectReason)
                                    showRejectDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) { Text("Confirm Reject") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showRejectDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }
    }
}
