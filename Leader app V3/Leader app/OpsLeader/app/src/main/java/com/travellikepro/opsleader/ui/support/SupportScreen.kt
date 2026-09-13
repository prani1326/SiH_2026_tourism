package com.travellikepro.opsleader.ui.support

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.travellikepro.opsleader.data.api.SupportTicketDto
import com.travellikepro.opsleader.ui.components.StatusBadge
import com.travellikepro.opsleader.ui.components.StatusLevel
import com.travellikepro.opsleader.ui.theme.StatusCritical
import com.travellikepro.opsleader.ui.theme.StatusWarning

@Composable
fun SupportScreen(
    viewModel: SupportViewModel = hiltViewModel()
) {
    val uiData by viewModel.uiData.collectAsState()
    var showBroadcastDialog by remember { mutableStateOf(false) }
    var selectedTicket by remember { mutableStateOf<SupportTicketDto?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Support Desk",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.loadTickets() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
                FilledTonalButton(
                    onClick = { showBroadcastDialog = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Broadcast", maxLines = 1, softWrap = false)
                }
            }
        }

        if (uiData.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiData.tickets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("No active support tickets.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = { viewModel.loadTickets() }) { Text("Refresh Tickets") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiData.tickets) { ticket ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedTicket = ticket },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ticket.id,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                val statusLevel = when ((ticket.status ?: "").uppercase()) {
                                    "RESOLVED" -> StatusLevel.RESOLVED
                                    "IN_PROGRESS" -> StatusLevel.WARNING
                                    else -> StatusLevel.ATTENTION
                                }
                                StatusBadge(label = ticket.status ?: "OPEN", status = statusLevel)
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ticket.subject ?: "Support Request",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Tourist: ${ticket.tourist_name ?: "Guest"} • Category: ${ticket.category ?: "GENERAL"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val priorityColor = when ((ticket.priority ?: "").uppercase()) {
                                    "URGENT" -> StatusCritical
                                    "HIGH" -> StatusWarning
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                AssistChip(
                                    onClick = { },
                                    label = { Text("Priority: ${ticket.priority ?: "MEDIUM"}") },
                                    colors = AssistChipDefaults.assistChipColors(labelColor = priorityColor)
                                )

                                Text("${ticket.messages?.size ?: 0} Messages", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    // Ticket Conversation Dialog
    if (selectedTicket != null) {
        var replyText by remember { mutableStateOf("") }
        val currentTicket = uiData.tickets.find { it.id == selectedTicket!!.id } ?: selectedTicket!!

        AlertDialog(
            onDismissRequest = { selectedTicket = null },
            title = { Text(currentTicket.subject) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Conversation with ${currentTicket.tourist_name ?: "Guest"}", style = MaterialTheme.typography.labelMedium)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentTicket.messages.forEach { msg ->
                            val isMe = msg.sender_role.equals("OPS_LEADER", ignoreCase = true)
                            Surface(
                                color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("${msg.sender_name ?: "User"} (${msg.timestamp ?: "Recent"})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Text(msg.message, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        placeholder = { Text("Type reply to tourist...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (replyText.isNotBlank()) {
                            viewModel.sendReply(currentTicket.id, replyText.trim())
                            replyText = ""
                        }
                    }
                ) { Text("Send Reply") }
            },
            dismissButton = {
                TextButton(onClick = { selectedTicket = null }) { Text("Close") }
            }
        )
    }

    // Broadcast Dialog
    if (showBroadcastDialog) {
        var broadcastTitle by remember { mutableStateOf("") }
        var broadcastBody by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showBroadcastDialog = false },
            title = { Text("Broadcast Push Alert") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("Broadcast Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = broadcastBody,
                        onValueChange = { broadcastBody = it },
                        label = { Text("Message Body") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (broadcastTitle.isNotBlank() && broadcastBody.isNotBlank()) {
                            viewModel.broadcastAlert(broadcastTitle.trim(), broadcastBody.trim())
                            showBroadcastDialog = false
                        }
                    }
                ) { Text("Send Broadcast") }
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastDialog = false }) { Text("Cancel") }
            }
        )
    }
}
