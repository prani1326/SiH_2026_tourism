package com.touristapp.presentation.trips.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import com.touristapp.data.models.CrowdIntelligenceDto
import com.touristapp.ui.theme.PrimaryOrange
import com.touristapp.ui.theme.TravelPrimary

@Composable
fun CrowdIntelligenceCard(
    destinationName: String = "Amber Fort",
    crowdData: CrowdIntelligenceDto? = null,
    onAlternativeClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showAlternatives by remember { mutableStateOf(false) }

    val crowdPercentage = crowdData?.crowd_density_pct ?: 78
    val crowdLevel = crowdData?.crowd_badge ?: "HIGH"
    val waitTime = crowdData?.estimated_wait_min ?: 30
    val bestTime = crowdData?.best_visiting_window ?: "8:00 AM – 10:00 AM"
    val trafficLevel = crowdData?.traffic_level ?: "Moderate"
    val alternatives = crowdData?.alternatives ?: emptyList()

    val levelColor = when (crowdLevel.uppercase()) {
        "LOW" -> Color(0xFF2E7D32)
        "MODERATE", "MEDIUM" -> Color(0xFFF57C00)
        "HIGH", "VERY_HIGH" -> Color(0xFFD32F2F)
        else -> Color(0xFFF57C00)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header: Title & Live Crowd Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = levelColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = destinationName.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = levelColor.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(levelColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$crowdLevel CROWD ($crowdPercentage%)",
                            color = levelColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3-Metric Bar: Wait Time, Best Window, Traffic
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InfoPill(
                    icon = Icons.Filled.Timer,
                    label = "Est. Wait",
                    value = "$waitTime min",
                    modifier = Modifier.weight(1f)
                )
                InfoPill(
                    icon = Icons.Filled.WbTwilight,
                    label = "Best Time",
                    value = bestTime,
                    modifier = Modifier.weight(1.3f)
                )
                InfoPill(
                    icon = Icons.Filled.Traffic,
                    label = "Traffic",
                    value = trafficLevel,
                    modifier = Modifier.weight(1f)
                )
            }

            // Alternatives toggle button
            if (alternatives.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = { showAlternatives = !showAlternatives },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = if (showAlternatives) Icons.Filled.ExpandLess else Icons.Filled.AltRoute,
                        contentDescription = null,
                        tint = TravelPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (showAlternatives) "Hide Alternatives" else "View Less Crowded Alternatives (${alternatives.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TravelPrimary
                    )
                }

                // Alternatives List
                AnimatedVisibility(visible = showAlternatives) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        alternatives.forEach { alt ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onAlternativeClick(alt.name) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = alt.name,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${alt.distance_away} • ${alt.crowd_level}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF2E7D32).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "Low Wait",
                                            color = Color(0xFF2E7D32),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }
    }
}
