package com.travellikepro.travelvendor.data.model

data class WalletSummary(
    val vendor_id: Int = 0,
    val balance: Double = 0.0,
    val total_earned: Double = 0.0,
    val pending_amount: Double = 0.0,
    val withdrawn_amount: Double = 0.0,
    val recent_transactions: List<Transaction>? = null
)

data class Transaction(
    val id: Int = 0,
    val vendor_id: Int = 0,
    val type: String = "earning", // earning, payout, refund, adjustment
    val amount: Double = 0.0,
    val description: String? = null,
    val booking_id: Int? = null,
    val created_at: String = ""
)

data class TransactionsResponseData(
    val transactions: List<Transaction> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)
