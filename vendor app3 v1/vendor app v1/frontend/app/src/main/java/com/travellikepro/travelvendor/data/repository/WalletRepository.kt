package com.travellikepro.travelvendor.data.repository

import android.content.Context
import com.travellikepro.travelvendor.data.api.RetrofitClient

class WalletRepository(context: Context) {
    private val api = RetrofitClient.getApi(context)

    suspend fun getWallet() = api.getWallet()

    suspend fun getTransactions(type: String? = null) = api.getTransactions(type = type)
}
