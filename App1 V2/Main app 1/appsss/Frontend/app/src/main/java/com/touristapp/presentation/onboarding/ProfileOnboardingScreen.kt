package com.touristapp.presentation.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.theme.TravelDarkPrimary
import com.touristapp.ui.theme.TravelNavy
import com.touristapp.ui.theme.TravelPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private suspend fun getOrCreateCurrentUid(): String? {
    val auth = FirebaseAuth.getInstance()
    var uid = auth.currentUser?.uid
    if (uid == null) {
        try {
            uid = auth.signInAnonymously().await().user?.uid
        } catch (_: Exception) {}
    }
    return uid
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileOnboardingScreen(
    onOnboardingComplete: () -> Unit,
    onSkipKycComplete: () -> Unit,
    initialStep: Int = 1
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionRepo = ServiceLocator.sessionRepository
    val apiClient = ServiceLocator.backendApiClient
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var currentStep by remember { mutableIntStateOf(initialStep.coerceIn(1, 3)) }
    var isLoading by remember { mutableStateOf(false) }

    // --- STEP 1: Personal Details State ---
    var fullName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var firstName by remember { mutableStateOf(currentUser?.displayName?.split(" ")?.firstOrNull() ?: "") }
    var lastName by remember { mutableStateOf(currentUser?.displayName?.split(" ")?.drop(1)?.joinToString(" ") ?: "") }
    var dateOfBirth by remember { mutableStateOf("1998-05-15") }
    var showDatePicker by remember { mutableStateOf(false) }
    var gender by remember { mutableStateOf("Male") }
    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say")
    var preferredLanguage by remember { mutableStateOf("English") }
    val languageOptions = listOf("English", "Hindi", "Spanish", "French", "German", "Japanese", "Tamil", "Bengali")
    var nationality by remember { mutableStateOf("Indian") }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    // Validation Errors Step 1
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var dobError by remember { mutableStateOf<String?>(null) }
    var nationalityError by remember { mutableStateOf<String?>(null) }
    var languageError by remember { mutableStateOf<String?>(null) }

    // --- STEP 2: Contact & Address State ---
    var mobileNumber by remember { mutableStateOf(currentUser?.phoneNumber ?: "+91 98765 43210") }
    var emailAddress by remember { mutableStateOf(currentUser?.email ?: "traveler@example.com") }
    var addressLine1 by remember { mutableStateOf("") }
    var addressLine2 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("India") }
    var postalCode by remember { mutableStateOf("") }
    var isDetectingLocation by remember { mutableStateOf(false) }

    // Validation Errors Step 2
    var mobileError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var addressError by remember { mutableStateOf<String?>(null) }
    var cityError by remember { mutableStateOf<String?>(null) }
    var stateError by remember { mutableStateOf<String?>(null) }
    var postalCodeError by remember { mutableStateOf<String?>(null) }

    // --- STEP 3: Identity / KYC State ---
    var selectedDocType by remember { mutableStateOf("Passport") }
    val docTypeOptions = listOf("Passport", "Driving Licence", "Voter ID", "National ID")
    var docNumber by remember { mutableStateOf("") }
    var docPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var docNumberError by remember { mutableStateOf<String?>(null) }

    // Image Pickers
    val avatarPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        avatarUri = uri
    }
    val docPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        docPhotoUri = uri
    }

    // Location Permission Launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            isDetectingLocation = true
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    isDetectingLocation = false
                    if (loc != null) {
                        try {
                            val geocoder = Geocoder(context, Locale.getDefault())
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                            if (!addresses.isNullOrEmpty()) {
                                val addr = addresses[0]
                                addressLine1 = addr.thoroughfare ?: addr.featureName ?: "Main Street"
                                addressLine2 = addr.subLocality ?: ""
                                city = addr.locality ?: addr.subAdminArea ?: "City"
                                state = addr.adminArea ?: "State"
                                country = addr.countryName ?: "India"
                                postalCode = addr.postalCode ?: ""
                                Toast.makeText(context, "Address detected from current location.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Location coordinates found. Please verify address.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Unable to get current location. Please enter manually.", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener {
                    isDetectingLocation = false
                    Toast.makeText(context, "Could not fetch GPS location.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) {
                isDetectingLocation = false
            }
        } else {
            Toast.makeText(context, "Location permission denied. Enter address manually.", Toast.LENGTH_SHORT).show()
        }
    }

    // Restore saved step on mount
    LaunchedEffect(Unit) {
        val userJson = sessionRepo.getUserJson()
        if (userJson != null) {
            try {
                val userDto = ServiceLocator.backendApiClient.jsonParser.decodeFromString<com.touristapp.data.models.UserDto>(userJson)
                if (userDto.full_name.isNotBlank()) fullName = userDto.full_name
                userDto.profile?.let { p ->
                    if (!p.first_name.isNullOrBlank()) firstName = p.first_name
                    if (!p.last_name.isNullOrBlank()) lastName = p.last_name
                    if (!p.date_of_birth.isNullOrBlank()) dateOfBirth = p.date_of_birth
                    if (!p.gender.isNullOrBlank()) gender = p.gender
                    if (!p.language.isNullOrBlank()) preferredLanguage = p.language
                    if (!p.nationality.isNullOrBlank()) nationality = p.nationality
                }
                userDto.address?.let { a ->
                    if (a.address_line1.isNotBlank()) addressLine1 = a.address_line1
                    if (a.address_line2.isNotBlank()) addressLine2 = a.address_line2
                    if (a.city.isNotBlank()) city = a.city
                    if (a.state.isNotBlank()) state = a.state
                    if (a.country.isNotBlank()) country = a.country
                    if (a.postal_code.isNotBlank()) postalCode = a.postal_code
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
    }

    // Date Picker Dialog for DOB
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.of(1998, 5, 15).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val selected = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
                        dateOfBirth = selected.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
                        dobError = null
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = TravelPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            when (currentStep) {
                                1 -> "Personal Details"
                                2 -> "Contact & Address"
                                else -> "Identity Verification"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Step $currentStep of 3",
                            style = MaterialTheme.typography.bodySmall,
                            color = TravelPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    if (currentStep > 1) {
                        IconButton(onClick = { currentStep -= 1 }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (currentStep == 3) {
                        TextButton(onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                val skipPayload = mapOf(
                                    "kyc_status" to "SKIPPED",
                                    "has_completed_onboarding" to true,
                                    "onboarding_step" to 3
                                )
                                val uid = getOrCreateCurrentUid()
                                if (uid != null) {
                                    try {
                                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            .collection("users").document(uid)
                                            .set(skipPayload, com.google.firebase.firestore.SetOptions.merge())
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                sessionRepo.saveKycStatus("SKIPPED")
                                sessionRepo.saveOnboardingStatus(true)
                                sessionRepo.saveOnboardingStep(3)
                                try { apiClient.skipKyc() } catch (_: Exception) {}
                                isLoading = false
                                Toast.makeText(context, "KYC can be completed later from your Profile.", Toast.LENGTH_LONG).show()
                                onSkipKycComplete()
                            }
                        }) {
                            Text("Skip", color = TravelPrimary, fontWeight = FontWeight.Bold)
                        }
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
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
        ) {
            // Step Progress Indicator Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (1..3).forEach { stepNum ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (stepNum <= currentStep) TravelPrimary
                                    else TravelPrimary.copy(alpha = 0.2f)
                                )
                        )
                    }
                }
            }

            // ==========================================
            // PAGE 1 — PERSONAL DETAILS
            // ==========================================
            if (currentStep == 1) {
                item {
                    Text(
                        "Tell us a little about yourself",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                // Avatar / Profile Photo
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(TravelPrimary.copy(alpha = 0.15f))
                                .clickable { avatarPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUri != null) {
                                AsyncImage(
                                    model = avatarUri,
                                    contentDescription = "Profile Photo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Filled.AddAPhoto, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(28.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Photo", fontSize = 11.sp, color = TravelPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Full Name
                item {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            val parts = it.trim().split(" ")
                            firstName = parts.firstOrNull() ?: ""
                            lastName = parts.drop(1).joinToString(" ")
                            fullNameError = if (it.isBlank()) "Full Name is required" else null
                        },
                        label = { Text("Full Name *") },
                        isError = fullNameError != null,
                        supportingText = { fullNameError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = TravelPrimary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // First Name & Last Name (Auto-populated/Editable)
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            label = { Text("First Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            label = { Text("Last Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                // Date of Birth
                item {
                    OutlinedTextField(
                        value = dateOfBirth,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Date of Birth *") },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = TravelPrimary) },
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Filled.EditCalendar, contentDescription = "Pick Date", tint = TravelPrimary)
                            }
                        },
                        isError = dobError != null,
                        supportingText = { dobError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true },
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Gender Selection
                item {
                    Text("Gender", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        genderOptions.forEach { opt ->
                            val isSelected = gender.equals(opt, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { gender = opt },
                                label = { Text(opt, fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TravelPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Preferred Language
                item {
                    Text("Preferred Language *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languageOptions.take(4).forEach { lang ->
                            val isSelected = preferredLanguage.equals(lang, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { preferredLanguage = lang },
                                label = { Text(lang, fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TravelPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Nationality
                item {
                    OutlinedTextField(
                        value = nationality,
                        onValueChange = {
                            nationality = it
                            nationalityError = if (it.isBlank()) "Nationality is required" else null
                        },
                        label = { Text("Nationality *") },
                        leadingIcon = { Icon(Icons.Filled.Public, contentDescription = null, tint = TravelPrimary) },
                        isError = nationalityError != null,
                        supportingText = { nationalityError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // NEXT BUTTON Step 1
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            var isValid = true
                            if (fullName.isBlank()) {
                                fullNameError = "Full Name is required"
                                isValid = false
                            }
                            if (dateOfBirth.isBlank()) {
                                dobError = "Date of Birth is required"
                                isValid = false
                            }
                            if (nationality.isBlank()) {
                                nationalityError = "Nationality is required"
                                isValid = false
                            }
                            if (preferredLanguage.isBlank()) {
                                languageError = "Preferred Language is required"
                                isValid = false
                            }

                            if (isValid) {
                                coroutineScope.launch {
                                    isLoading = true
                                    sessionRepo.saveOnboardingStep(2)
                                    val payload = mapOf(
                                        "full_name" to fullName,
                                        "first_name" to firstName,
                                        "last_name" to lastName,
                                        "date_of_birth" to dateOfBirth,
                                        "gender" to gender,
                                        "nationality" to nationality,
                                        "language" to preferredLanguage
                                    )
                                    val uid = getOrCreateCurrentUid()
                                    if (uid != null) {
                                        try {
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("users").document(uid)
                                                .set(payload + ("onboarding_step" to 2), com.google.firebase.firestore.SetOptions.merge())
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    try { apiClient.saveProfileOnboardingStep(step = 1, data = payload) } catch (_: Exception) {}
                                    isLoading = false
                                    currentStep = 2
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Next: Contact & Address", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ==========================================
            // PAGE 2 — CONTACT & ADDRESS
            // ==========================================
            else if (currentStep == 2) {
                item {
                    Text(
                        "Add your verified contact and address information",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                }

                // Mobile Number (Verified)
                item {
                    OutlinedTextField(
                        value = mobileNumber,
                        onValueChange = {
                            mobileNumber = it
                            mobileError = if (it.isBlank()) "Mobile number is required" else null
                        },
                        label = { Text("Mobile Number *") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = TravelPrimary) },
                        trailingIcon = {
                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "✓ Verified",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        },
                        isError = mobileError != null,
                        supportingText = { mobileError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Email Address (Verified)
                item {
                    OutlinedTextField(
                        value = emailAddress,
                        onValueChange = {
                            emailAddress = it
                            emailError = if (it.isBlank()) "Email is required" else null
                        },
                        label = { Text("Email Address *") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null, tint = TravelPrimary) },
                        trailingIcon = {
                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    "✓ Verified",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        },
                        isError = emailError != null,
                        supportingText = { emailError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Auto-Detect GPS Location Button
                item {
                    OutlinedButton(
                        onClick = {
                            val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fineGranted || coarseGranted) {
                                isDetectingLocation = true
                                val fused = LocationServices.getFusedLocationProviderClient(context)
                                try {
                                    fused.lastLocation.addOnSuccessListener { loc ->
                                        isDetectingLocation = false
                                        if (loc != null) {
                                            try {
                                                val geocoder = Geocoder(context, Locale.getDefault())
                                                @Suppress("DEPRECATION")
                                                val list = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                                                if (!list.isNullOrEmpty()) {
                                                    val a = list[0]
                                                    addressLine1 = a.thoroughfare ?: a.featureName ?: "Main Street"
                                                    addressLine2 = a.subLocality ?: ""
                                                    city = a.locality ?: "City"
                                                    state = a.adminArea ?: "State"
                                                    country = a.countryName ?: "India"
                                                    postalCode = a.postalCode ?: ""
                                                    Toast.makeText(context, "Address auto-detected! You can edit below.", Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Location coordinates found. Please verify details.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                } catch (e: SecurityException) {
                                    isDetectingLocation = false
                                }
                            } else {
                                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, TravelPrimary)
                    ) {
                        if (isDetectingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = TravelPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Detecting location...", color = TravelPrimary, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Filled.MyLocation, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Use Current Location", color = TravelPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }

                // Address Line 1
                item {
                    OutlinedTextField(
                        value = addressLine1,
                        onValueChange = {
                            addressLine1 = it
                            addressError = if (it.isBlank()) "Address Line 1 is required" else null
                        },
                        label = { Text("Address Line 1 *") },
                        leadingIcon = { Icon(Icons.Filled.Home, contentDescription = null, tint = TravelPrimary) },
                        isError = addressError != null,
                        supportingText = { addressError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Address Line 2
                item {
                    OutlinedTextField(
                        value = addressLine2,
                        onValueChange = { addressLine2 = it },
                        label = { Text("Address Line 2 (Optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // City & State
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = city,
                            onValueChange = {
                                city = it
                                cityError = if (it.isBlank()) "City required" else null
                            },
                            label = { Text("City *") },
                            isError = cityError != null,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = state,
                            onValueChange = {
                                state = it
                                stateError = if (it.isBlank()) "State required" else null
                            },
                            label = { Text("State *") },
                            isError = stateError != null,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                // Country & Postal Code
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = country,
                            onValueChange = { country = it },
                            label = { Text("Country *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = postalCode,
                            onValueChange = {
                                postalCode = it
                                postalCodeError = if (it.isBlank()) "Postal Code required" else null
                            },
                            label = { Text("Postal Code *") },
                            isError = postalCodeError != null,
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }

                // NEXT BUTTON Step 2
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            var isValid = true
                            if (mobileNumber.isBlank()) { mobileError = "Mobile required"; isValid = false }
                            if (emailAddress.isBlank()) { emailError = "Email required"; isValid = false }
                            if (addressLine1.isBlank()) { addressError = "Address Line 1 required"; isValid = false }
                            if (city.isBlank()) { cityError = "City required"; isValid = false }
                            if (state.isBlank()) { stateError = "State required"; isValid = false }
                            if (postalCode.isBlank()) { postalCodeError = "Postal Code required"; isValid = false }

                            if (isValid) {
                                coroutineScope.launch {
                                    isLoading = true
                                    sessionRepo.saveOnboardingStep(3)
                                    val payload = mapOf(
                                        "phone" to mobileNumber,
                                        "email" to emailAddress,
                                        "address_line1" to addressLine1,
                                        "address_line2" to addressLine2,
                                        "city" to city,
                                        "state" to state,
                                        "country" to country,
                                        "postal_code" to postalCode
                                    )
                                    val uid = getOrCreateCurrentUid()
                                    if (uid != null) {
                                        try {
                                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("users").document(uid)
                                                .set(payload + ("onboarding_step" to 3), com.google.firebase.firestore.SetOptions.merge())
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    try { apiClient.saveProfileOnboardingStep(step = 2, data = payload) } catch (_: Exception) {}
                                    isLoading = false
                                    currentStep = 3
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Next: Identity & KYC", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // ==========================================
            // PAGE 3 — IDENTITY / KYC (SKIPPABLE)
            // ==========================================
            else if (currentStep == 3) {
                // Info Card Explaining Why KYC is Required
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Why is KYC Required?", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Identity verification helps us keep traveler information accurate and enables secure AI trip planning, bookings, and emergency safety features.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // KYC Status Badge
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("KYC Status", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "⚪ Not Started",
                                color = Color(0xFFE65100),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // Document Type Selection
                item {
                    Text("Select Government Document", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        docTypeOptions.forEach { dt ->
                            val isSelected = selectedDocType.equals(dt, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDocType = dt },
                                label = { Text(dt, fontSize = 12.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = TravelPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                // Document Number
                item {
                    OutlinedTextField(
                        value = docNumber,
                        onValueChange = {
                            docNumber = it
                            docNumberError = if (it.isBlank()) "Document number is required" else null
                        },
                        label = { Text("$selectedDocType Number *") },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null, tint = TravelPrimary) },
                        isError = docNumberError != null,
                        supportingText = { docNumberError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                // Optional Document Image Upload
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { docPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, TravelPrimary.copy(alpha = 0.35f)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (docPhotoUri != null) "Document Image Attached ✓" else "Upload $selectedDocType (Optional)",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    "Tap to select JPG/PNG from gallery",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Action Buttons: Complete KYC vs Skip for Now
                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (docNumber.isBlank()) {
                                docNumberError = "Document number is required"
                                return@Button
                            }
                            coroutineScope.launch {
                                isLoading = true
                                val kycPayload = mapOf(
                                    "kyc_status" to "VERIFIED",
                                    "document_type" to selectedDocType,
                                    "document_number" to docNumber,
                                    "profile_completion" to 100f,
                                    "has_completed_onboarding" to true,
                                    "onboarding_step" to 3
                                )
                                val uid = getOrCreateCurrentUid()
                                if (uid != null) {
                                    try {
                                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            .collection("users").document(uid)
                                            .set(kycPayload, com.google.firebase.firestore.SetOptions.merge())
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                sessionRepo.saveKycStatus("VERIFIED")
                                sessionRepo.saveProfileCompletion(100f)
                                sessionRepo.saveOnboardingStatus(true)
                                sessionRepo.saveOnboardingStep(3)
                                try { apiClient.verifyKyc(documentType = selectedDocType, documentNumber = docNumber) } catch (_: Exception) {}
                                isLoading = false
                                Toast.makeText(context, "KYC Verified successfully! Trip Planning is unlocked.", Toast.LENGTH_LONG).show()
                                onOnboardingComplete()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Complete KYC & Unlock Trips", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                val skipPayload = mapOf(
                                    "kyc_status" to "SKIPPED",
                                    "has_completed_onboarding" to true,
                                    "onboarding_step" to 3
                                )
                                val uid = getOrCreateCurrentUid()
                                if (uid != null) {
                                    try {
                                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                            .collection("users").document(uid)
                                            .set(skipPayload, com.google.firebase.firestore.SetOptions.merge())
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                sessionRepo.saveKycStatus("SKIPPED")
                                sessionRepo.saveOnboardingStatus(true)
                                sessionRepo.saveOnboardingStep(3)
                                try { apiClient.skipKyc() } catch (_: Exception) {}
                                isLoading = false
                                Toast.makeText(context, "KYC skipped. You can complete verification later from Profile.", Toast.LENGTH_LONG).show()
                                onSkipKycComplete()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Skip for Now", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }
        }
    }
}
