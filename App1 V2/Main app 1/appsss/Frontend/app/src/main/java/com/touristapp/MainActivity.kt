package com.touristapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.touristapp.di.ServiceLocator
import com.touristapp.presentation.navigation.AppNavigation
import com.touristapp.ui.theme.TouristAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase service locator and data repositories
        ServiceLocator.initialize(applicationContext)

        lifecycleScope.launch {
            ServiceLocator.sessionManager.loadUserFromStorage()
        }

        enableEdgeToEdge()
        setContent {
            val themeMode by ServiceLocator.sessionManager.themeMode.collectAsState()
            TouristAppTheme(themeMode = themeMode) {
                AppNavigation()
            }
        }
    }
}
