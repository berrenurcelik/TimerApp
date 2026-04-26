package com.example.timerapp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier

/**
 * MainActivity is the single entry point of the Android application.
 * It sets up the theme and hands off to [AppNavigation] for screen routing.
 *
 * KMP NOTE: Activity is an Android-only concept. The actual feature logic
 * lives in the shared module; this class is just the "launcher" that
 * connects Android's OS lifecycle to Compose.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

/**
 * AppNavigation renders a bottom tab bar with two tabs:
 *   • Stopwatch (existing TimerScreen)
 *   • Focus     (new FocusScreen — the galaxy pomodoro)
 *
 * Uses Scaffold + NavigationBar (Material 3) — no NavController needed for
 * two simple tabs. Each tab composable keeps its own ViewModel instance
 * alive while the tab is selected; the ViewModel is re-created (reset) if
 * the tab is removed from the back stack.
 */
@Composable
fun AppNavigation() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick  = { selectedTab = 0 },
                    icon     = { Icon(Icons.Default.Timer, contentDescription = "Stopwatch") },
                    label    = { Text("Stopwatch") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick  = { selectedTab = 1 },
                    icon     = { Icon(Icons.Default.Stars, contentDescription = "Focus") },
                    label    = { Text("Focus") }
                )
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> TimerScreen()
                1 -> FocusScreen()
            }
        }
    }
}
