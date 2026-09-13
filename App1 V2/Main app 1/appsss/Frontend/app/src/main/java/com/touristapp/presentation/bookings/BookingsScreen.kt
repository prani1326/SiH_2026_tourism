package com.touristapp.presentation.bookings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.touristapp.data.models.BookingModel
import com.touristapp.data.remote.ApiResult
import com.touristapp.ui.components.EmptyState
import com.touristapp.ui.components.ErrorState
import com.touristapp.ui.components.SkeletonLoader
import com.touristapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingsScreen(
    onBackClick: () -> Unit,
    viewModel: BookingViewModel = viewModel()
) {
    val bookingsResult by viewModel.bookings.collectAsState()
    val paymentOrder by viewModel.paymentOrder.collectAsState()
    val paymentMessage by viewModel.paymentStatusMessage.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") }
    val filterOptions = listOf("All", "Paid", "Confirmed", "Pending", "Cancelled")

    var activePaymentBookingId by remember { mutableStateOf<String?>(null) }
    var selectedPassBooking by remember { mutableStateOf<BookingModel?>(null) }
    var cancelBookingTarget by remember { mutableStateOf<BookingModel?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(paymentMessage) {
        paymentMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPaymentMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("My Bookings & Reservations", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filterOptions.forEach { opt ->
                    val isSelected = selectedFilter == opt
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = opt },
                        label = { Text(opt) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = TravelPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            when (bookingsResult) {
                is ApiResult.Loading -> {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        repeat(3) { SkeletonLoader(modifier = Modifier.fillMaxWidth().height(140.dp)) }
                    }
                }
                is ApiResult.Exception -> {
                    ErrorState(
                        message = (bookingsResult as ApiResult.Exception).e.message ?: "Failed to load bookings",
                        onRetry = { viewModel.loadBookings() }
                    )
                }
                is ApiResult.Error -> {
                    ErrorState(
                        message = (bookingsResult as ApiResult.Error).message,
                        onRetry = { viewModel.loadBookings() }
                    )
                }
                is ApiResult.Success -> {
                    val allList = (bookingsResult as ApiResult.Success<List<BookingModel>>).data
                    val cancelledStatuses = listOf("cancelled", "refund pending", "refund initiated", "refund completed")
                    val filtered = when (selectedFilter) {
                        "Paid" -> allList.filter { it.paymentStatus.equals("paid", ignoreCase = true) }
                        "Confirmed" -> allList.filter { it.status.equals("confirmed", ignoreCase = true) }
                        "Pending" -> allList.filter { it.status.equals("pending", ignoreCase = true) || it.status.equals("requested", ignoreCase = true) }
                        "Cancelled" -> allList.filter { it.status.lowercase() in cancelledStatuses }
                        else -> allList
                    }

                    if (filtered.isEmpty()) {
                        EmptyState(
                            title = "No bookings found",
                            message = "Your hotel, transport, and attraction tickets will appear here."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filtered) { booking ->
                                BookingCard(
                                    booking = booking,
                                    onPayNow = {
                                        activePaymentBookingId = booking.id
                                        viewModel.initiateRazorpayPayment(booking.id)
                                    },
                                    onShowPass = {
                                        selectedPassBooking = booking
                                    },
                                    onCancel = {
                                        cancelBookingTarget = booking
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Cancellation & Refund Confirmation Dialog
    if (cancelBookingTarget != null) {
        val target = cancelBookingTarget!!
        AlertDialog(
            onDismissRequest = { cancelBookingTarget = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(TravelEmergency.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, tint = TravelEmergency)
                }
            },
            title = { Text("Cancel Reservation?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Are you sure you want to cancel booking ${target.bookingReference}?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (target.paymentStatus.equals("paid", ignoreCase = true)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.brandPillBackground),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = TravelPrimary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Eligible for 100% instant refund of ₹${target.totalAmount.toInt()} back to your payment account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelBooking(target.id, "User cancelled via Android app")
                        cancelBookingTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelEmergency)
                ) {
                    Text("Confirm Cancellation", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { cancelBookingTarget = null }) {
                    Text("Keep Booking")
                }
            }
        )
    }

    // Digital E-Pass & Voucher Modal
    if (selectedPassBooking != null) {
        val pass = selectedPassBooking!!
        AlertDialog(
            onDismissRequest = { selectedPassBooking = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, tint = TravelPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Digital Travel Voucher", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.brandPillBackground,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.QrCode2,
                                    contentDescription = "QR Code Pass",
                                    tint = Color.Black,
                                    modifier = Modifier.size(80.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Ref: ${pass.bookingReference}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "SECURE DIGITAL PASS • SCAN AT CHECK-IN",
                                style = MaterialTheme.typography.labelSmall,
                                color = TravelPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Service:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(pass.itemTitle.ifBlank { pass.destinationName }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Date:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(pass.checkInDate, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Guests:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("${pass.guestCount} Guest(s)", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Payment Status:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text("PAID (₹${pass.totalAmount.toInt()})", color = TravelSuccess, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedPassBooking = null },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                ) {
                    Text("Close")
                }
            }
        )
    }

    // Razorpay Payment Simulation & Confirmation Dialog
    if (paymentOrder != null && activePaymentBookingId != null) {
        val order = paymentOrder!!
        val bId = activePaymentBookingId!!

        AlertDialog(
            onDismissRequest = { activePaymentBookingId = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.brandPillBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Payment, contentDescription = null, tint = TravelPrimary)
                }
            },
            title = { Text("Razorpay Secure Checkout", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Order ID: ${order.orderId}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("Total Amount: ₹${order.amount.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TravelDarkPrimary)
                    Text("Merchant Key ID: ${order.keyId}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Click 'Confirm Payment' to complete signature verification with the backend.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dummyPaymentId = "pay_${System.currentTimeMillis()}"
                        val dummySignature = "sig_${System.currentTimeMillis()}"
                        viewModel.verifyRazorpayPayment(
                            bookingId = bId,
                            orderId = order.orderId,
                            paymentId = dummyPaymentId,
                            signature = dummySignature
                        )
                        activePaymentBookingId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                ) {
                    Text("Confirm Payment (₹${order.amount.toInt()})", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activePaymentBookingId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BookingCard(
    booking: BookingModel,
    onPayNow: () -> Unit,
    onShowPass: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            booking.status.equals("confirmed", ignoreCase = true) -> TravelSuccess.copy(alpha = 0.15f)
                            booking.status.lowercase() in listOf("cancelled", "refund pending", "refund initiated", "refund completed") -> TravelEmergency.copy(alpha = 0.15f)
                            else -> TravelWarning.copy(alpha = 0.15f)
                        }
                    ) {
                        Text(
                            text = booking.status.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                booking.status.equals("confirmed", ignoreCase = true) -> TravelSuccess
                                booking.status.lowercase() in listOf("cancelled", "refund pending", "refund initiated", "refund completed") -> TravelEmergency
                                else -> TravelWarning
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    // Payment Badge
                    if (booking.paymentStatus.lowercase() == "paid") {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TravelSuccess.copy(alpha = 0.12f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TravelSuccess, modifier = Modifier.size(11.dp))
                                Text(
                                    text = "PAID",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TravelSuccess,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Ref: ${booking.bookingReference.ifBlank { "BKG-IND" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = booking.itemTitle.ifBlank { booking.destinationName.ifBlank { "Travel Reservation" } },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DateRange, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${booking.checkInDate} • ${booking.guestCount} Guest(s)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Price", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text("₹${booking.totalAmount.toInt()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TravelDarkPrimary)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val isCancelledOrRefund = booking.status.lowercase() in listOf("cancelled", "refund pending", "refund initiated", "refund completed")

                    if (booking.paymentStatus.lowercase() == "paid" && !isCancelledOrRefund) {
                        FilledTonalButton(
                            onClick = onShowPass,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.brandPillBackground)
                        ) {
                            Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, tint = TravelPrimary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("E-Pass", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TravelPrimary)
                        }
                    } else if (!isCancelledOrRefund) {
                        Button(
                            onClick = onPayNow,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                        ) {
                            Text("Pay Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (!isCancelledOrRefund) {
                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Cancel", fontSize = 12.sp, color = TravelEmergency)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TravelEmergency.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = when (booking.status.lowercase()) {
                                    "refund completed" -> "REFUND COMPLETED"
                                    "refund pending" -> "REFUND PENDING"
                                    "refund initiated" -> "REFUND INITIATED"
                                    else -> "REFUND PROCESSED"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = TravelEmergency,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
