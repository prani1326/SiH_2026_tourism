package com.touristapp.presentation.trips.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.data.models.TripEligibilityResponse
import com.touristapp.ui.theme.TravelDarkPrimary
import com.touristapp.ui.theme.TravelPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlanningLockBottomSheet(
    eligibility: TripEligibilityResponse?,
    onDismiss: () -> Unit,
    onCompleteKycClick: () -> Unit,
    onCompleteProfileClick: () -> Unit
) {
    val kycStatus = eligibility?.kyc_status ?: "NOT_STARTED"
    val isKycMissing = !eligibility?.kyc_verified.let { it == true }
    val isProfileIncomplete = !eligibility?.profile_complete.let { it == true }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header with Lock Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFEBEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Trip Planning Locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            if (isKycMissing) "Identity Verification Required" else "Complete Profile First",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                "To create a secure and personalized trip, we require your traveler profile and government identity verification.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Checklist Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Requirements Checklist", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                    val hasPersonal = eligibility?.missing_requirements?.contains("PERSONAL_DETAILS") != true
                    val hasContact = eligibility?.missing_requirements?.contains("CONTACT_DETAILS") != true
                    val hasAddress = eligibility?.missing_requirements?.contains("ADDRESS_DETAILS") != true
                    val hasKyc = eligibility?.kyc_verified == true

                    ChecklistItem(title = "Personal Details (Name, DOB, Nationality)", isComplete = hasPersonal)
                    ChecklistItem(title = "Contact Details (Verified Phone & Email)", isComplete = hasContact)
                    ChecklistItem(title = "Address & City Details", isComplete = hasAddress)
                    ChecklistItem(
                        title = when (kycStatus) {
                            "PENDING" -> "Identity Verification (Pending Review)"
                            "FAILED" -> "Identity Verification (Failed — Retry Needed)"
                            "VERIFIED" -> "Identity Verification (✓ Verified)"
                            else -> "Identity Verification (KYC Required)"
                        },
                        isComplete = hasKyc,
                        isWarning = kycStatus == "PENDING"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Primary Action Button
            if (isKycMissing && !isProfileIncomplete) {
                Button(
                    onClick = {
                        onDismiss()
                        onCompleteKycClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                ) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (kycStatus == "FAILED") "Retry KYC Verification"
                        else if (kycStatus == "PENDING") "View KYC Status"
                        else "Complete KYC Now",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Button(
                    onClick = {
                        onDismiss()
                        onCompleteProfileClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                ) {
                    Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete Profile & KYC", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ChecklistItem(title: String, isComplete: Boolean, isWarning: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        val (icon, tint) = when {
            isComplete -> Icons.Filled.CheckCircle to Color(0xFF2E7D32)
            isWarning -> Icons.Filled.Pending to Color(0xFFF57C00)
            else -> Icons.Filled.Cancel to Color(0xFFD32F2F)
        }
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isComplete) FontWeight.Normal else FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
