package com.travellikepro.opsleader.ui.main.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travellikepro.opsleader.ui.components.MetricTile
import com.travellikepro.opsleader.ui.components.StatusLevel
import com.travellikepro.opsleader.ui.theme.StatusWarning

@Composable
fun DashboardScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToRequests: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Announcements Banner
        var showAnnouncement by remember { mutableStateOf(true) }
        if (showAnnouncement) {
            AnnouncementBanner(
                message = "New vendor onboarding guidelines effective today. Please review before assigning new vendors.",
                onDismiss = { showAnnouncement = false }
            )
        }

        // Profile & Workload Section
        ProfileAndWorkloadSection(onNavigateToProfile)

        // Dashboard Metrics Grid
        Text(
            text = "Operational Overview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricTile(
                title = "New Requests",
                value = "14",
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNavigateToRequests() },
                statusLevel = StatusLevel.ATTENTION
            )
            MetricTile(
                title = "Awaiting Assignment",
                value = "6",
                modifier = Modifier.weight(1f),
                statusLevel = StatusLevel.WARNING
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricTile(
                title = "Active Assignments",
                value = "8",
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                title = "Completed This Week",
                value = "23",
                modifier = Modifier.weight(1f),
                statusLevel = StatusLevel.RESOLVED
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricTile(
                title = "Vendors Available",
                value = "12",
                modifier = Modifier.weight(1f),
                statusLevel = StatusLevel.NORMAL
            )
            MetricTile(
                title = "Vendors At Capacity",
                value = "3",
                modifier = Modifier.weight(1f),
                statusLevel = StatusLevel.WARNING
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun AnnouncementBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Campaign, contentDescription = "Announcement", tint = StatusWarning)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ProfileAndWorkloadSection(onNavigateToProfile: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToProfile() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text("JD", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.titleMedium)
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Info
                Column(modifier = Modifier.weight(1f)) {
                    Text("Jane Doe", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Ops Leader • Rajasthan Region", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Status Dropdown
                var expanded by remember { mutableStateOf(false) }
                var currentStatus by remember { mutableStateOf("Active") }

                Box {
                    AssistChip(
                        onClick = { expanded = true },
                        label = { Text(currentStatus) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (currentStatus == "Away") Color.Gray else Color.Green)
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Active") },
                            onClick = { currentStatus = "Active"; expanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Away") },
                            onClick = { currentStatus = "Away"; expanded = false }
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            // Personal Workload Strip
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("My Assigned Requests", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("7 Active", style = MaterialTheme.typography.titleMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Vendor Assignments", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("4 In Progress", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
