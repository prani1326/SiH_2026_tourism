package com.travellikepro.travelvendor.data.repository

import android.content.Context
import com.travellikepro.travelvendor.data.api.RetrofitClient
import com.travellikepro.travelvendor.data.model.RejectBookingRequest

class BookingsRepository(context: Context) {
    private val api = RetrofitClient.getApi(context)

    suspend fun getBookings(status: String? = null) = api.getBookings(status = status)

    suspend fun getBookingById(id: Int) = api.getBookingById(id)

    suspend fun acceptBooking(id: Int) = api.acceptBooking(id)

    suspend fun rejectBooking(id: Int, reason: String? = null) =
        api.rejectBooking(id, RejectBookingRequest(reason = reason))
}
