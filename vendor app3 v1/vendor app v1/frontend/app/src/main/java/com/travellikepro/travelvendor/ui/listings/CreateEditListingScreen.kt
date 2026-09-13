package com.travellikepro.travelvendor.ui.listings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travellikepro.travelvendor.data.model.CreateListingRequest
import com.travellikepro.travelvendor.ui.components.AppPrimaryButton
import com.travellikepro.travelvendor.ui.components.BrandedTopAppBar

@Composable
fun CreateEditListingScreen(
    listingId: Int? = null,
    onNavigateBack: () -> Unit,
    viewModel: ListingsViewModel = viewModel()
) {
    val existingListing by viewModel.detailState.collectAsState()
    val isOperating by viewModel.isOperating.collectAsState()

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    var maxTravellersStr by remember { mutableStateOf("10") }
    var status by remember { mutableStateOf("active") }

    LaunchedEffect(listingId) {
        if (listingId != null) {
            viewModel.loadListingDetail(listingId)
        }
    }

    LaunchedEffect(existingListing) {
        if (listingId != null && existingListing != null) {
            val item = existingListing!!
            title = item.title
            description = item.description ?: ""
            destination = item.destination
            priceStr = item.price.toString()
            duration = item.duration
            maxTravellersStr = item.max_travellers.toString()
            status = item.status
        }
    }

    Scaffold(
        topBar = {
            BrandedTopAppBar(
                title = if (listingId != null) "Edit Listing" else "Create New Listing",
                canNavigateBack = true,
                onNavigateBack = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Tour Package Information",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Tour Title *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = destination,
                        onValueChange = { destination = it },
                        label = { Text("Destination Location *") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = priceStr,
                            onValueChange = { priceStr = it },
                            label = { Text("Price (₹) *") },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = duration,
                            onValueChange = { duration = it },
                            label = { Text("Duration (e.g. 2 Days) *") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = maxTravellersStr,
                        onValueChange = { maxTravellersStr = it },
                        label = { Text("Max Guests") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Tour Description") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = status == "active",
                            onClick = { status = "active" },
                            label = { Text("Active") }
                        )
                        FilterChip(
                            selected = status == "inactive",
                            onClick = { status = "inactive" },
                            label = { Text("Inactive") }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    AppPrimaryButton(
                        onClick = {
                            val req = CreateListingRequest(
                                title = title,
                                description = description,
                                destination = destination,
                                price = priceStr.toDoubleOrNull() ?: 0.0,
                                duration = duration,
                                max_travellers = maxTravellersStr.toIntOrNull() ?: 10,
                                status = status
                            )
                            if (listingId != null) {
                                viewModel.updateListing(listingId, req) { onNavigateBack() }
                            } else {
                                viewModel.createListing(req) { onNavigateBack() }
                            }
                        },
                        enabled = !isOperating && title.isNotBlank() && destination.isNotBlank() && priceStr.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isOperating) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text(if (listingId != null) "UPDATE LISTING" else "SAVE & PUBLISH")
                        }
                    }
                }
            }
        }
    }
}
