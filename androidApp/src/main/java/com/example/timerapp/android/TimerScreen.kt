package com.example.timerapp.android

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Context
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

// Palette consistent with FocusScreen
private val BgColor    = Color(0xFF050A18)
private val AccentWork = Color(0xFF6200EE)
private val AccentPause = Color(0xFF00BCD4)
private val SurfaceDim = Color(0xFF0D1526)
private val TextDim    = Color(0xFF8891A8)

private val Context.timerDataStore by preferencesDataStore(name = "timer_prefs")
private val KEY_ELAPSED = longPreferencesKey("elapsed_millis")
private val KEY_LAPS    = stringPreferencesKey("laps_json")
private val KEY_HISTORY = stringPreferencesKey("history_json")

@Composable
fun TimerScreen(timerViewModel: TimerViewModel = viewModel()) {
    val context = LocalContext.current
    val state   by timerViewModel.timerState.collectAsState()
    val history by timerViewModel.history.collectAsState()
    val showHistory = remember { mutableStateOf(false) }

    // ── Restore persisted state on first launch ──────────────────────────────
    LaunchedEffect(Unit) {
        val prefs = context.timerDataStore.data.first()
        val lapsArray = JSONArray(prefs[KEY_LAPS] ?: "[]")
        timerViewModel.restoreState(
            prefs[KEY_ELAPSED] ?: 0L,
            buildList { for (i in 0 until lapsArray.length()) add(lapsArray.getString(i)) }
        )
        val histArr = JSONArray(prefs[KEY_HISTORY] ?: "[]")
        timerViewModel.restoreHistory(buildList {
            for (i in 0 until histArr.length()) {
                val o = histArr.getJSONObject(i)
                val ll = o.getJSONArray("laps")
                add(TimerSession(
                    id = o.getString("id"), createdAt = o.getLong("createdAt"),
                    totalElapsedMillis = o.getLong("totalElapsedMillis"),
                    laps = buildList { for (j in 0 until ll.length()) add(ll.getString(j)) }
                ))
            }
        })
    }

    // ── Persist on every change ───────────────────────────────────────────────
    LaunchedEffect(state.elapsedMillis, state.laps) {
        context.timerDataStore.edit { p ->
            p[KEY_ELAPSED] = state.elapsedMillis
            p[KEY_LAPS]    = JSONArray(state.laps).toString()
        }
    }
    LaunchedEffect(history) {
        val arr = JSONArray()
        history.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id); put("createdAt", s.createdAt)
                put("totalElapsedMillis", s.totalElapsedMillis)
                put("laps", JSONArray(s.laps))
            })
        }
        context.timerDataStore.edit { it[KEY_HISTORY] = arr.toString() }
    }

    if (showHistory.value) {
        HistoryDialogScreen(
            sessions  = history,
            onDismiss = { showHistory.value = false },
            onDelete  = { id -> timerViewModel.deleteSession(id) },
            onClearAll = { timerViewModel.clearHistory() }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {

            // ── Timer + decorative ring ───────────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(top = 72.dp)
                    .size(260.dp)
            ) {
                PulsingRingCanvas(isRunning = state.isRunning)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = state.formattedTime,
                            fontSize = 62.sp, fontWeight = FontWeight.Thin,
                            fontFamily = FontFamily.Monospace, color = Color.White
                        )
                        Text(
                            text = state.formattedMillis,
                            fontSize = 24.sp, fontWeight = FontWeight.Light,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                    }
                    if (state.laps.isNotEmpty()) {
                        Text(
                            text = "LAP ${state.laps.size}",
                            fontSize = 11.sp, letterSpacing = 3.sp,
                            color = AccentPause.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Controls ─────────────────────────────────────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            ) {
                Button(
                    onClick = { if (state.isRunning) timerViewModel.pauseTimer() else timerViewModel.startTimer() },
                    modifier = Modifier.weight(1.5f).height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isRunning) AccentPause else AccentWork
                    )
                ) {
                    Text(
                        text = if (state.isRunning) "PAUSE" else "START",
                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                    )
                }
                OutlinedButton(
                    onClick = { timerViewModel.addLap() },
                    enabled = state.elapsedMillis > 0,
                    modifier = Modifier.weight(1f).height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f))
                ) {
                    Text("LAP", color = Color.White.copy(alpha = 0.7f), letterSpacing = 2.sp)
                }
                IconButton(
                    onClick = { timerViewModel.resetTimer() },
                    modifier = Modifier
                        .size(58.dp)
                        .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset",
                        tint = Color.White.copy(alpha = 0.55f))
                }
                IconButton(
                    onClick = { showHistory.value = true },
                    modifier = Modifier
                        .size(58.dp)
                        .background(AccentWork.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.History, contentDescription = "History",
                        tint = AccentWork.copy(alpha = 0.9f))
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Lap list ─────────────────────────────────────────────────────
            if (state.laps.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("LAP",  fontSize = 11.sp, letterSpacing = 3.sp, color = TextDim)
                    Text("TIME", fontSize = 11.sp, letterSpacing = 3.sp, color = TextDim)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(state.laps) { index, lapTime ->
                        val lapNumber = state.laps.size - index
                        val isLatest  = index == 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isLatest) AccentWork.copy(alpha = 0.12f) else SurfaceDim,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Lap $lapNumber",
                                fontWeight = if (isLatest) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isLatest) Color.White else Color.White.copy(alpha = 0.65f),
                                fontSize = 14.sp
                            )
                            Text(
                                lapTime,
                                fontFamily = FontFamily.Monospace,
                                color = if (isLatest) AccentWork else Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Subtle double-ring behind the timer; pulses when running
@Composable
private fun PulsingRingCanvas(isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "ringPulse"
    )
    val alpha = if (isRunning) 0.18f + pulse * 0.14f else 0.07f

    Canvas(modifier = Modifier.fillMaxSize()) {
        val r1 = size.minDimension / 2f - 8f
        val r2 = r1 - 12f
        drawCircle(color = AccentWork, radius = r1, center = center,
            style = Stroke(1.5f), alpha = alpha)
        drawCircle(color = AccentWork, radius = r2, center = center,
            style = Stroke(0.7f), alpha = alpha * 0.45f)
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
        containerColor   = Color(0xFF0D1526),
        title = {
            Text("History", color = Color.White,
                fontWeight = FontWeight.Light, letterSpacing = 2.sp)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (sessions.isEmpty()) {
                    item {
                        Text("No sessions yet", color = TextDim,
                            modifier = Modifier.padding(16.dp))
                    }
                } else {
                    itemsIndexed(sessions) { _, session ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF151F33), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(formatDuration(session.totalElapsedMillis),
                                    color = Color.White, fontWeight = FontWeight.Medium, fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace)
                                Text(formatDate(session.createdAt), color = TextDim, fontSize = 11.sp)
                                if (session.laps.isNotEmpty())
                                    Text("${session.laps.size} laps", color = TextDim, fontSize = 11.sp)
                            }
                            IconButton(
                                onClick = { onDelete(session.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Delete",
                                    tint = Color(0xFFCF6679), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = AccentWork)
            }
        },
        dismissButton = {
            if (sessions.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text("Clear All", color = Color(0xFFCF6679))
                }
            }
        }
    )
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatDuration(millis: Long): String {
    val s = millis / 1000
    return String.format("%02d:%02d.%02d", (s / 60) % 60, s % 60, (millis % 1000) / 10)
}
