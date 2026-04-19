package com.example.timerapp.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * A dedicated Composable file for the Timer UI.
 * This separates the UI logic from the Activity lifecycle.
 */

@Composable
fun TimerScreen(timerViewModel: TimerViewModel = viewModel()) {
    val state by timerViewModel.timerState.collectAsState()

    // Main Container with a dark themed background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // Modern Timer Display inside a subtle surface
            Surface(
                tonalElevation = 4.dp,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.padding(bottom = 40.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
                ) {
                    Text(
                        text = state.formattedTime,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = state.formattedMillis,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
            }

            // High-end Control Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Start/Pause Button with dynamic colors
                Button(
                    onClick = { if (state.isRunning) timerViewModel.pauseTimer() else timerViewModel.startTimer() },
                    modifier = Modifier.weight(1.4f).height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isRunning) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(if (state.isRunning) "PAUSE" else "START", fontWeight = FontWeight.Bold)
                }

                // Lap Button
                OutlinedButton(
                    onClick = { timerViewModel.addLap() },
                    enabled = state.elapsedMillis > 0,
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("LAP")
                }

                // Reset Button
                IconButton(
                    onClick = { timerViewModel.resetTimer() },
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Elegant Laps List
            Text(
                text = "LAP TIMES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(state.laps) { index, lapTime ->
                    val lapNumber = state.laps.size - index
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lap $lapNumber", fontWeight = FontWeight.Medium)
                            Text(lapTime, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}