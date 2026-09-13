package com.touristapp.presentation.safety

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.data.models.LostPhoneSettings
import com.touristapp.data.models.OfficialEmergencyNumber
import com.touristapp.data.models.SafetyGroupSettings
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScreen(
    onBackClick: () -> Unit,
    onOpenEmergencySos: () -> Unit,
    onOpenEmergencyContacts: () -> Unit,
    onOpenNearbyHelp: () -> Unit,
    onOpenLostPhoneSetup: () -> Unit,
    onOpenSafetyGroupConfig: () -> Unit,
    onOpenIncidentReport: () -> Unit,
    onSosTrigger: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val safetyRepository = remember { ServiceLocator.safetyRepository }

    var lostPhoneSettings by remember { mutableStateOf(LostPhoneSettings()) }
    var safetyGroupSettings by remember { mutableStateOf(SafetyGroupSettings()) }
    var contactsCount by remember { mutableIntStateOf(2) }

    LaunchedEffect(Unit) {
        lostPhoneSettings = safetyRepository.getLostPhoneSettings()
        safetyGroupSettings = safetyRepository.getSafetyGroupSettings()
        val contacts = safetyRepository.getEmergencyContacts()
        contactsCount = contacts.size
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Safety & Security", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 32.dp)
        ) {
            // 1. EMERGENCY ASSISTANCE HERO CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenEmergencySos() },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = TravelEmergency),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            "One-Tap Emergency SOS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            "Dispatches your GPS coordinates to responders, contacts, and trip leaders.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onOpenEmergencySos,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("DISPATCH SOS NOW", color = TravelEmergency, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // 2. QUICK ACTION FEATURE TILES (Nearby Help / Report Incident / Contacts / Live Location)
            item {
                Text(
                    "EMERGENCY & SAFETY TOOLS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SafetyFeatureCard(
                            icon = Icons.Filled.People,
                            title = "Emergency Contacts",
                            subtitle = "$contactsCount Saved",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenEmergencyContacts
                        )
                        SafetyFeatureCard(
                            icon = Icons.Filled.MyLocation,
                            title = "Live Location",
                            subtitle = "GPS Tracking",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenEmergencySos
                        )
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        SafetyFeatureCard(
                            icon = Icons.Filled.LocalHospital,
                            title = "Nearby Help",
                            subtitle = "Police & Hospital",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenNearbyHelp
                        )
                        SafetyFeatureCard(
                            icon = Icons.Filled.ReportProblem,
                            title = "Report Incident",
                            subtitle = "Safety Log",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenIncidentReport
                        )
                    }
                }
            }

            // 3. LOST PHONE MODE CARD WITH ON/OFF TOGGLE
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (lostPhoneSettings.isEnabled) TravelSuccess.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.PhonelinkLock,
                                    contentDescription = null,
                                    tint = if (lostPhoneSettings.isEnabled) TravelSuccess else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lost Phone Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    if (lostPhoneSettings.isEnabled) "Protection ON • ${lostPhoneSettings.safetyTimeoutHours}h Safety Timeout" else "Protection OFF",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (lostPhoneSettings.isEnabled) TravelSuccess else Color.Gray
                                )
                            }
                            Switch(
                                checked = lostPhoneSettings.isEnabled,
                                onCheckedChange = { checked ->
                                    val updated = lostPhoneSettings.copy(isEnabled = checked)
                                    lostPhoneSettings = updated
                                    coroutineScope.launch {
                                        safetyRepository.saveLostPhoneSettings(updated)
                                    }
                                    if (checked) {
                                        Toast.makeText(context, "Lost Phone Mode Enabled", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Lost Phone Mode Disabled", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TravelPrimary,
                                    checkedTrackColor = TravelLightPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLostPhoneSetup() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Configure Contacts & Recovery PIN",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TravelPrimary
                            )
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 4. SAFETY GROUP MODE CARD WITH ON/OFF TOGGLE (Placed immediately BELOW Lost Phone Mode)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (safetyGroupSettings.isEnabled) TravelSuccess.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = if (safetyGroupSettings.isEnabled) TravelSuccess else Color.Gray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Safety Group Mode", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    "Automatic safety alerts if you don't respond",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (safetyGroupSettings.isEnabled) "Protection ACTIVE" else "Protection OFF",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (safetyGroupSettings.isEnabled) TravelSuccess else Color.Gray
                                )
                            }
                            Switch(
                                checked = safetyGroupSettings.isEnabled,
                                onCheckedChange = { checked ->
                                    val updated = safetyGroupSettings.copy(isEnabled = checked)
                                    safetyGroupSettings = updated
                                    coroutineScope.launch {
                                        safetyRepository.saveSafetyGroupSettings(updated)
                                    }
                                    if (checked) {
                                        Toast.makeText(context, "Safety Group Mode Enabled (Protection ACTIVE)", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Safety Group Mode Disabled (Protection OFF)", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = TravelPrimary,
                                    checkedTrackColor = TravelLightPrimary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenSafetyGroupConfig() },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Configure Safety Group >",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TravelPrimary
                            )
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 5. OFFICIAL INDIAN HELPLINES (Centralized data source + accessible 1-tap call button)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Official Emergency Numbers (India)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(12.dp))

                        OfficialEmergencyNumber.INDIA_LIST.forEachIndexed { index, emergencyItem ->
                            HelplineRowItem(item = emergencyItem, context = context)
                            if (index < OfficialEmergencyNumber.INDIA_LIST.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SafetyFeatureCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.brandPillBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
private fun HelplineRowItem(
    item: OfficialEmergencyNumber,
    context: Context
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = TravelPrimary
            ) {
                Text(
                    text = item.number,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(item.description, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.number}"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.brandPillBackground)
            ) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = "Call ${item.name} (${item.number})",
                    tint = TravelPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
