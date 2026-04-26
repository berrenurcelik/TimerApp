package com.example.timerapp.android

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timerapp.shared.DailySummary
import java.text.SimpleDateFormat
import java.util.Locale

private val BgColor      = Color(0xFF050A18)
private val CardColor    = Color(0xFF0D1428)
private val AccentPurple = Color(0xFFBB86FC)
private val AccentCyan   = Color(0xFF00BCD4)
private val TextDim      = Color(0xFF8888AA)

// ── Root ──────────────────────────────────────────────────────────────────────

@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val summaries   by viewModel.summaries.collectAsState()
    val allTimeSecs by viewModel.allTimeSecs.collectAsState()

    // Refresh every time the screen enters composition
    LaunchedEffect(Unit) { viewModel.refresh() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        if (summaries.isEmpty()) {
            EmptyState(modifier = Modifier.align(Alignment.Center))
        } else {
            var showConfirm by remember { mutableStateOf(false) }
            if (showConfirm) {
                AlertDialog(
                    onDismissRequest = { showConfirm = false },
                    title = { Text("Clear History", color = Color.White) },
                    text  = { Text("All focus sessions will be deleted.", color = TextDim) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearAll(); showConfirm = false }) {
                            Text("Delete", color = Color(0xFFCF6679))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
                    },
                    containerColor = CardColor
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { AllTimeBanner(allTimeSecs, onClearAll = { showConfirm = true }) }
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    Text(
                        "Day by Day",
                        color = TextDim,
                        fontSize = 12.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Light
                    )
                }
                items(summaries) { summary ->
                    DayCard(summary)
                }
            }
        }
    }
}

// ── All-time banner ───────────────────────────────────────────────────────────

@Composable
private fun AllTimeBanner(allTimeSecs: Long, onClearAll: () -> Unit) {
    val totalHours = allTimeSecs / 3600
    val totalMins  = (allTimeSecs % 3600) / 60
    val totalStars = (allTimeSecs / 300).toInt().coerceAtMost(200)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "YOUR GALAXY",
                color = TextDim,
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.height(12.dp))

            // Mini star field — dots representing all-time stars
            MiniStarField(starCount = totalStars, maxDisplay = 60)

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                StatChip(
                    value = if (totalHours > 0) "${totalHours}h ${totalMins}m" else "${totalMins}m",
                    label = "TOTAL FOCUS"
                )
                StatChip(value = "$totalStars", label = "STARS EARNED")
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onClearAll) {
                Text("Clear All History", color = Color(0xFFCF6679), fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun MiniStarField(starCount: Int, maxDisplay: Int) {
    val displayed = starCount.coerceAtMost(maxDisplay)
    val cols = 12
    val rows  = (maxDisplay + cols - 1) / cols

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height((rows * 14).dp)
    ) {
        // Use a fixed layout — dots in a grid, earned ones lit up
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            var remaining = displayed
            repeat(rows) {
                val count = minOf(cols, remaining + (maxDisplay - displayed))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.wrapContentWidth()
                ) {
                    repeat(cols) { col ->
                        val index = it * cols + col
                        val earned = index < displayed
                        Box(
                            modifier = Modifier
                                .size(if (earned) 6.dp else 4.dp)
                                .background(
                                    color = if (earned) AccentPurple.copy(alpha = 0.85f)
                                            else Color.White.copy(alpha = 0.06f),
                                    shape = CircleShape
                                )
                        )
                    }
                }
                remaining -= cols
            }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = AccentPurple, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextDim, fontSize = 10.sp, letterSpacing = 1.5.sp)
    }
}

// ── Day card ──────────────────────────────────────────────────────────────────

@Composable
private fun DayCard(summary: DailySummary) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = CardColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = formatDate(summary.date),
                    color      = Color.White,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text  = "${summary.totalPauses} pauses · ${summary.sessionCount} session${if (summary.sessionCount > 1) "s" else ""}",
                    color = TextDim,
                    fontSize = 12.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text       = summary.formattedDuration,
                    color      = AccentCyan,
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                StarDots(count = summary.starCount.coerceAtMost(8))
            }
        }
    }
}

@Composable
private fun StarDots(count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(AccentPurple.copy(alpha = 0.7f), CircleShape)
            )
        }
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✦", fontSize = 48.sp, color = AccentPurple.copy(alpha = 0.4f))
        Spacer(Modifier.height(16.dp))
        Text(
            "No focus sessions yet",
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Start a session to earn your first stars",
            color = TextDim,
            fontSize = 13.sp
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDate(isoDate: String): String = try {
    val input  = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val output = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    output.format(input.parse(isoDate)!!)
} catch (e: Exception) {
    isoDate
}
