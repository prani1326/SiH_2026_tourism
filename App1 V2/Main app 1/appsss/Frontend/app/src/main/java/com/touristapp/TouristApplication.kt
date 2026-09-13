package com.touristapp

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.touristapp.di.ServiceLocator

class TouristApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()

        // 1. Optimize HTTP networking & TCP Keep-Alive
        System.setProperty("http.keepAlive", "true")
        System.setProperty("http.maxConnections", "15")

        // Configure OSMDroid Map SDK
        try {
            org.osmdroid.config.Configuration.getInstance().userAgentValue = packageName
        } catch (_: Exception) {}

        // 2. Initialize Firebase services first
        try {
            FirebaseApp.initializeApp(this)

            // Configure persistent disk caching for Cloud Firestore
            try {
                val firestoreSettings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings
            } catch (e: Exception) {
                Log.w(TAG, "Firestore settings already initialized or customized: ${e.message}")
            }

            try {
                val appCheck = FirebaseAppCheck.getInstance()
                if (BuildConfig.DEBUG) {
                    appCheck.installAppCheckProviderFactory(
                        DebugAppCheckProviderFactory.getInstance()
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "AppCheck initialization non-fatal: ${e.message}")
            }

            try {
                FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
                FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(true)
            } catch (_: Exception) {}

            Log.i(TAG, "TouristApplication successfully initialized Firebase services")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase services in Application class", e)
        }

        // 3. Initialize ServiceLocator so repositories use initialized Firebase instances
        ServiceLocator.initialize(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(60L * 1024 * 1024) // 60 MB disk cache
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    companion object {
        private const val TAG = "TouristApplication"
    }
}

