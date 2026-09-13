package com.touristapp.presentation.safety

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.touristapp.data.models.EmergencyContact
import com.touristapp.data.models.SafetyGroupSettings
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.theme.*
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyGroupConfigScreen(
    onBackClick: () -> Unit,
    onSavedSuccessfully: () -> Unit = onBackClick
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val safetyRepository = remember { ServiceLocator.safetyRepository }

    var userName by remember { mutableStateOf("") }
    var userEmail by remember { mutableStateOf("") }
    var userPhone by remember { mutableStateOf("") }
    var selectedTimingHours by remember { mutableIntStateOf(1) }
    var contacts by remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Dialog state for Add/Edit Contact
    var showContactDialog by remember { mutableStateOf(false) }
    var contactToEdit by remember { mutableStateOf<EmergencyContact?>(null) }

    LaunchedEffect(Unit) {
        val settings = safetyRepository.getSafetyGroupSettings()
        userName = settings.userName
        userEmail = settings.userEmail
        userPhone = settings.userPhone
        selectedTimingHours = if (settings.firstAlertHours > 0) settings.firstAlertHours else 1
        contacts = settings.contacts
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Safety Group Mode", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp)
                        Text("Set up trusted contacts and safety alerts.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    }
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
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (contacts.isEmpty()) {
                                Toast.makeText(context, "Please add at least one safety contact.", Toast.LENGTH_LONG).show()
                                return@Button
                            }
                            isSaving = true
                            coroutineScope.launch {
                                val updatedSettings = SafetyGroupSettings(
                                    isEnabled = true,
                                    userName = userName.trim(),
                                    userEmail = userEmail.trim(),
                                    userPhone = userPhone.trim(),
                                    firstAlertHours = selectedTimingHours,
                                    vendorAlertMinutes = selectedTimingHours * 60,
                                    emergencyEscalationMinutes = 30,
                                    contactAlertMinutes = 60,
                                    contacts = contacts,
                                    status = "MONITORING"
                                )
                                val res = safetyRepository.saveSafetyGroupSettings(updatedSettings)
                                isSaving = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "✓ Safety Group Saved\nYour safety protection has been configured.", Toast.LENGTH_LONG).show()
                                    onSavedSuccessfully()
                                } else {
                                    Toast.makeText(context, "Failed to save safety settings: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("SAVE & ACTIVATE", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TravelPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
            ) {
                // 1. USER DETAILS CARD
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.brandPillBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Person, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Traveler Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Used in emergency alerts & vendor dispatch", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = userName,
                                onValueChange = { userName = it },
                                label = { Text("Full Name") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = userEmail,
                                onValueChange = { userEmail = it },
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = userPhone,
                                onValueChange = { userPhone = it },
                                label = { Text("Phone Number") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true
                            )
                        }
                    }
                }

                // 2. SAFETY CONTACTS SECTION WITH (+) BUTTON
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Safety Contacts", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                            Text("Will be alerted if you do not respond", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        IconButton(
                            onClick = {
                                contactToEdit = null
                                showContactDialog = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(TravelPrimary)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = "Add Contact", tint = Color.White)
                        }
                    }
                }

                if (contacts.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Safety Contacts Added Yet", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Tap the '+' button above to add family or friends.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    items(contacts, key = { it.id }) { contact ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(TravelLightPrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TravelPrimary,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(contact.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.brandPillBackground
                                        ) {
                                            Text(
                                                contact.relationship,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TravelPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                    if (contact.primaryPhone.isNotBlank()) {
                                        Text(contact.primaryPhone, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                    if (contact.alternatePhone.isNotBlank()) {
                                        Text("Alt: ${contact.alternatePhone}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            contactToEdit = contact
                                            showContactDialog = true
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = TravelPrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            contacts = contacts.filterNot { it.id == contact.id }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = TravelEmergency, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. SAFETY ALERT TIMING SELECTION
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.brandPillBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Timer, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Safety Alert Timing", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Maximum wait time when there is no response", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val timingOptions = listOf(1, 2, 3, 5)
                                timingOptions.forEach { hours ->
                                    val isSelected = selectedTimingHours == hours
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) TravelPrimary else MaterialTheme.colorScheme.background,
                                        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { selectedTimingHours = hours }
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "$hours hr",
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                "Wait",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. ESCALATION TIMELINE VISUALIZER
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Automatic Escalation Workflow", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text("Step-by-step emergency response progression", style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                            Spacer(modifier = Modifier.height(14.dp))

                            EscalationStepItem(
                                stepNumber = "1",
                                title = "No Response Detected",
                                subtitle = "Inactivity period reaches $selectedTimingHours hour(s) during active trip",
                                isHighlight = false
                            )

                            EscalationArrow()

                            EscalationStepItem(
                                stepNumber = "2",
                                title = "Vendor / Trip Support Alert",
                                subtitle = "Assigned tour leader and operations dispatch receive alert",
                                isHighlight = true
                            )

                            EscalationArrow(label = "30 Minutes")

                            EscalationStepItem(
                                stepNumber = "3",
                                title = "Emergency-Service Escalation",
                                subtitle = "High-priority operations incident logged & nearest responder routing",
                                isHighlight = true
                            )

                            EscalationArrow(label = "1 Hour")

                            EscalationStepItem(
                                stepNumber = "4",
                                title = "Saved Safety Contacts Alert",
                                subtitle = "SMS, Push & Email broadcast to all ${contacts.size} safety contacts",
                                isHighlight = true
                            )
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Safety Contact Dialog
    if (showContactDialog) {
        AddEditContactDialog(
            existingContact = contactToEdit,
            onDismiss = { showContactDialog = false },
            onSave = { newContact ->
                val current = contacts.toMutableList()
                val idx = current.indexOfFirst { it.id == newContact.id }
                if (idx >= 0) {
                    current[idx] = newContact
                } else {
                    current.add(newContact)
                }
                contacts = current
                showContactDialog = false
            }
        )
    }
}

@Composable
private fun EscalationStepItem(
    stepNumber: String,
    title: String,
    subtitle: String,
    isHighlight: Boolean
) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isHighlight) TravelPrimary else MaterialTheme.colorScheme.brandPillBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(stepNumber, fontWeight = FontWeight.Bold, color = if (isHighlight) Color.White else TravelPrimary, fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
private fun EscalationArrow(label: String? = null) {
    Column(
        modifier = Modifier
            .padding(start = 13.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(20.dp)
                    .background(TravelPrimary.copy(alpha = 0.4f))
            )
            if (label != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.brandPillBackground
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TravelPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditContactDialog(
    existingContact: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf(existingContact?.name ?: "") }
    var email by remember { mutableStateOf(existingContact?.email ?: "") }
    var primaryPhone by remember { mutableStateOf(existingContact?.primaryPhone ?: "") }
    var alternatePhone by remember { mutableStateOf(existingContact?.alternatePhone ?: "") }
    var relationship by remember { mutableStateOf(existingContact?.relationship ?: "Family") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (existingContact == null) "Add Safety Contact" else "Edit Safety Contact",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Save trusted family or friends for safety escalations.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = primaryPhone,
                    onValueChange = { primaryPhone = it },
                    label = { Text("Primary Phone Number *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = alternatePhone,
                    onValueChange = { alternatePhone = it },
                    label = { Text("Alternate Phone (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text("Relationship (e.g. Mother, Spouse, Friend)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank() || primaryPhone.isBlank()) {
                                return@Button
                            }
                            val contact = existingContact?.copy(
                                name = name.trim(),
                                primaryPhone = primaryPhone.trim(),
                                alternatePhone = alternatePhone.trim(),
                                email = email.trim(),
                                relationship = relationship.trim().ifBlank { "Family" }
                            ) ?: EmergencyContact(
                                id = UUID.randomUUID().toString(),
                                name = name.trim(),
                                primaryPhone = primaryPhone.trim(),
                                alternatePhone = alternatePhone.trim(),
                                email = email.trim(),
                                relationship = relationship.trim().ifBlank { "Family" }
                            )
                            onSave(contact)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                        enabled = name.isNotBlank() && primaryPhone.isNotBlank()
                    ) {
                        Text("SAVE CONTACT", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
