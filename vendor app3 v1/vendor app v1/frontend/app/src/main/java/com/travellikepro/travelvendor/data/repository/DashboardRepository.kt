package com.travellikepro.travelvendor.data.repository

import android.content.Context
import com.travellikepro.travelvendor.data.api.RetrofitClient

class DashboardRepository(context: Context) {
    private val api = RetrofitClient.getApi(context)

    suspend fun getDashboard() = api.getDashboard()
}
