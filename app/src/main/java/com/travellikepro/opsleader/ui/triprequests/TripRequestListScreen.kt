package com.travellikepro.opsleader.ui.triprequests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travellikepro.opsleader.data.model.triprequest.SourceChannel
import com.travellikepro.opsleader.data.model.triprequest.TripRequest
import com.travellikepro.opsleader.data.model.triprequest.TripRequestStatus
import com.travellikepro.opsleader.ui.components.StatusBadge
import com.travellikepro.opsleader.ui.components.StatusLevel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TripRequestListScreen(
    onNavigateToDetail: (String) -> Unit
) {
    // Dummy Data for now
    val requests = remember {
        listOf(
            TripRequest(
                id = "TR-101",
                requesterName = "Amit Sharma",
                requesterContact = "amit.sharma@example.com",
                destination = "Jaipur, Rajasthan",
                preferredDates = "Oct 12 - Oct 18",
                groupSize = 2,
                budgetRange = "₹80,000 - ₹1,50,000",
                specialRequirements = "Wheelchair accessible hotel",
                sourceChannel = SourceChannel.APP,
                submittedTimestamp = System.currentTimeMillis() - 3600000,
                slaDeadlineTimestamp = System.currentTimeMillis() + 3600000,
                status = TripRequestStatus.NEW
            ),
            TripRequest(
                id = "TR-102",
                requesterName = "Priya Nair",
                requesterContact = "priya.nair@example.com",
                destination = "Munnar, Kerala",
                preferredDates = "Dec 1 - Dec 14",
                groupSize = 4,
                budgetRange = "₹2,00,000+",
                specialRequirements = "Dietary restrictions: Vegan",
                sourceChannel = SourceChannel.WEB,
                submittedTimestamp = System.currentTimeMillis() - 86400000,
                slaDeadlineTimestamp = System.currentTimeMillis() - 3600000, // SLA Breached
                status = TripRequestStatus.UNDER_REVIEW,
                assignedReviewer = "Jane Doe"
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(requests) { request ->
            TripRequestCard(
                request = request,
                onClick = { onNavigateToDetail(request.id) }
            )
        }
    }
}

@Composable
fun TripRequestCard(request: TripRequest, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = request.destination,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val statusLevel = when (request.status) {
                    TripRequestStatus.NEW -> StatusLevel.ATTENTION
                    TripRequestStatus.UNDER_REVIEW -> StatusLevel.NORMAL
                    TripRequestStatus.AWAITING_INFO -> StatusLevel.WARNING
                    TripRequestStatus.APPROVED -> StatusLevel.RESOLVED
                    TripRequestStatus.REJECTED -> StatusLevel.CRITICAL
                    TripRequestStatus.EXPIRED -> StatusLevel.CRITICAL
                }
                StatusBadge(label = request.status.name, status = statusLevel)
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Requester: ${request.requesterName}", style = MaterialTheme.typography.bodyMedium)
            Text("Dates: ${request.preferredDates} • Group of ${request.groupSize}", style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone(com.travellikepro.opsleader.config.AppConfig.DEFAULT_TIMEZONE)
            }
            val submittedStr = dateFormat.format(Date(request.submittedTimestamp))
            
            // SLA formatting
            val slaPassed = System.currentTimeMillis() > request.slaDeadlineTimestamp
            val slaColor = if (slaPassed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            val slaText = if (slaPassed) "SLA Breached" else "SLA: ${dateFormat.format(Date(request.slaDeadlineTimestamp))}"
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Submitted: $submittedStr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(slaText, style = MaterialTheme.typography.labelSmall, color = slaColor)
            }
        }
    }
}
