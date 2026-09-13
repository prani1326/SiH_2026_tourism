package com.touristapp.presentation.safety

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.data.models.LostPhoneSettings
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostPhoneSetupScreen(
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val safetyRepo = remember { ServiceLocator.safetyRepository }

    var isEnabled by remember { mutableStateOf(false) }
    var primaryEmail by remember { mutableStateOf("") }
    var primaryPhone by remember { mutableStateOf("") }
    var alternateEmail by remember { mutableStateOf("") }
    var alternatePhone by remember { mutableStateOf("") }
    var trustedName by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Family") }
    var emergencyMessage by remember { mutableStateOf("This device has not shown activity for the safety period. Please contact the traveler.") }
    var selectedTimeoutHours by remember { mutableIntStateOf(3) }
    var recoveryPin by remember { mutableStateOf("") }

    var isSaving by remember { mutableStateOf(false) }
    var saveSuccessMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val settings = safetyRepo.getLostPhoneSettings()
        isEnabled = settings.isEnabled
        primaryEmail = settings.primaryContactEmail
        primaryPhone = settings.primaryContactPhone
        alternateEmail = settings.alternateContactEmail
        alternatePhone = settings.alternateContactPhone
        trustedName = settings.trustedContactName
        relationship = settings.relationship
        emergencyMessage = settings.emergencyMessage
        selectedTimeoutHours = settings.safetyTimeoutHours
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Lost Phone Protection", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            isSaving = true
                            errorMessage = null
                            saveSuccessMessage = null
                            coroutineScope.launch {
                                val current = safetyRepo.getLostPhoneSettings()
                                val updated = current.copy(
                                    isEnabled = isEnabled,
                                    primaryContactEmail = primaryEmail.trim(),
                                    primaryContactPhone = primaryPhone.trim(),
                                    alternateContactEmail = alternateEmail.trim(),
                                    alternateContactPhone = alternatePhone.trim(),
                                    trustedContactName = trustedName.trim(),
                                    relationship = relationship,
                                    emergencyMessage = emergencyMessage.trim(),
                                    safetyTimeoutHours = selectedTimeoutHours
                                )
                                val res = safetyRepo.saveLostPhoneSettings(updated)
                                isSaving = false
                                res.onSuccess {
                                    saveSuccessMessage = "Lost Phone settings saved securely!"
                                }.onFailure {
                                    errorMessage = it.localizedMessage ?: "Failed to save settings."
                                }
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SAVE LOST PHONE SETTINGS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Feature Enable Card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (isEnabled) MaterialTheme.colorScheme.brandPillBackground else MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, if (isEnabled) TravelPrimary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.PhonelinkLock,
                        contentDescription = null,
                        tint = if (isEnabled) TravelPrimary else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Lost Phone Mode",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (isEnabled) "Active: Monitored via safety checks" else "Disabled: Turn on for automated checks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TravelPrimary,
                            checkedTrackColor = TravelLightPrimary,
                            uncheckedThumbColor = Color(0xFFE0E0E0),
                            uncheckedTrackColor = Color(0xFFF0F0F0),
                            uncheckedBorderColor = Color(0xFFD0D0D0)
                        )
                    )
                }
            }

            // Staged Process Explanation
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        "STAGED SAFETY PROTOCOL",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall,
                        color = TravelPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "1. Safety Timeout reached\n2. Device verifies activity / sends local confirmation\n3. Grace period before alert\n4. Verified message sent to safety contact only if unconfirmed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }

            if (saveSuccessMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TravelSuccess.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TravelSuccess, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(saveSuccessMessage ?: "", color = TravelSuccess, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TravelEmergency.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = TravelEmergency, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage ?: "", color = TravelEmergency, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Safety Timeout Selector
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Safety Check Timeout", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Start check if app is inactive without confirmation for:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(2, 3, 5, 8).forEach { hours ->
                            val isSelected = selectedTimeoutHours == hours
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedTimeoutHours = hours },
                                label = { Text("${hours} Hours", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                modifier = Modifier.weight(1f),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TravelPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // Contact Form
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Primary Safety Contact", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

                    OutlinedTextField(
                        value = primaryEmail,
                        onValueChange = { primaryEmail = it },
                        label = { Text("Primary Email Address *") },
                        placeholder = { Text("contact@example.com") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = primaryPhone,
                        onValueChange = { primaryPhone = it },
                        label = { Text("Primary Phone Number *") },
                        placeholder = { Text("+91 9876543210") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text("Alternate Contact (Optional)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

                    OutlinedTextField(
                        value = alternateEmail,
                        onValueChange = { alternateEmail = it },
                        label = { Text("Alternate Email") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = alternatePhone,
                        onValueChange = { alternatePhone = it },
                        label = { Text("Alternate Phone") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text("Emergency Message to Contact", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)

                    OutlinedTextField(
                        value = emergencyMessage,
                        onValueChange = { emergencyMessage = it },
                        label = { Text("Alert Message") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
