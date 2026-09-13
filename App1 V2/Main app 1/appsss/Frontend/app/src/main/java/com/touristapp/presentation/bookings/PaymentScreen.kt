package com.touristapp.presentation.bookings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.data.models.TripModel
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    tripId: String,
    totalAmount: Double,
    onBackClick: () -> Unit,
    onPaymentSuccess: (String) -> Unit
) {
    var trip by remember { mutableStateOf<TripModel?>(null) }
    var selectedMethod by remember { mutableStateOf("UPI") }
    var upiId by remember { mutableStateOf("traveler@okhdfcbank") }
    var cardNumber by remember { mutableStateOf("4532 •••• •••• 8892") }
    var cardExpiry by remember { mutableStateOf("12/28") }
    var cardCvv by remember { mutableStateOf("•••") }
    var selectedBank by remember { mutableStateOf("HDFC Bank") }

    var isProcessing by remember { mutableStateOf(false) }
    var processingStage by remember { mutableStateOf("Securing connection...") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(tripId) {
        trip = ServiceLocator.tripRepository.getTripById(tripId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Payment Checkout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "256-bit Encrypted Secure Gateway",
                            style = MaterialTheme.typography.bodySmall,
                            color = TravelSuccess,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, enabled = !isProcessing) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                // 1. Amount Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = TravelNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Amount Payable", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = TravelPrimary
                                ) {
                                    Text("INR", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "₹${totalAmount.toInt()}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                trip?.title ?: "Trip Itinerary Package",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                // 2. Select Payment Method
                item {
                    Text(
                        "Select Payment Method",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                // UPI Option
                item {
                    PaymentMethodCard(
                        title = "UPI / QR / Apps",
                        subtitle = "Google Pay, PhonePe, Paytm, BHIM",
                        icon = Icons.Filled.QrCodeScanner,
                        isSelected = selectedMethod == "UPI",
                        onClick = { selectedMethod = "UPI" }
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            OutlinedTextField(
                                value = upiId,
                                onValueChange = { upiId = it },
                                label = { Text("Enter UPI ID (e.g. name@okhdfcbank)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                // Credit / Debit Card Option
                item {
                    PaymentMethodCard(
                        title = "Credit / Debit Card",
                        subtitle = "Visa, MasterCard, RuPay, Diners",
                        icon = Icons.Filled.CreditCard,
                        isSelected = selectedMethod == "CARD",
                        onClick = { selectedMethod = "CARD" }
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cardNumber,
                                onValueChange = { cardNumber = it },
                                label = { Text("Card Number") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = cardExpiry,
                                    onValueChange = { cardExpiry = it },
                                    label = { Text("MM/YY") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = cardCvv,
                                    onValueChange = { cardCvv = it },
                                    label = { Text("CVV") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }

                // Net Banking Option
                item {
                    PaymentMethodCard(
                        title = "Net Banking",
                        subtitle = "HDFC, SBI, ICICI, Axis & 50+ Banks",
                        icon = Icons.Filled.AccountBalance,
                        isSelected = selectedMethod == "NETBANKING",
                        onClick = { selectedMethod = "NETBANKING" }
                    ) {
                        val banks = listOf("HDFC Bank", "State Bank of India", "ICICI Bank", "Axis Bank", "Kotak Mahindra")
                        Column(modifier = Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            banks.forEach { bank ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedBank = bank }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedBank == bank,
                                        onClick = { selectedBank = bank },
                                        colors = RadioButtonDefaults.colors(selectedColor = TravelPrimary)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(bank, fontWeight = if (selectedBank == bank) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Razorpay Sandbox Direct
                item {
                    PaymentMethodCard(
                        title = "Razorpay Secure Gateway",
                        subtitle = "Instant Sandbox / Live Verification",
                        icon = Icons.Filled.Lock,
                        isSelected = selectedMethod == "RAZORPAY",
                        onClick = { selectedMethod = "RAZORPAY" }
                    )
                }

                // Security note
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = TravelSuccess, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RBI Compliant • PCI-DSS Certified • Instant Confirmation", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }

            // Fixed Bottom Floating Pay Button
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 16.dp
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Button(
                        onClick = {
                            isProcessing = true
                            processingStage = "Creating reservation & booking records..."
                            scope.launch {
                                try {
                                    // 1. Create booking in repository/Firestore
                                    val currentTrip = trip
                                    val targetTitle = currentTrip?.title ?: "Trip Journey"
                                    val destination = currentTrip?.destinationName ?: "Destination"
                                    val startDate = currentTrip?.startDate ?: "2026-09-15"
                                    val endDate = currentTrip?.endDate ?: "2026-09-20"
                                    val guests = currentTrip?.travelersCount ?: 1

                                    val bookingRes = ServiceLocator.bookingRepository.requestBooking(
                                        itemType = "trip_package",
                                        itemId = tripId,
                                        itemTitle = "$targetTitle ($destination)",
                                        checkInDate = startDate,
                                        checkOutDate = endDate,
                                        guestCount = guests,
                                        totalAmount = totalAmount,
                                        specialRequests = "Complete AI Itinerary Package with Cab, Hotel and Sightseeing"
                                    )

                                    if (bookingRes.isSuccess) {
                                        val booking = bookingRes.getOrThrow()
                                        processingStage = "Authorizing payment with $selectedMethod..."
                                        kotlinx.coroutines.delay(800)

                                        processingStage = "Verifying cryptographic payment signature..."
                                        val payRes = ServiceLocator.bookingRepository.processDirectPayment(
                                            bookingId = booking.id,
                                            paymentMethod = selectedMethod.lowercase(),
                                            amount = totalAmount
                                        )

                                        if (payRes.isSuccess) {
                                            processingStage = "Generating digital invoice & QR pass..."
                                            kotlinx.coroutines.delay(600)
                                            isProcessing = false
                                            onPaymentSuccess(booking.id)
                                        } else {
                                            isProcessing = false
                                            snackbarHostState.showSnackbar("Payment verification failed. Please try again.")
                                        }
                                    } else {
                                        isProcessing = false
                                        snackbarHostState.showSnackbar("Failed to create reservation: ${bookingRes.exceptionOrNull()?.message}")
                                    }
                                } catch (e: Exception) {
                                    isProcessing = false
                                    snackbarHostState.showSnackbar("Payment error: ${e.localizedMessage}")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "PAY ₹${totalAmount.toInt()}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Processing Overlay
            AnimatedVisibility(
                visible = isProcessing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.88f)
                            .padding(20.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = TravelPrimary,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "Processing Payment",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = processingStage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TravelPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp),
                                color = TravelPrimary,
                                trackColor = MaterialTheme.colorScheme.brandPillBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) BorderStroke(2.dp, TravelPrimary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        colors = CardDefaults.cardColors(containerColor = if (isSelected) MaterialTheme.colorScheme.brandPillBackground else MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) TravelPrimary else MaterialTheme.colorScheme.brandPillBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else TravelPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
                RadioButton(
                    selected = isSelected,
                    onClick = onClick,
                    colors = RadioButtonDefaults.colors(selectedColor = TravelPrimary)
                )
            }

            if (isSelected && content != null) {
                content()
            }
        }
    }
}
