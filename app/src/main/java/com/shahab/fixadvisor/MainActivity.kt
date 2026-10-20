package com.shahab.fixadvisor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shahab.fixadvisor.ui.navigation.AppNavGraph
import com.shahab.fixadvisor.ui.theme.FixAdvisorTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Required for Android 15+ (targetSdk 36): the system draws the app
        // edge-to-edge, and Compose Scaffold applies the safe insets as padding.
        enableEdgeToEdge()
        setContent {
            FixAdvisorTheme {
                AppNavGraph()
            }
        }
    }
}
