package com.touristapp.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.touristapp.data.repository.SessionRepository

@Composable
fun AuthDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onLoginClick: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Sign up to continue") },
            text = { Text("You're currently browsing as a guest. Please log in or sign up to use this feature.") },
            confirmButton = {
                TextButton(onClick = {
                    onDismiss()
                    onLoginClick()
                }) {
                    Text("Log In / Sign Up")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}
