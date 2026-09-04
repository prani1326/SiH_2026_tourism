package com.travellikepro.opsleader.ui.vendors

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travellikepro.opsleader.data.model.vendor.ServiceType
import com.travellikepro.opsleader.data.model.vendor.Vendor
import com.travellikepro.opsleader.data.model.vendor.VendorAvailability
import com.travellikepro.opsleader.ui.components.StatusBadge
import com.travellikepro.opsleader.ui.components.StatusLevel

@Composable
fun VendorsScreen() {
    // Mock Data
    val vendors = remember {
        listOf(
            Vendor(
                name = "Raj Tours & Travels",
                contactPhone = "+91 9876543210",
                serviceType = ServiceType.TRANSPORT,
                regionCoverage = listOf("Rajasthan", "Delhi"),
                availabilityStatus = VendorAvailability.AVAILABLE,
                qualityScore = 4.8
            ),
            Vendor(
                name = "Goa Heritage Guides",
                contactPhone = "+91 8765432109",
                serviceType = ServiceType.GUIDE,
                regionCoverage = listOf("Goa"),
                availabilityStatus = VendorAvailability.AT_CAPACITY,
                qualityScore = 4.9
            ),
            Vendor(
                name = "Kerala Backwater Resorts",
                contactPhone = "+91 7654321098",
                serviceType = ServiceType.HOTEL,
                regionCoverage = listOf("Kerala"),
                availabilityStatus = VendorAvailability.AVAILABLE,
                qualityScore = 4.5
            )
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Vendors & Partners", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        // Mock Filter Row
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { }, label = { Text("State / UT") })
            AssistChip(onClick = { }, label = { Text("Service Type") })
            AssistChip(onClick = { }, label = { Text("Availability") })
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(vendors) { vendor ->
                VendorCard(vendor = vendor)
            }
        }
    }
}

@Composable
fun VendorCard(vendor: Vendor) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(vendor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                val statusLevel = when (vendor.availabilityStatus) {
                    VendorAvailability.AVAILABLE -> StatusLevel.RESOLVED
                    VendorAvailability.AT_CAPACITY -> StatusLevel.WARNING
                    VendorAvailability.UNAVAILABLE -> StatusLevel.CRITICAL
                }
                StatusBadge(label = vendor.availabilityStatus.name, status = statusLevel)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Text("Service: ${vendor.serviceType}", style = MaterialTheme.typography.bodyMedium)
            Text("Regions: ${vendor.regionCoverage.joinToString()}", style = MaterialTheme.typography.bodyMedium)
            Text("Contact: ${vendor.contactPhone}", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, contentDescription = "Rating", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(vendor.qualityScore.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
