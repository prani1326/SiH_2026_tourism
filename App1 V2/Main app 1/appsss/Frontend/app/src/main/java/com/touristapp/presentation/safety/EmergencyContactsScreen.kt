package com.touristapp.presentation.safety

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.data.models.EmergencyContact
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyContactsScreen(
    onBackClick: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val safetyRepo = remember { ServiceLocator.safetyRepository }

    var contacts by remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedContactForEdit by remember { mutableStateOf<EmergencyContact?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf<EmergencyContact?>(null) }

    fun refresh() {
        coroutineScope.launch {
            isLoading = true
            contacts = safetyRepo.getEmergencyContacts()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Emergency Contacts", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            selectedContactForEdit = null
                            showEditDialog = true
                        }
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = "Add Contact", tint = TravelPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    selectedContactForEdit = null
                    showEditDialog = true
                },
                containerColor = TravelPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Contact", fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            // Privacy & Disclosure Banner
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.brandPillBackground,
                    border = BorderStroke(1.dp, TravelPrimary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = TravelPrimary,
                            modifier = Modifier.size(24.dp).padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Emergency Privacy Guarantee",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Contacts only receive notifications and location coordinates during an activated SOS or verified Lost Phone safety check.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = TravelPrimary)
                    }
                }
            } else if (contacts.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.GroupOff, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No Emergency Contacts Added", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Add family members, trip companions, or trusted friends to receive immediate emergency dispatch alerts.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(contacts, key = { it.id }) { contact ->
                    EmergencyContactCard(
                        contact = contact,
                        onEdit = {
                            selectedContactForEdit = contact
                            showEditDialog = true
                        },
                        onDelete = {
                            showDeleteConfirmDialog = contact
                        },
                        onToggleEnabled = { enabled ->
                            coroutineScope.launch {
                                safetyRepo.saveEmergencyContact(contact.copy(isEnabled = enabled))
                                refresh()
                            }
                        }
                    )
                }
            }
        }
    }

    // Add / Edit Dialog
    if (showEditDialog) {
        ContactEditDialog(
            existing = selectedContactForEdit,
            onDismiss = { showEditDialog = false },
            onSave = { saved ->
                coroutineScope.launch {
                    safetyRepo.saveEmergencyContact(saved)
                    showEditDialog = false
                    refresh()
                }
            }
        )
    }

    // Delete Confirmation
    if (showDeleteConfirmDialog != null) {
        val target = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Delete Contact", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove ${target.name} from emergency dispatch contacts?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            safetyRepo.deleteEmergencyContact(target.id)
                            showDeleteConfirmDialog = null
                            refresh()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelEmergency)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmergencyContactCard(
    contact: EmergencyContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
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
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(TravelPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = TravelPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            contact.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = TravelPrimary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                contact.relationship,
                                color = TravelPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        contact.primaryPhone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }

                Switch(
                    checked = contact.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TravelPrimary,
                        checkedTrackColor = TravelLightPrimary,
                        uncheckedThumbColor = Color(0xFFE0E0E0),
                        uncheckedTrackColor = Color(0xFFF0F0F0),
                        uncheckedBorderColor = Color(0xFFD0D0D0)
                    )
                )
            }

            if (contact.email.isNotBlank() || contact.alternatePhone.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    if (contact.email.isNotBlank()) {
                        Text(
                            "✉️ ${contact.email}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    if (contact.isLocationSharingAllowed) {
                        Text(
                            "📍 Location Sharing Permitted",
                            style = MaterialTheme.typography.labelSmall,
                            color = TravelSuccess,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = TravelPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", color = TravelPrimary, fontSize = 12.sp)
                }
                TextButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp), tint = TravelEmergency)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", color = TravelEmergency, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun ContactEditDialog(
    existing: EmergencyContact?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContact) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var relationship by remember { mutableStateOf(existing?.relationship ?: "Family") }
    var primaryPhone by remember { mutableStateOf(existing?.primaryPhone ?: "") }
    var alternatePhone by remember { mutableStateOf(existing?.alternatePhone ?: "") }
    var email by remember { mutableStateOf(existing?.email ?: "") }
    var isLocationSharingAllowed by remember { mutableStateOf(existing?.isLocationSharingAllowed ?: true) }

    val relationships = listOf("Family", "Spouse", "Friend", "Trip Leader", "Colleague", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Add Emergency Contact" else "Edit Contact", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = primaryPhone,
                    onValueChange = { primaryPhone = it },
                    label = { Text("Primary Phone Number *") },
                    placeholder = { Text("+91 9876543210") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = alternatePhone,
                    onValueChange = { alternatePhone = it },
                    label = { Text("Alternate Phone (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Relationship", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    relationships.take(3).forEach { rel ->
                        FilterChip(
                            selected = relationship == rel,
                            onClick = { relationship = rel },
                            label = { Text(rel, fontSize = 11.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Allow Live GPS Sharing in SOS", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isLocationSharingAllowed,
                        onCheckedChange = { isLocationSharingAllowed = it },
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = (existing ?: EmergencyContact()).copy(
                        name = name.trim(),
                        relationship = relationship,
                        primaryPhone = primaryPhone.trim(),
                        alternatePhone = alternatePhone.trim(),
                        email = email.trim(),
                        isLocationSharingAllowed = isLocationSharingAllowed,
                        isEnabled = true
                    )
                    onSave(result)
                },
                enabled = name.isNotBlank() && primaryPhone.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
            ) {
                Text("Save Contact")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
