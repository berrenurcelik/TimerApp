package com.example.timerapp.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*


private val Context.timerDataStore by preferencesDataStore(name = "timer_prefs")
private val KEY_ELAPSED = longPreferencesKey("elapsed_millis")
private val KEY_LAPS = stringPreferencesKey("laps_json")
private val KEY_HISTORY = stringPreferencesKey("history_json")


/**
 * A dedicated Composable file for the Timer UI.
 * This separates the UI logic from the Activity lifecycle.
 */

@Composable
fun TimerScreen(timerViewModel: TimerViewModel = viewModel()) {
    val context = LocalContext.current
    val state by timerViewModel.timerState.collectAsState()
    val history by timerViewModel.history.collectAsState()
    val showHistory = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val prefs = context.timerDataStore.data.first()
        val elapsed = prefs[KEY_ELAPSED] ?: 0L
        val lapsJson = prefs[KEY_LAPS] ?: "[]"
        val lapsArray = JSONArray(lapsJson)
        val laps = buildList {
            for (i in 0 until lapsArray.length()) add(lapsArray.getString(i))
        }
        timerViewModel.restoreState(elapsed, laps)

        val historyJson = prefs[KEY_HISTORY] ?: "[]"
        val historyArray = JSONArray(historyJson)
        val sessions = buildList {
            for (i in 0 until historyArray.length()) {
                val obj = historyArray.getJSONObject(i)
                val lapsArr = obj.getJSONArray("laps")
                val lapsList = buildList {
                    for (j in 0 until lapsArr.length()) add(lapsArr.getString(j))
                }
                add(
                    TimerSession(
                        id = obj.getString("id"),
                        createdAt = obj.getLong("createdAt"),
                        totalElapsedMillis = obj.getLong("totalElapsedMillis"),
                        laps = lapsList
                    )
                )
            }
        }
        timerViewModel.restoreHistory(sessions)
    }

    LaunchedEffect(state.elapsedMillis, state.laps) {
        val lapsJson = JSONArray(state.laps).toString()
        context.timerDataStore.edit { prefs ->
            prefs[KEY_ELAPSED] = state.elapsedMillis
            prefs[KEY_LAPS] = lapsJson
        }
    }

    LaunchedEffect(history) {
        val historyJsonArray = JSONArray()
        history.forEach { session ->
            val obj = JSONObject().apply {
                put("id", session.id)
                put("createdAt", session.createdAt)
                put("totalElapsedMillis", session.totalElapsedMillis)
                put("laps", JSONArray(session.laps))
            }
            historyJsonArray.put(obj)
        }

        context.timerDataStore.edit { prefs ->
            prefs[KEY_HISTORY] = historyJsonArray.toString()
        }
    }

    if (showHistory.value) {
        HistoryDialogScreen(
            sessions = history,
            onDismiss = { showHistory.value = false },
            onDelete = { id -> timerViewModel.deleteSession(id) },
            onClearAll = { timerViewModel.clearHistory() }
        )
    }

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

                // History Button
                IconButton(
                    onClick = { showHistory.value = true },
                    modifier = Modifier
                        .size(60.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "History",
                        tint = MaterialTheme.colorScheme.primary
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

@Composable
fun HistoryDialogScreen(
    sessions: List<TimerSession>,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("History") },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                if (sessions.isEmpty()) {
                    item {
                        Text("No sessions yet", modifier = Modifier.padding(16.dp))
                    }
                } else {
                    itemsIndexed(sessions) { _, session ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = formatDuration(session.totalElapsedMillis),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = formatDate(session.createdAt),
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "Laps: ${session.laps.size}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(session.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            if (sessions.isNotEmpty()) {
                Button(
                    onClick = onClearAll,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = (totalSeconds / 60) % 60
    val seconds = totalSeconds % 60
    val hundredths = (millis % 1000) / 10
    return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
}