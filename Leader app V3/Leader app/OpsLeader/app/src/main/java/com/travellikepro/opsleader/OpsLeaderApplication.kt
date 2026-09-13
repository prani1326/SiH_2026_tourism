package com.travellikepro.opsleader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class OpsLeaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
