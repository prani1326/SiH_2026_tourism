package com.travellikepro.travelvendor.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travellikepro.travelvendor.data.model.DashboardData
import com.travellikepro.travelvendor.ui.components.*
import com.travellikepro.travelvendor.ui.theme.EmeraldSecondary
import com.travellikepro.travelvendor.ui.theme.TerracottaPrimary

@Composable
fun HomeScreen(
    onNavigateToBookings: () -> Unit,
    onNavigateToListings: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCreateListing: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadDashboard()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = uiState,
            label = "homeContentState"
        ) { state ->
            when (state) {
                is HomeUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        LoadingShimmerItem(height = 120.dp)
                        Spacer(modifier = Modifier.height(16.dp))
                        LoadingShimmerList(count = 3)
                    }
                }
                is HomeUiState.Error -> {
                    ErrorStateView(
                        errorMessage = state.message,
                        onRetry = { viewModel.loadDashboard() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is HomeUiState.Success -> {
                    HomeDashboardContent(
                        data = state.data,
                        onNavigateToBookings = onNavigateToBookings,
                        onNavigateToListings = onNavigateToListings,
                        onNavigateToWallet = onNavigateToWallet,
                        onNavigateToNotifications = onNavigateToNotifications,
                        onNavigateToCreateListing = onNavigateToCreateListing
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeDashboardContent(
    data: DashboardData,
    onNavigateToBookings: () -> Unit,
    onNavigateToListings: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToCreateListing: () -> Unit
) {
    val vendorName = data.vendor?.name ?: "Vendor"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Header Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Welcome back 👋",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Text(
                        text = vendorName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(EmeraldSecondary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = EmeraldSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Verified Vendor",
                            color = EmeraldSecondary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = onNavigateToNotifications,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Row
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                title = "New Listing",
                icon = Icons.Default.AddCircleOutline,
                onClick = onNavigateToCreateListing,
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                title = "Bookings",
                icon = Icons.Outlined.ConfirmationNumber,
                onClick = onNavigateToBookings,
                modifier = Modifier.weight(1f)
            )

            QuickActionButton(
                title = "Wallet",
                icon = Icons.Outlined.AccountBalanceWallet,
                onClick = onNavigateToWallet,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Active Trip Card if exists
        data.active_trip?.let { active ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DirectionsRun,
                                contentDescription = null,
                                tint = EmeraldSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TRIP IN PROGRESS",
                                fontWeight = FontWeight.Bold,
                                color = EmeraldSecondary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        StatusPill(status = "ACTIVE")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = active.listing_title ?: "Tour Trip",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Text(
                        text = "Traveller: ${active.traveller_name ?: "Unknown"} (${active.traveller_count} guests)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Stats Overview Grid
        Text(
            text = "Business Overview",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Total Bookings",
                    value = data.total_bookings.toString(),
                    icon = Icons.Outlined.ConfirmationNumber,
                    accentColor = TerracottaPrimary,
                    onClick = onNavigateToBookings,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Pending Requests",
                    value = data.pending_requests.toString(),
                    icon = Icons.Outlined.PendingActions,
                    accentColor = MaterialTheme.colorScheme.tertiary,
                    onClick = onNavigateToBookings,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Upcoming Trips",
                    value = data.upcoming_trips.toString(),
                    icon = Icons.Outlined.Hiking,
                    accentColor = EmeraldSecondary,
                    onClick = onNavigateToBookings,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Wallet Balance",
                    value = "₹${String.format("%.0f", data.wallet_balance)}",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    accentColor = TerracottaPrimary,
                    onClick = onNavigateToWallet,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}