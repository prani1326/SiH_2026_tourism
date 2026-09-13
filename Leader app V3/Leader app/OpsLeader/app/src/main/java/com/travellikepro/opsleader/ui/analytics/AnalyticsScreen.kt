package com.travellikepro.opsleader.ui.analytics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travellikepro.opsleader.data.api.OperationalReportDto
import com.travellikepro.opsleader.ui.components.MetricTile
import com.travellikepro.opsleader.ui.components.StatusLevel

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun AnalyticsScreen() {
    var showGenerateDialog by remember { mutableStateOf(false) }

    var reports by remember {
        mutableStateOf(
            listOf(
                OperationalReportDto(
                    id = "REP-2026-08",
                    title = "Monthly Ops & Partner Performance Audit",
                    period = "August 2026",
                    total_trips = 142,
                    total_revenue = 184500.0,
                    incident_count = 3,
                    average_resolution_time_minutes = 24,
                    generated_at = "Sep 01, 2026"
                ),
                OperationalReportDto(
                    id = "REP-2026-07",
                    title = "Mid-Year Safety & Incident Compliance Review",
                    period = "July 2026",
                    total_trips = 128,
                    total_revenue = 159200.0,
                    incident_count = 5,
                    average_resolution_time_minutes = 31,
                    generated_at = "Aug 01, 2026"
                )
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Analytics & Reports", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { showGenerateDialog = true }) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Generate Report")
            }
        }

        // Key KPI Matrix
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricTile(
                title = "Total Active Trips",
                value = "28",
                modifier = Modifier.weight(1f),
                statusLevel = StatusLevel.RESOLVED
            )
            MetricTile(
                title = "Avg Resolution",
                value = "18m",
                modifier = Modifier.weight(1f),
                statusLevel = StatusLevel.NORMAL
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricTile(
                title = "Partner Satisfaction",
                value = "4.87 / 5",
                modifier = Modifier.weight(1f),
                statusLevel = StatusLevel.NORMAL
            )
            MetricTile(
                title = "Safety Index",
                value = "99.4%",
                modifier = Modifier.weight(1f),
                statusLevel = StatusLevel.RESOLVED
            )
        }

        Text("Regional Workload Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RegionalBar(region = "Rajasthan (Jaipur, Udaipur)", share = "42% of volume", progress = 0.42f)
                RegionalBar(region = "Goa Coastal Tours", share = "28% of volume", progress = 0.28f)
                RegionalBar(region = "Kerala Backwaters & Munnar", share = "20% of volume", progress = 0.20f)
                RegionalBar(region = "Golden Triangle / Delhi", share = "10% of volume", progress = 0.10f)
            }
        }

        Text("Generated Intelligence Reports", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        reports.forEach { report ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(report.id, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Text(report.period, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(report.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("• Completed Trips: ${report.total_trips} | Revenue: $${report.total_revenue}", style = MaterialTheme.typography.bodySmall)
                    Text("• Safety Incidents: ${report.incident_count} | Avg Resolution: ${report.average_resolution_time_minutes}m", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showGenerateDialog) {
        var reportType by remember { mutableStateOf("MONTHLY_OPS") }

        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text("Generate Operational Report") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Report Scope:", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = reportType == "MONTHLY_OPS", onClick = { reportType = "MONTHLY_OPS" })
                        Text("Full Monthly Operations Summary")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = reportType == "INCIDENTS_SUMMARY", onClick = { reportType = "INCIDENTS_SUMMARY" })
                        Text("Safety & Incident Resolution Audit")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = reportType == "PARTNER_PERFORMANCE", onClick = { reportType = "PARTNER_PERFORMANCE" })
                        Text("Vendor & Partner Quality Ratings")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reports = listOf(
                            OperationalReportDto(
                                id = "REP-2026-09",
                                title = when (reportType) {
                                    "INCIDENTS_SUMMARY" -> "Real-time Safety & Incident Audit"
                                    "PARTNER_PERFORMANCE" -> "Quarterly Partner Quality Evaluation"
                                    else -> "September 2026 Operations Report"
                                },
                                period = "September 2026",
                                total_trips = 58,
                                total_revenue = 74200.0,
                                incident_count = 1,
                                average_resolution_time_minutes = 15,
                                generated_at = "Just now"
                            )
                        ) + reports
                        showGenerateDialog = false
                    }
                ) { Text("Generate & Download") }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun RegionalBar(region: String, share: String, progress: Float) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(region, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(share, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surface
        )
    }
}
