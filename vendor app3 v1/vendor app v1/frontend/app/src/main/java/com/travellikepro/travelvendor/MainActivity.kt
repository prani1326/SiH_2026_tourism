package com.travellikepro.travelvendor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.travellikepro.travelvendor.navigation.AppNavigation
import com.travellikepro.travelvendor.ui.theme.TravelVendorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            TravelVendorTheme {
                AppNavigation()
            }
        }
    }
}