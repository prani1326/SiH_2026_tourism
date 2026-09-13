package com.travellikepro.travelvendor.ui.kyc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travellikepro.travelvendor.ui.components.ErrorStateView

@Composable
fun KycCheckScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToKyc: () -> Unit,
    onLogout: () -> Unit,
    viewModel: KycCheckViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkKycStatus()
    }

    LaunchedEffect(state) {
        if (state is KycCheckState.Success) {
            val kycStatus = (state as KycCheckState.Success).kycStatus
            if (kycStatus.lowercase() == "approved") {
                onNavigateToHome()
            } else {
                onNavigateToKyc()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        when (state) {
            is KycCheckState.Loading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Checking verification status...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is KycCheckState.Error -> {
                val errorMsg = (state as KycCheckState.Error).message
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    ErrorStateView(
                        errorMessage = errorMsg,
                        onRetry = { viewModel.checkKycStatus() }
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    TextButton(onClick = onLogout) {
                        Text("Return to Login")
                    }
                }
            }
            is KycCheckState.Success -> {
                // UI handles navigation via LaunchedEffect
            }
        }
    }
}
