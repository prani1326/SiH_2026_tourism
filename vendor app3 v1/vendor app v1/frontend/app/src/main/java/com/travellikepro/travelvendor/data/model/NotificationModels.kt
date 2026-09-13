package com.travellikepro.travelvendor.data.model

data class Notification(
    val id: Int = 0,
    val recipient_type: String = "vendor",
    val recipient_id: Int = 0,
    val title: String = "",
    val message: String = "",
    val type: String = "info",
    val is_read: Int = 0, // 0 or 1
    val created_at: String = ""
)

data class NotificationsResponseData(
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val total: Int = 0,
    val page: Int = 1,
    val limit: Int = 20
)
