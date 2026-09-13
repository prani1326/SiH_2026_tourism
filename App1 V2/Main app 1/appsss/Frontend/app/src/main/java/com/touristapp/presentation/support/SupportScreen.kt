package com.touristapp.presentation.support

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.touristapp.data.remote.ApiResult
import com.touristapp.data.remote.model.SupportTicketOutDto
import com.touristapp.ui.components.EmptyState
import com.touristapp.ui.components.ErrorState
import com.touristapp.ui.components.SkeletonLoader
import com.touristapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(
    onBackClick: () -> Unit,
    viewModel: SupportViewModel = viewModel()
) {
    val ticketsResult by viewModel.tickets.collectAsState()
    val statusMsg by viewModel.statusMessage.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("24/7 Support Concierge", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = TravelPrimary,
                contentColor = Color.White,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Open Ticket", fontWeight = FontWeight.Bold) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Helplines Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.brandPillBackground)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.SupportAgent, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("24/7 Tourist Assistance", fontWeight = FontWeight.Bold, color = TravelPrimary)
                        Text("Emergency Helpline: 1363 (Toll Free in India)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            when (ticketsResult) {
                is ApiResult.Loading -> {
                    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(120.dp)) }
                    }
                }
                is ApiResult.Exception -> {
                    ErrorState(
                        message = (ticketsResult as ApiResult.Exception).e.message ?: "Failed to load support tickets",
                        onRetry = { viewModel.loadTickets() }
                    )
                }
                is ApiResult.Error -> {
                    ErrorState(
                        message = (ticketsResult as ApiResult.Error).message,
                        onRetry = { viewModel.loadTickets() }
                    )
                }
                is ApiResult.Success -> {
                    val tickets = (ticketsResult as ApiResult.Success<List<SupportTicketOutDto>>).data
                    if (tickets.isEmpty()) {
                        EmptyState(
                            title = "No tickets open",
                            message = "Have an inquiry about your booking or travel safety? Open a ticket."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(tickets) { ticket ->
                                TicketCard(ticket = ticket)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateTicketDialog(
            onDismiss = { showCreateDialog = false },
            onSubmit = { subject, category, message ->
                viewModel.createTicket(subject, category, message)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun TicketCard(ticket: SupportTicketOutDto) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (ticket.status.equals("resolved", ignoreCase = true)) TravelSuccess.copy(alpha = 0.15f) else TravelPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = ticket.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (ticket.status.equals("resolved", ignoreCase = true)) TravelSuccess else TravelPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = ticket.category,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = ticket.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (ticket.messages.isNotEmpty()) {
                val latest = ticket.messages.last()
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${latest.senderName}: ${latest.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun CreateTicketDialog(
    onDismiss: () -> Unit,
    onSubmit: (subject: String, category: String, message: String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General Inquiry") }
    var message by remember { mutableStateOf("") }

    val categories = listOf("General Inquiry", "Booking Issue", "Payment & Refund", "Safety & Emergency", "Feedback")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Support Ticket", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Category", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Explain your issue") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(subject, category, message) },
                enabled = subject.isNotBlank() && message.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
            ) {
                Text("Submit Ticket")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
