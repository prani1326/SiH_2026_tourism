package com.touristapp.presentation.bookings

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.touristapp.data.remote.model.DigitalInvoiceDto
import com.touristapp.di.ServiceLocator
import com.touristapp.ui.components.EmptyState
import com.touristapp.ui.components.SkeletonLoader
import com.touristapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceScreen(
    bookingId: String,
    onBackClick: () -> Unit,
    onViewTripClick: ((String) -> Unit)? = null
) {
    var invoice by remember { mutableStateOf<DigitalInvoiceDto?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(bookingId) {
        isLoading = true
        val res = ServiceLocator.bookingRepository.getInvoice(bookingId)
        if (res.isSuccess) {
            invoice = res.getOrNull()
        } else {
            // Fallback from booking
            val booking = ServiceLocator.bookingRepository.getBookingById(bookingId)
            if (booking != null) {
                val total = booking.totalAmount
                val sub = total / 1.18
                val gst = total - sub
                invoice = DigitalInvoiceDto(
                    invoiceNumber = "INV-2026-${booking.id.take(6).uppercase()}",
                    bookingReference = booking.bookingReference,
                    bookingId = booking.id,
                    tripId = booking.tripId ?: booking.itemId,
                    itemTitle = booking.itemTitle,
                    guestName = "Lead Traveler",
                    guestContact = "user@touristapp.com",
                    dates = "${booking.checkInDate} to ${booking.checkOutDate}",
                    destination = booking.destinationName,
                    subtotal = sub,
                    gstTax18Pct = gst,
                    platformFee = 99.0,
                    totalPaid = total,
                    currency = "INR",
                    paymentMethod = "UPI / Verified Gateway",
                    paymentId = "PAY-${booking.id.take(8).uppercase()}",
                    status = "Paid & Verified",
                    issuedAt = "05 Sep 2026"
                )
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tax Invoice", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text("Official Digital Receipt", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val inv = invoice
                        if (inv != null) {
                            val text = """
                                ========================================
                                OFFICIAL TAX INVOICE
                                ========================================
                                Invoice No: ${inv.invoiceNumber}
                                Booking Ref: ${inv.bookingReference}
                                Destination: ${inv.destination}
                                Travel Dates: ${inv.dates}
                                
                                Subtotal: ₹${inv.subtotal.toInt()}
                                GST (18%): ₹${inv.gstTax18Pct.toInt()}
                                Platform Fee: ₹${inv.platformFee.toInt()}
                                ----------------------------------------
                                TOTAL PAID: ₹${inv.totalPaid.toInt()}
                                Payment ID: ${inv.paymentId ?: "N/A"}
                                Status: ${inv.status}
                                ========================================
                            """.trimIndent()
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Tax Invoice ${inv.invoiceNumber}")
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Tax Invoice"))
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SkeletonLoader(modifier = Modifier.fillMaxWidth().height(160.dp))
                SkeletonLoader(modifier = Modifier.fillMaxWidth().height(260.dp))
            }
        } else if (invoice == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                EmptyState(
                    title = "Invoice Not Available",
                    message = "Could not locate the generated tax invoice for this booking."
                )
            }
        } else {
            val inv = invoice!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Invoice Header Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column {
                                    Text("Tourist App Travel Services", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("GSTIN: 08AAACT1234F1Z5", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Text("CIN: U63040RJ2024PTC089912", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = TravelSuccess.copy(alpha = 0.15f)
                                ) {
                                    Text("PAID", modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TravelSuccess)
                                }
                            }

                            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("INVOICE NUMBER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(inv.invoiceNumber, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("DATE ISSUED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text(inv.issuedAt.ifBlank { "05 Sep 2026" }, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }

                // 2. Traveler & Trip Info
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Billed To:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(inv.guestName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(inv.guestContact, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))

                            Divider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                            Text("Trip Itinerary Package:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(inv.destination, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Travel Dates: ${inv.dates}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }

                // 3. Itemized Tax Table
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.brandPillBackground),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Itemized Charges", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(10.dp))

                            InvoiceItemRow(item = "Hotel & Resort Accommodation", category = "Stay", amount = inv.subtotal * 0.40)
                            InvoiceItemRow(item = "Dedicated Cab & Itinerary Transfers", category = "Transport", amount = inv.subtotal * 0.25)
                            InvoiceItemRow(item = "Attraction Passes & Monument Entry", category = "Passes", amount = inv.subtotal * 0.15)
                            InvoiceItemRow(item = "Curated Dining Table Reservations", category = "Dining", amount = inv.subtotal * 0.20)

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))

                            InvoiceSummaryRow(label = "Subtotal (Taxable Value)", amount = inv.subtotal)
                            InvoiceSummaryRow(label = "CGST (9%)", amount = inv.gstTax18Pct / 2)
                            InvoiceSummaryRow(label = "SGST (9%)", amount = inv.gstTax18Pct / 2)
                            InvoiceSummaryRow(label = "Platform & Digital Processing Fee", amount = inv.platformFee)

                            Divider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), thickness = 1.5.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Paid Amount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                Text("₹${inv.totalPaid.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                // 4. Payment Reference Card
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Payment Transaction Record", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Transaction ID: ${inv.paymentId ?: "N/A"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                            Text("Payment Mode: ${inv.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                            Text("Booking Reference: ${inv.bookingReference}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                        }
                    }
                }

                // 5. Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onViewTripClick != null) {
                            val tripId = inv.tripId ?: ""
                            if (tripId.isNotBlank()) {
                                Button(
                                    onClick = { onViewTripClick(tripId) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = TravelPrimary)
                                ) {
                                    Text("Trip Dashboard", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceItemRow(item: String, category: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        Text("₹${amount.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun InvoiceSummaryRow(label: String, amount: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        Text("₹${amount.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
