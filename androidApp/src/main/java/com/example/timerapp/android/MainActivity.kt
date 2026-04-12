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

/**
 * TimerScreen is a stateless Composable (though it holds a ViewModel reference).
 * It represents the entire visual state of the timer application.
 */
@Composable
fun TimerScreen(timerViewModel: TimerViewModel = viewModel()) {

    // Convert the StateFlow from ViewModel into a Compose State object.
    // Whenever the Shared Module updates the time, this triggers a recomposition.
    val state by timerViewModel.timerState.collectAsState()

    // Vertical layout container centered on the screen
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Horizontal layout for time display (Minutes:Seconds and Milliseconds)
        Row(verticalAlignment = Alignment.Bottom) {
            // Displays the formatted MM:SS string from the Shared Module
            Text(
                text = state.formattedTime,
                fontSize = 64.sp,
                fontFamily = FontFamily.Monospace // Monospace prevents layout jumping
            )
            // Displays the .SS fractional part with a smaller font
            Text(
                text = state.formattedMillis,
                fontSize = 32.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // Space between the clock and control buttons
        Spacer(modifier = Modifier.height(32.dp))

        // Row containing Start/Pause and Reset buttons
        Row {
            // Context-aware button: Toggles between Start and Pause logic
            Button(
                onClick = {
                    if (state.isRunning) timerViewModel.pauseTimer()
                    else timerViewModel.startTimer()
                },
                modifier = Modifier.width(120.dp)
            ) {
                Text(text = if (state.isRunning) "Pause" else "Start")
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Reset button: Resets the state via the ViewModel
            Button(
                onClick = { timerViewModel.resetTimer() },
                modifier = Modifier.width(120.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(text = "Reset")
            }
        }
    }
}