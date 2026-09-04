package com.travellikepro.opsleader.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToSignup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "TRAVELLIKEPRO",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ops Leader",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))

        // Google Sign-In Button
        Button(
            onClick = {
                // In production: launch CredentialManager GetCredentialRequest
                // For now: mock the Google sign-in flow
                viewModel.handleGoogleSignIn(
                    idToken = "mock-google-id-token",
                    isNewUser = false,
                    onSuccess = onLoginSuccess,
                    onNewUserNeedsReferenceId = onNavigateToSignup,
                    onError = { /* Handle error */ }
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Continue with Google")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sign in with your Google account to continue",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = onNavigateToSignup) {
            Text("First time? Set up your account")
        }
    }
}