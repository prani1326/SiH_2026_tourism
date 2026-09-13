package com.touristapp.presentation.profile.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.theme.TravelPrimary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KycVerificationDialog(
    onDismiss: () -> Unit,
    onVerificationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = ServiceLocator.backendApiClient
    val sessionRepo = ServiceLocator.sessionRepository

    var selectedDocType by remember { mutableStateOf("Passport") }
    val docTypes = listOf("Passport", "Driving Licence", "Voter ID", "National ID")
    var documentNumber by remember { mutableStateOf("") }
    var docNumberError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Identity Verification (KYC)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                "Verify your identity to unlock Trip Planning and access verified travel features.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text("Select Document Type", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                docTypes.forEach { type ->
                    val isSelected = selectedDocType.equals(type, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDocType = type },
                        label = { Text(type, fontSize = 12.sp) },
                        shape = RoundedCornerShape(12.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TravelPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = documentNumber,
                onValueChange = {
                    documentNumber = it
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (documentNumber.isBlank()) {
                        docNumberError = "Document number is required"
                        return@Button
                    }
                    coroutineScope.launch {
                        isLoading = true
                        val res = apiClient.verifyKyc(documentType = selectedDocType, documentNumber = documentNumber)
                        isLoading = false
                        if (res.isSuccess) {
                            sessionRepo.saveKycStatus("VERIFIED")
                            sessionRepo.saveProfileCompletion(100f)
                            Toast.makeText(context, "KYC Verified! Trip Planning is now unlocked.", Toast.LENGTH_LONG).show()
                            onVerificationSuccess()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Verification failed. Please check details and try again.", Toast.LENGTH_LONG).show()
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
                    Text("Verify & Unlock Trip Planning", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
