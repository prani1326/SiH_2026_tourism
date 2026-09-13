package com.touristapp.presentation.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.touristapp.ui.theme.PrimaryOrange
import com.touristapp.ui.theme.TravelPrimary

data class AccessibilityPreferences(
    val wheelchairAccess: Boolean = false,
    val avoidStairs: Boolean = false,
    val maxWalkingKmPerDay: Float = 3.0f,
    val frequentRestBreaks: Boolean = false,
    val elderlyFriendly: Boolean = false,
    val childFriendly: Boolean = false,
    val visualAssistance: Boolean = false,
    val hearingAssistance: Boolean = false
)

@Composable
fun AccessibilityPreferencesDialog(
    initialPreferences: AccessibilityPreferences = AccessibilityPreferences(),
    onDismiss: () -> Unit,
    onSave: (AccessibilityPreferences) -> Unit
) {
    var wheelchair by remember { mutableStateOf(initialPreferences.wheelchairAccess) }
    var avoidStairs by remember { mutableStateOf(initialPreferences.avoidStairs) }
    var maxWalking by remember { mutableStateOf(initialPreferences.maxWalkingKmPerDay) }
    var restBreaks by remember { mutableStateOf(initialPreferences.frequentRestBreaks) }
    var elderly by remember { mutableStateOf(initialPreferences.elderlyFriendly) }
    var childFriendly by remember { mutableStateOf(initialPreferences.childFriendly) }
    var visualAssist by remember { mutableStateOf(initialPreferences.visualAssistance) }
    var hearingAssist by remember { mutableStateOf(initialPreferences.hearingAssistance) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Accessible,
                        contentDescription = null,
                        tint = PrimaryOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Accessibility Preferences",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AI will tailor trip itineraries to your comfort",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // 1. Mobility Options
                Text(
                    text = "Mobility & Step-Free Travel",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
                )
                Spacer(modifier = Modifier.height(8.dp))

                PreferenceSwitchRow(
                    title = "Wheelchair Accessibility",
                    subtitle = "Prioritize ramps, elevators & flat pathways",
                    icon = Icons.Filled.AccessibleForward,
                    checked = wheelchair,
                    onCheckedChange = { wheelchair = it }
                )

                PreferenceSwitchRow(
                    title = "Avoid Stairs & Steep Inclines",
                    subtitle = "Skip monument towers & step wells without lift",
                    icon = Icons.Filled.Stairs,
                    checked = avoidStairs,
                    onCheckedChange = { avoidStairs = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Walking Limits Slider
                Text(
                    text = "Max Walking Distance: ${String.format("%.1f", maxWalking)} km/day",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = maxWalking,
                    onValueChange = { maxWalking = it },
                    valueRange = 0.5f..10.0f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryOrange,
                        activeTrackColor = PrimaryOrange
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Comfort & Family Settings
                Text(
                    text = "Comfort & Family Profiles",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
                )
                Spacer(modifier = Modifier.height(8.dp))

                PreferenceSwitchRow(
                    title = "Frequent Rest Breaks",
                    subtitle = "Schedule stops every 90 minutes near shaded seating",
                    icon = Icons.Filled.AirlineSeatReclineNormal,
                    checked = restBreaks,
                    onCheckedChange = { restBreaks = it }
                )

                PreferenceSwitchRow(
                    title = "Elderly Traveler Mode",
                    subtitle = "Slow-paced itinerary with close vehicle drop-offs",
                    icon = Icons.Filled.Elderly,
                    checked = elderly,
                    onCheckedChange = { elderly = it }
                )

                PreferenceSwitchRow(
                    title = "Child & Stroller Friendly",
                    subtitle = "Paved paths and kid-safe attractions",
                    icon = Icons.Filled.ChildCare,
                    checked = childFriendly,
                    onCheckedChange = { childFriendly = it }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 4. Sensory & Audio-Visual
                Text(
                    text = "Sensory Support",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryOrange
                )
                Spacer(modifier = Modifier.height(8.dp))

                PreferenceSwitchRow(
                    title = "Audio Narration & High Contrast",
                    subtitle = "Enable Heritage Lens auto-TTS and large typography",
                    icon = Icons.Filled.Visibility,
                    checked = visualAssist,
                    onCheckedChange = { visualAssist = it }
                )

                PreferenceSwitchRow(
                    title = "Signage & Visual Alerts",
                    subtitle = "Visual crowd and safety notifications",
                    icon = Icons.Filled.Hearing,
                    checked = hearingAssist,
                    onCheckedChange = { hearingAssist = it }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updated = AccessibilityPreferences(
                                wheelchairAccess = wheelchair,
                                avoidStairs = avoidStairs,
                                maxWalkingKmPerDay = maxWalking,
                                frequentRestBreaks = restBreaks,
                                elderlyFriendly = elderly,
                                childFriendly = childFriendly,
                                visualAssistance = visualAssist,
                                hearingAssistance = hearingAssist
                            )
                            onSave(updated)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Save Preferences", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PrimaryOrange
            )
        )
    }
}
