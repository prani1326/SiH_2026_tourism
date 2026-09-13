package com.travellikepro.travelvendor.ui.kyc

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travellikepro.travelvendor.data.model.KycResponseData
import com.travellikepro.travelvendor.data.model.KycSubmitRequest
import com.travellikepro.travelvendor.ui.components.*
import com.travellikepro.travelvendor.ui.theme.EmeraldSecondary
import com.travellikepro.travelvendor.ui.theme.StatusRejectedBg
import com.travellikepro.travelvendor.ui.theme.StatusRejectedText
import com.travellikepro.travelvendor.ui.theme.TerracottaPrimary

@Composable
fun KycScreen(
    onKycApprovedContinue: () -> Unit,
    onLogout: () -> Unit,
    viewModel: KycViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            BrandedTopAppBar(
                title = "KYC Verification",
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedContent(
                targetState = uiState,
                label = "kycContentState"
            ) { state ->
                when (state) {
                    is KycUiState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            LoadingShimmerItem(height = 100.dp)
                            Spacer(modifier = Modifier.height(16.dp))
                            LoadingShimmerList(count = 4)
                        }
                    }
                    is KycUiState.Error -> {
                        ErrorStateView(
                            errorMessage = state.message,
                            onRetry = { viewModel.loadKycStatus() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    is KycUiState.Success -> {
                        KycStatusAndFormContent(
                            data = state.data,
                            viewModel = viewModel,
                            onKycApprovedContinue = onKycApprovedContinue
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KycStatusAndFormContent(
    data: KycResponseData,
    viewModel: KycViewModel,
    onKycApprovedContinue: () -> Unit
) {
    val status = data.kyc_status.lowercase()
    val isApproved = status == "approved"
    val isPending = status == "pending"
    val isRejected = status == "rejected"

    LaunchedEffect(isApproved) {
        if (isApproved) {
            onKycApprovedContinue()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val isSubmitting by viewModel.isSubmitting.collectAsState()

    // Form fields state
    val details = data.details
    var fullName by remember(details) { mutableStateOf(details?.full_name ?: "Rahul Sharma") }
    var mobile by remember(details) { mutableStateOf(details?.mobile ?: "9876543210") }
    var email by remember(details) { mutableStateOf(details?.email ?: "rahul@himalayanguides.com") }
    var dob by remember(details) { mutableStateOf(details?.dob ?: "1992-05-14") }

    var businessName by remember(details) { mutableStateOf(details?.business_name ?: "Himalayan Treks & Mountain Guides") }
    var businessType by remember(details) { mutableStateOf(details?.business_type ?: "Tour Guide") }
    var panNumber by remember(details) { mutableStateOf(details?.pan_number ?: "ABCPS1234F") }
    var aadhaarNumber by remember(details) { mutableStateOf(details?.aadhaar_number ?: "987654321098") }
    var gstNumber by remember(details) { mutableStateOf(details?.gst_number ?: "02ABCPS1234F1Z5") }
    var tourismLicenseNo by remember(details) { mutableStateOf(details?.tourism_license_no ?: "HP-TOUR-2024-889") }

    var address by remember(details) { mutableStateOf(details?.address ?: "The Mall Road, Near Circuit House") }
    var city by remember(details) { mutableStateOf(details?.city ?: "Manali") }
    var state by remember(details) { mutableStateOf(details?.state ?: "Himachal Pradesh") }
    var pincode by remember(details) { mutableStateOf(details?.pincode ?: "175131") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Status Banner Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isApproved -> EmeraldSecondary.copy(alpha = 0.12f)
                    isRejected -> StatusRejectedBg
                    else -> MaterialTheme.colorScheme.tertiaryContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "KYC STATUS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    StatusPill(status = data.kyc_status)
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isApproved) {
                    Text(
                        text = "Verification Approved! 🎉",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = data.verification_remarks ?: "Your identity and business documents have been verified.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AppPrimaryButton(
                        onClick = onKycApprovedContinue,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("ENTER VENDOR DASHBOARD")
                    }
                } else if (isPending) {
                    Text(
                        text = "Application Under Review",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Our admin team is currently reviewing your submitted details and document proof.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                    )
                } else if (isRejected) {
                    Text(
                        text = "KYC Application Declined",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = StatusRejectedText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Reason: ${data.rejection_reason ?: "Uploaded documents require updates."}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = StatusRejectedText
                    )
                    if (!data.verification_remarks.isNull_or_blank()) {
                        Text(
                            text = "Remarks: ${data.verification_remarks}",
                            style = MaterialTheme.typography.bodySmall,
                            color = StatusRejectedText.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        if (!isApproved) {
            Spacer(modifier = Modifier.height(20.dp))

            // Section Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Personal") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Business") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Location") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    when (selectedTab) {
                        0 -> {
                            Text(
                                text = "Personal Details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = fullName,
                                onValueChange = { fullName = it },
                                label = { Text("Full Name *") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = mobile,
                                onValueChange = { mobile = it },
                                label = { Text("Mobile Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = dob,
                                onValueChange = { dob = it },
                                label = { Text("Date of Birth (YYYY-MM-DD)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        1 -> {
                            Text(
                                text = "Business Details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = it },
                                label = { Text("Business / Operator Name *") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = businessType,
                                onValueChange = { businessType = it },
                                label = { Text("Business Type (e.g. Tour Guide, Activity Provider) *") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = panNumber,
                                onValueChange = { panNumber = it },
                                label = { Text("PAN Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = aadhaarNumber,
                                onValueChange = { aadhaarNumber = it },
                                label = { Text("Aadhaar Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = tourismLicenseNo,
                                onValueChange = { tourismLicenseNo = it },
                                label = { Text("Tourism License Number") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        2 -> {
                            Text(
                                text = "Location Details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                label = { Text("Street Address") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = { Text("City") },
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = state,
                                    onValueChange = { state = it },
                                    label = { Text("State") },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = pincode,
                                onValueChange = { pincode = it },
                                label = { Text("Pincode") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AppPrimaryButton(
                        onClick = {
                            viewModel.submitKycForm(
                                KycSubmitRequest(
                                    full_name = fullName,
                                    mobile = mobile,
                                    email = email,
                                    dob = dob,
                                    business_name = businessName,
                                    business_type = businessType,
                                    pan_number = panNumber,
                                    aadhaar_number = aadhaarNumber,
                                    gst_number = gstNumber,
                                    tourism_license_no = tourismLicenseNo,
                                    address = address,
                                    city = city,
                                    state = state,
                                    pincode = pincode
                                )
                            )
                        },
                        enabled = !isSubmitting && fullName.isNotBlank() && businessName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("SUBMIT KYC FOR REVIEW")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Document Upload Status Section
            Text(
                text = "Submitted Documents",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (data.documents.isEmpty()) {
                Text(
                    text = "No document copies uploaded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    data.documents.forEach { doc ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.InsertDriveFile,
                                        contentDescription = null,
                                        tint = TerracottaPrimary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = doc.file_name,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = doc.doc_type.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Uploaded",
                                    tint = EmeraldSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun String?.isNull_or_blank(): Boolean = this == null || this.trim().isEmpty()
