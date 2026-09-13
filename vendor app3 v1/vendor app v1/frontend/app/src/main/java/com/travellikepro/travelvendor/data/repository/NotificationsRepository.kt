package com.travellikepro.travelvendor.data.repository

import android.content.Context
import com.travellikepro.travelvendor.data.api.RetrofitClient

class NotificationsRepository(context: Context) {
    private val api = RetrofitClient.getApi(context)

    suspend fun getNotifications(unreadOnly: Boolean = false) = api.getNotifications(unreadOnly = unreadOnly)

    suspend fun markAsRead(id: Int) = api.markNotificationAsRead(id)

    suspend fun markAllAsRead() = api.markAllNotificationsAsRead()
}
