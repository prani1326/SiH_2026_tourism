package com.touristapp.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.touristapp.presentation.profile.components.KycVerificationDialog
import com.touristapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogoutClick: () -> Unit,
    onBookingsClick: () -> Unit = {},
    onToolsClick: () -> Unit = {},
    onSupportClick: () -> Unit = {},
    onOpenOnboarding: () -> Unit = {},
    onGuardianClick: () -> Unit = {},
    onSafetyClick: () -> Unit = {},
    onScamShieldClick: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel()
) {
    val user by viewModel.currentUser.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val editFullName by viewModel.editFullName.collectAsState()
    val editBio by viewModel.editBio.collectAsState()
    val editDob by viewModel.editDob.collectAsState()
    val editGender by viewModel.editGender.collectAsState()
    val editNationality by viewModel.editNationality.collectAsState()
    val editLanguage by viewModel.editLanguage.collectAsState()
    val editPhone by viewModel.editPhone.collectAsState()
    val editAddressLine1 by viewModel.editAddressLine1.collectAsState()
    val editCity by viewModel.editCity.collectAsState()
    val editState by viewModel.editState.collectAsState()
    val editCountry by viewModel.editCountry.collectAsState()
    val editPostalCode by viewModel.editPostalCode.collectAsState()

    val fullName = user?.full_name?.takeIf { it.isNotBlank() } ?: "Traveler"
    val initials = fullName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }.joinToString("")
    val email = user?.email ?: "No email provided"
    val avatarUrl = user?.profile?.avatar_url

    // Calculation of profile completion
    val profileCompletionPercent = uiState.eligibility?.profile_completion ?: user?.profile_completion ?: 40f
    val completenessProgress = (profileCompletionPercent / 100f).coerceIn(0f, 1f)

    // KYC Status
    val kycStatus = uiState.eligibility?.kyc_status ?: user?.kyc_status ?: "NOT_STARTED"
    val isKycVerified = uiState.eligibility?.kyc_verified ?: user?.kyc_verified ?: false

    var showKycDialog by remember { mutableStateOf(false) }

    if (showKycDialog) {
        KycVerificationDialog(
            onDismiss = { showKycDialog = false },
            onVerificationSuccess = {
                viewModel.refreshProfile()
                viewModel.checkEligibility()
            }
        )
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadAvatar(context, it) }
    }

    // Show snackbar on success/error
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.successMessage, uiState.error) {
        val msg = uiState.successMessage ?: uiState.error
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    val currentThemeMode by viewModel.themeMode.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccessibilityDialog by remember { mutableStateOf(false) }

    if (showAccessibilityDialog) {
        com.touristapp.presentation.profile.components.AccessibilityPreferencesDialog(
            onDismiss = { showAccessibilityDialog = false },
            onSave = {
                showAccessibilityDialog = false
                android.widget.Toast.makeText(context, "Accessibility preferences updated!", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    "Select App Theme",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AppThemeMode.SYSTEM to "System Default",
                        AppThemeMode.LIGHT to "Light Mode",
                        AppThemeMode.DARK to "Dark Mode"
                    ).forEach { (mode, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (currentThemeMode == mode),
                                onClick = {
                                    viewModel.setThemeMode(mode)
                                    showThemeDialog = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = TravelPrimary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("Close", color = TravelPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                actions = {
                    if (uiState.isEditMode) {
                        TextButton(onClick = { viewModel.toggleEditMode() }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                        }
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 8.dp))
                        } else {
                            TextButton(onClick = { viewModel.saveProfile() }) {
                                Text("Save", color = TravelPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        IconButton(onClick = { viewModel.toggleEditMode() }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Profile", tint = TravelPrimary)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {

            // Avatar + Name Header + Completion Indicator
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(TravelPrimary.copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(110.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                progress = { completenessProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = TravelPrimary,
                                strokeWidth = 4.dp,
                                trackColor = TravelPrimary.copy(alpha = 0.15f)
                            )
                            Box(
                                modifier = Modifier
                                    .size(98.dp)
                                    .clip(CircleShape)
                                    .background(TravelPrimary)
                                    .clickable { if (uiState.isEditMode) imagePickerLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (!avatarUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(initials, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                if (uiState.isEditMode) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AnimatedContent(targetState = uiState.isEditMode, label = "nameAnim") { isEditing ->
                            if (isEditing) {
                                OutlinedTextField(
                                    value = editFullName,
                                    onValueChange = { viewModel.editFullName.value = it },
                                    label = { Text("Full Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(fullName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${profileCompletionPercent.toInt()}% profile complete",
                            style = MaterialTheme.typography.labelMedium,
                            color = TravelPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ==========================================
            // IDENTITY & KYC STATUS CARD (SECTION 7 & 8)
            // ==========================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isKycVerified) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isKycVerified) Color(0xFF81C784) else TravelPrimary.copy(alpha = 0.3f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isKycVerified) Icons.Filled.Verified else Icons.Filled.Badge,
                                    contentDescription = null,
                                    tint = if (isKycVerified) Color(0xFF2E7D32) else TravelPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "IDENTITY & KYC",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isKycVerified) Color(0xFF2E7D32) else TravelPrimary,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Status Pill
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (kycStatus) {
                                    "VERIFIED" -> Color(0xFFC8E6C9)
                                    "PENDING" -> Color(0xFFFFF3E0)
                                    "FAILED" -> Color(0xFFFFEBEE)
                                    else -> Color(0xFFEEEEEE)
                                }
                            ) {
                                Text(
                                    when (kycStatus) {
                                        "VERIFIED" -> "🟢 Verified"
                                        "PENDING" -> "🟡 Pending"
                                        "FAILED" -> "🔴 Failed"
                                        else -> "⚪ Not Completed"
                                    },
                                    color = when (kycStatus) {
                                        "VERIFIED" -> Color(0xFF1B5E20)
                                        "PENDING" -> Color(0xFFE65100)
                                        "FAILED" -> Color(0xFFB71C1C)
                                        else -> Color(0xFF424242)
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isKycVerified) {
                            Text(
                                "Your identity is verified. Trip Planning, booking services, and verified traveler benefits are fully unlocked.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF2E7D32),
                                lineHeight = 18.sp
                            )
                        } else {
                            Text(
                                if (kycStatus == "PENDING") "Your document verification is currently under review."
                                else if (kycStatus == "FAILED") "Identity verification could not be completed. Please retry with a valid document."
                                else "Complete KYC verification to unlock AI Trip Planning and travel booking services.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = { showKycDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                            ) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (kycStatus == "FAILED") "Retry Verification"
                                    else "Verify Identity",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // EDIT PROFILE MODE
            // ==========================================
            item {
                AnimatedVisibility(visible = uiState.isEditMode, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Edit Personal Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                            if (isKycVerified) {
                                Surface(
                                    color = Color(0xFFFFF3E0),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "⚠️ This information is linked to your verified identity. Changing critical fields may require re-verification.",
                                        fontSize = 11.sp,
                                        color = Color(0xFFE65100),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = editBio,
                                onValueChange = { viewModel.editBio.value = it },
                                label = { Text("Bio") },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editDob,
                                onValueChange = { viewModel.editDob.value = it },
                                label = { Text("Date of Birth (YYYY-MM-DD)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editNationality,
                                onValueChange = { viewModel.editNationality.value = it },
                                label = { Text("Nationality") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editLanguage,
                                onValueChange = { viewModel.editLanguage.value = it },
                                label = { Text("Language") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { viewModel.editPhone.value = it },
                                label = { Text("Phone") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = editAddressLine1,
                                onValueChange = { viewModel.editAddressLine1.value = it },
                                label = { Text("Address Line 1") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editCity,
                                    onValueChange = { viewModel.editCity.value = it },
                                    label = { Text("City") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = editState,
                                    onValueChange = { viewModel.editState.value = it },
                                    label = { Text("State") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = editCountry,
                                    onValueChange = { viewModel.editCountry.value = it },
                                    label = { Text("Country") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = editPostalCode,
                                    onValueChange = { viewModel.editPostalCode.value = it },
                                    label = { Text("Postal Code") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // ==========================================
            // PERSONAL INFORMATION CARD (VIEW MODE)
            // ==========================================
            item {
                AnimatedVisibility(visible = !uiState.isEditMode, enter = fadeIn(), exit = fadeOut()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PERSONAL INFORMATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TravelPrimary)
                            Spacer(modifier = Modifier.height(10.dp))

                            val personalItems = listOf(
                                Triple(Icons.Filled.Person, "Full Name", user?.full_name?.takeIf { it.isNotBlank() } ?: "—"),
                                Triple(Icons.Filled.CalendarMonth, "Date of Birth", user?.profile?.date_of_birth?.takeIf { it.isNotBlank() } ?: "—"),
                                Triple(Icons.Filled.LocationOn, "Nationality", user?.profile?.nationality?.takeIf { it.isNotBlank() } ?: "Indian"),
                                Triple(Icons.Filled.Language, "Preferred Language", user?.profile?.language?.takeIf { it.isNotBlank() } ?: "English"),
                                Triple(Icons.Filled.Wc, "Gender", user?.profile?.gender?.takeIf { it.isNotBlank() } ?: "Prefer not to say")
                            )

                            personalItems.forEachIndexed { idx, (icon, label, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (idx < personalItems.size - 1) HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // CONTACT & ADDRESS CARD (VIEW MODE)
            // ==========================================
            item {
                AnimatedVisibility(visible = !uiState.isEditMode, enter = fadeIn(), exit = fadeOut()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("CONTACT & ADDRESS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = TravelPrimary)
                            Spacer(modifier = Modifier.height(10.dp))

                            val addressText = listOfNotNull(
                                user?.address?.address_line1?.takeIf { it.isNotBlank() },
                                user?.address?.address_line2?.takeIf { it.isNotBlank() }
                            ).joinToString(", ").ifBlank { "—" }

                            val cityStateText = listOfNotNull(
                                user?.address?.city?.takeIf { it.isNotBlank() },
                                user?.address?.state?.takeIf { it.isNotBlank() },
                                user?.address?.postal_code?.takeIf { it.isNotBlank() }
                            ).joinToString(", ").ifBlank { "—" }

                            val contactItems = listOf(
                                Triple(Icons.Filled.Phone, "Mobile Number", user?.phone?.takeIf { it.isNotBlank() } ?: "—"),
                                Triple(Icons.Filled.Email, "Email Address", user?.email?.takeIf { it.isNotBlank() } ?: "—"),
                                Triple(Icons.Filled.Home, "Street Address", addressText),
                                Triple(Icons.Filled.LocationCity, "City, State & Postal Code", cityStateText),
                                Triple(Icons.Filled.Public, "Country", user?.address?.country?.takeIf { it.isNotBlank() } ?: "India")
                            )

                            contactItems.forEachIndexed { idx, (icon, label, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (idx < contactItems.size - 1) HorizontalDivider(thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // EXISTING MENU ITEMS
            // ==========================================
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                val themeLabel = when (currentThemeMode) {
                    AppThemeMode.LIGHT -> "Light Mode"
                    AppThemeMode.DARK -> "Dark Mode"
                    AppThemeMode.SYSTEM -> "System Default"
                }
                ProfileMenuItem(
                    icon = Icons.Filled.DarkMode,
                    title = "App Theme ($themeLabel)",
                    onClick = { showThemeDialog = true }
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Accessible,
                    title = "Accessibility & Comfort Preferences",
                    onClick = { showAccessibilityDialog = true }
                )
                ProfileMenuItem(
                    icon = Icons.Filled.FamilyRestroom,
                    title = "Trip Guardian & Family Security",
                    onClick = onGuardianClick
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Shield,
                    title = "Tourist Safety Intelligence",
                    onClick = onSafetyClick
                )
                ProfileMenuItem(
                    icon = Icons.Filled.GppMaybe,
                    title = "Tourist Scam Shield",
                    onClick = onScamShieldClick
                )
                ProfileMenuItem(
                    icon = Icons.Filled.ConfirmationNumber,
                    title = "My Bookings & Passes",
                    onClick = onBookingsClick
                )
                ProfileMenuItem(
                    icon = Icons.Filled.Lock,
                    title = "Document Vault & Tools",
                    onClick = onToolsClick
                )
                ProfileMenuItem(
                    icon = Icons.Filled.SupportAgent,
                    title = "24/7 Support & FAQ",
                    onClick = onSupportClick
                )
                HorizontalDivider()
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    title = "Logout",
                    onClick = {
                        viewModel.logout()
                        onLogoutClick()
                    },
                    isDestructive = true
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun ProfileMenuItem(icon: ImageVector, title: String, onClick: () -> Unit = {}, isDestructive: Boolean = false) {
    val color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = color, modifier = Modifier.weight(1f))
        if (!isDestructive) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
        }
    }
}
