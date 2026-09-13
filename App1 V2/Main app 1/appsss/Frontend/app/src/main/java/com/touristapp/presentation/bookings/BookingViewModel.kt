package com.touristapp.presentation.bookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.touristapp.data.models.BookingModel
import com.touristapp.data.remote.ApiResult
import com.touristapp.data.remote.model.RazorpayOrderOutDto
import com.touristapp.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BookingViewModel : ViewModel() {

    private val bookingRepository = ServiceLocator.bookingRepository

    private val _bookings = MutableStateFlow<ApiResult<List<BookingModel>>>(ApiResult.Loading)
    val bookings: StateFlow<ApiResult<List<BookingModel>>> = _bookings

    private val _paymentOrder = MutableStateFlow<RazorpayOrderOutDto?>(null)
    val paymentOrder: StateFlow<RazorpayOrderOutDto?> = _paymentOrder

    private val _paymentStatusMessage = MutableStateFlow<String?>(null)
    val paymentStatusMessage: StateFlow<String?> = _paymentStatusMessage

    init {
        loadBookings()
    }

    fun loadBookings() {
        viewModelScope.launch {
            _bookings.value = ApiResult.Loading
            try {
                val list = bookingRepository.getUserBookings()
                _bookings.value = ApiResult.Success(list)
            } catch (e: Exception) {
                _bookings.value = ApiResult.Exception(e)
            }
        }
    }

    fun requestBooking(
        itemType: String,
        itemId: String,
        itemTitle: String,
        checkInDate: String,
        checkOutDate: String,
        guestCount: Int,
        totalAmount: Double,
        specialRequests: String? = null,
        onSuccess: ((BookingModel) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val res = bookingRepository.requestBooking(
                itemType = itemType,
                itemId = itemId,
                itemTitle = itemTitle,
                checkInDate = checkInDate,
                checkOutDate = checkOutDate,
                guestCount = guestCount,
                totalAmount = totalAmount,
                specialRequests = specialRequests
            )
            res.onSuccess { model ->
                _paymentStatusMessage.value = "Reservation request created: ${model.bookingReference}"
                loadBookings()
                onSuccess?.invoke(model)
            }.onFailure { e ->
                _paymentStatusMessage.value = "Booking request failed: ${e.message}"
            }
        }
    }

    fun initiateRazorpayPayment(bookingId: String) {
        viewModelScope.launch {
            val res = bookingRepository.createPaymentOrder(bookingId)
            res.onSuccess { order ->
                _paymentOrder.value = order
                _paymentStatusMessage.value = "Payment Order created. Ready to process."
            }.onFailure { e ->
                _paymentStatusMessage.value = "Failed to create payment order: ${e.message}"
            }
        }
    }

    fun verifyRazorpayPayment(bookingId: String, orderId: String, paymentId: String, signature: String) {
        viewModelScope.launch {
            val res = bookingRepository.verifyPayment(
                bookingId = bookingId,
                orderId = orderId,
                paymentId = paymentId,
                signature = signature
            )
            res.onSuccess {
                _paymentOrder.value = null
                _paymentStatusMessage.value = "Payment Verified & Booking Confirmed! 🎉"
                loadBookings()
            }.onFailure { e ->
                _paymentStatusMessage.value = "Payment verification failed: ${e.message}"
            }
        }
    }

    fun cancelBooking(bookingId: String, reason: String = "User requested cancellation") {
        viewModelScope.launch {
            val res = bookingRepository.cancelBooking(bookingId, reason)
            if (res.isSuccess) {
                _paymentStatusMessage.value = "Booking cancelled. Refund initiated to source account."
            } else {
                _paymentStatusMessage.value = "Failed to cancel booking: ${res.exceptionOrNull()?.message}"
            }
            loadBookings()
        }
    }

    fun clearPaymentMessage() {
        _paymentStatusMessage.value = null
    }
}
