package com.travellikepro.opsleader.ui.tourists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travellikepro.opsleader.data.model.vendor.AssignmentStatus
import com.travellikepro.opsleader.data.model.vendor.ServiceType
import com.travellikepro.opsleader.data.model.vendor.Vendor
import com.travellikepro.opsleader.data.model.vendor.VendorAssignment
import com.travellikepro.opsleader.data.model.vendor.VendorAvailability
import com.travellikepro.opsleader.ui.components.StatusBadge
import com.travellikepro.opsleader.ui.components.StatusLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TouristDetailScreen(
    touristId: String,
    onBack: () -> Unit
) {
    var assignedVendors by remember { mutableStateOf(listOf<VendorAssignment>()) }
    var showAssignSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tourist: Raj Kumar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Basic Tourist Info Mock
            Text("Trip: Goa Family Vacation (TR-129)", style = MaterialTheme.typography.titleMedium)
            Text("Contact: +91 9123456789", style = MaterialTheme.typography.bodyMedium)
            
            Divider()

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Assigned Vendors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { showAssignSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Assign")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assign Vendor")
                }
            }

            if (assignedVendors.isEmpty()) {
                Text("No vendors assigned yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                assignedVendors.forEach { assignment ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(assignment.vendorName, fontWeight = FontWeight.Bold)
                                StatusBadge(
                                    label = assignment.status.name,
                                    status = if (assignment.status == AssignmentStatus.CANCELLED) StatusLevel.CRITICAL else StatusLevel.RESOLVED
                                )
                            }
                            Text("Service: ${assignment.serviceType}", style = MaterialTheme.typography.bodySmall)
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            if (assignment.status != AssignmentStatus.CANCELLED) {
                                OutlinedButton(
                                    onClick = {
                                        // Cancel assignment logic
                                        assignedVendors = assignedVendors.map { 
                                            if (it.id == assignment.id) it.copy(status = AssignmentStatus.CANCELLED) else it 
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Cancel Assignment", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAssignSheet) {
        AssignVendorBottomSheet(
            onDismiss = { showAssignSheet = false },
            onAssign = { vendor ->
                val newAssignment = VendorAssignment(
                    vendorId = vendor.id,
                    vendorName = vendor.name,
                    touristId = touristId,
                    touristName = "Raj Kumar",
                    tripId = "TR-129",
                    serviceType = vendor.serviceType,
                    assignedTimestamp = System.currentTimeMillis(),
                    assignedByOpsLeader = "Jane Doe"
                )
                assignedVendors = assignedVendors + newAssignment
                showAssignSheet = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignVendorBottomSheet(
    onDismiss: () -> Unit,
    onAssign: (Vendor) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedServiceType by remember { mutableStateOf(ServiceType.TRANSPORT) }
    
    // Mock Vendor Database
    val allVendors = listOf(
        Vendor(name = "Raj Tours & Travels", contactPhone = "+91 9876543210", serviceType = ServiceType.TRANSPORT, regionCoverage = listOf("Goa"), availabilityStatus = VendorAvailability.AVAILABLE),
        Vendor(name = "Goa Heritage Guides", contactPhone = "+91 8765432109", serviceType = ServiceType.GUIDE, regionCoverage = listOf("Goa"), availabilityStatus = VendorAvailability.AT_CAPACITY),
        Vendor(name = "Kerala Backwater Resorts", contactPhone = "+91 7654321098", serviceType = ServiceType.HOTEL, regionCoverage = listOf("Kerala"), availabilityStatus = VendorAvailability.AVAILABLE)
    )

    val filteredVendors = allVendors.filter { it.serviceType == selectedServiceType && it.regionCoverage.contains("Goa") }
    var showCapacityWarningFor by remember { mutableStateOf<Vendor?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Assign Vendor for Goa (TR-129)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            
            // Service Type Selector
            ScrollableTabRow(selectedTabIndex = selectedServiceType.ordinal) {
                ServiceType.values().forEach { type ->
                    Tab(
                        selected = selectedServiceType == type,
                        onClick = { selectedServiceType = type },
                        text = { Text(type.name) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (filteredVendors.isEmpty()) {
                Text("No vendors found for this service in this region.")
            } else {
                filteredVendors.forEach { vendor ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable {
                            if (vendor.availabilityStatus == VendorAvailability.AT_CAPACITY) {
                                showCapacityWarningFor = vendor
                            } else if (vendor.availabilityStatus == VendorAvailability.AVAILABLE) {
                                onAssign(vendor)
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (vendor.availabilityStatus == VendorAvailability.UNAVAILABLE) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(vendor.name, fontWeight = FontWeight.Bold)
                                Text("Quality: ${vendor.qualityScore}", style = MaterialTheme.typography.bodySmall)
                            }
                            
                            val statusLevel = when (vendor.availabilityStatus) {
                                VendorAvailability.AVAILABLE -> StatusLevel.RESOLVED
                                VendorAvailability.AT_CAPACITY -> StatusLevel.WARNING
                                VendorAvailability.UNAVAILABLE -> StatusLevel.CRITICAL
                            }
                            StatusBadge(label = vendor.availabilityStatus.name, status = statusLevel)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showCapacityWarningFor != null) {
        AlertDialog(
            onDismissRequest = { showCapacityWarningFor = null },
            title = { Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vendor at Capacity")
            }},
            text = { Text("${showCapacityWarningFor?.name} is currently marked as AT_CAPACITY. Assigning them may result in a rejected booking or double-booking. Are you sure you want to proceed?") },
            confirmButton = {
                Button(onClick = {
                    val v = showCapacityWarningFor
                    showCapacityWarningFor = null
                    if (v != null) onAssign(v)
                }) {
                    Text("Assign Anyway")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCapacityWarningFor = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
