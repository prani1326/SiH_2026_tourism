package com.travellikepro.opsleader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.travellikepro.opsleader.navigation.AppNavigation
import com.travellikepro.opsleader.ui.theme.OpsLeaderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            OpsLeaderTheme {
                AppNavigation()
            }
        }
    }
}