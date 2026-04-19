package com.example.timerapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * MainActivity is the primary entry point of the Android application.
 * It initializes the UI layer using Jetpack Compose and bridges it
 * with the TimerViewModel to observe data changes.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Apply the app's global theme (defined in MyApplicationTheme.kt)
            MyApplicationTheme {
                // Surface provides the background layer and enforces the theme's color scheme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Start rendering the main timer screen
                    TimerScreen()
                }
            }
        }
    }
}

