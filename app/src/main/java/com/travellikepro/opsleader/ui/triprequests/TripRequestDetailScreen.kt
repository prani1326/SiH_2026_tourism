package com.travellikepro.opsleader.ui.triprequests

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripRequestDetailScreen(
    requestId: String,
    onBack: () -> Unit
) {
    // Dummy fetching
    var request by remember {
        mutableStateOf(
            TripRequest(
                id = requestId,
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
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trip Request ${request.id}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (request.status == TripRequestStatus.NEW || request.status == TripRequestStatus.UNDER_REVIEW) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { request = request.copy(status = TripRequestStatus.AWAITING_INFO) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = "Request Info", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("More Info")
                        }
                        
                        Button(
                            onClick = { request = request.copy(status = TripRequestStatus.APPROVED) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Approve", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Approve")
                        }
                        
                        FilledTonalButton(
                            onClick = { request = request.copy(status = TripRequestStatus.REJECTED) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Reject", modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Reject")
                        }
                    }
                }
            }
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
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(request.destination, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                
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
            
            Divider()
            
            // Requester Info
            Text("Requester Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Name: ${request.requesterName}", style = MaterialTheme.typography.bodyMedium)
            Text("Contact: ${request.requesterContact}", style = MaterialTheme.typography.bodyMedium)
            Text("Source: ${request.sourceChannel}", style = MaterialTheme.typography.bodyMedium)
            
            Divider()
            
            // Trip Details
            Text("Trip Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Dates: ${request.preferredDates}", style = MaterialTheme.typography.bodyMedium)
            Text("Group Size: ${request.groupSize}", style = MaterialTheme.typography.bodyMedium)
            Text("Budget: ${request.budgetRange}", style = MaterialTheme.typography.bodyMedium)
            
            Text("Special Requirements:", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = request.specialRequirements,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Divider()
            
            // Timeline
            Text("Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (request.timeline.isEmpty()) {
                Text("No activity yet.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                request.timeline.forEach { activity ->
                    Text(
                        text = "${activity.timestamp} - ${activity.action} by ${activity.performedBy}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar
        }
    }
}
