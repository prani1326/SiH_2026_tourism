package com.touristapp.data.safety

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.provider.Settings
import com.touristapp.data.location.LocationService
import com.touristapp.data.models.DeviceLocation
import com.touristapp.data.models.DeviceStatus
import com.touristapp.data.remote.model.DeviceHeartbeatRequestDto
import com.touristapp.data.remote.model.DeviceLocationDto
import kotlinx.coroutines.*
import java.util.UUID

class DeviceHeartbeatManager(
    private val context: Context,
    private val locationService: LocationService?
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private val deviceId: String by lazy {
        try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: UUID.randomUUID().toString()
        } catch (e: Exception) {
            UUID.randomUUID().toString()
        }
    }

    fun getBatteryLevel(): Int {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) (level * 100 / scale) else 100
        } catch (e: Exception) {
            100
        }
    }

    fun isDeviceCharging(): Boolean {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val intent = context.registerReceiver(null, filter)
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } catch (e: Exception) {
            false
        }
    }

    fun getNetworkStatus(): String {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return "OFFLINE"
            val network = cm.activeNetwork ?: return "OFFLINE"
            val caps = cm.getNetworkCapabilities(network) ?: return "OFFLINE"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> "ONLINE"
                else -> "OFFLINE"
            }
        } catch (e: Exception) {
            "OFFLINE"
        }
    }

    suspend fun getDeviceLocation(): DeviceLocation? {
        return try {
            val loc = locationService?.getCurrentLocation()
            if (loc != null) {
                DeviceLocation(
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    accuracy = loc.accuracy,
                    address = "${loc.latitude}, ${loc.longitude}",
                    timestamp = System.currentTimeMillis(),
                    isLive = true
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun buildDeviceStatus(userId: String, deviceState: String = "ACTIVE"): DeviceStatus {
        val location = getDeviceLocation()
        return DeviceStatus(
            userId = userId,
            deviceId = deviceId,
            batteryLevel = getBatteryLevel(),
            isCharging = isDeviceCharging(),
            networkStatus = getNetworkStatus(),
            lastSeenAt = System.currentTimeMillis(),
            lastAppOpenedAt = System.currentTimeMillis(),
            lastLocationAt = location?.timestamp ?: System.currentTimeMillis(),
            lastKnownLocation = location,
            deviceState = deviceState,
            updatedAt = System.currentTimeMillis()
        )
    }

    suspend fun buildHeartbeatRequest(appActivity: String = "FOREGROUND"): DeviceHeartbeatRequestDto {
        val loc = getDeviceLocation()
        val locDto = loc?.let {
            DeviceLocationDto(
                latitude = it.latitude,
                longitude = it.longitude,
                accuracy = it.accuracy,
                address = it.address,
                timestamp = it.timestamp,
                isLive = it.isLive
            )
        }
        return DeviceHeartbeatRequestDto(
            deviceId = deviceId,
            batteryLevel = getBatteryLevel(),
            isCharging = isDeviceCharging(),
            networkStatus = getNetworkStatus(),
            location = locDto,
            appActivity = appActivity,
            timestamp = System.currentTimeMillis()
        )
    }

    fun startPeriodicHeartbeat(intervalMinutes: Int = 15, onHeartbeat: suspend () -> Unit) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                try {
                    onHeartbeat()
                } catch (e: Exception) {
                    // Safe retry on next tick
                }
                delay(intervalMinutes * 60 * 1000L)
            }
        }
    }

    fun stopPeriodicHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
    }
}
