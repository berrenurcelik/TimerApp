package com.example.timerapp.android

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.timerapp.shared.FocusSettings
import com.example.timerapp.shared.FocusState
import kotlin.random.Random

private val SpaceBackground = Color(0xFF050A18)
private val StarWhite       = Color(0xFFE8EAF6)
private val StarSession     = Color(0xFFBB86FC)
private val PlanetCore      = Color(0xFF7C4DFF)
private val PlanetEdge      = Color(0xFF1A0050)
private val NebulaGlow      = Color(0x55BB86FC)
private val WorkRing        = Color(0xFF6200EE)
private val BreakRing       = Color(0xFF00BCD4)
private val AccentPurple    = Color(0xFFBB86FC)
private val SheetBg         = Color(0xFF0D1428)
private val TextDim         = Color(0xFF8888AA)

private const val SECS_PER_STAR = 300L
private const val STAR_POOL     = 200

// ── Root ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusScreen(viewModel: FocusViewModel = viewModel()) {
    val state       by viewModel.state.collectAsState()
    val allTimeSecs by viewModel.allTimeSecs.collectAsState()
    val settings    by viewModel.settings.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        GalaxyCanvas(state = state, allTimeSecs = allTimeSecs, modifier = Modifier.fillMaxSize())

        FocusControls(
            state       = state,
            onStart     = viewModel::start,
            onPause     = viewModel::pause,
            onReset     = viewModel::reset,
            onSettings  = { showSettings = true },
            modifier    = Modifier.fillMaxSize().padding(horizontal = 24.dp)
        )
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor   = SheetBg
        ) {
            SettingsSheet(
                settings  = settings,
                isRunning = state.isRunning,
                onChange  = viewModel::updateSettings,
                onDismiss = { showSettings = false }
            )
        }
    }
}

// ── Galaxy Canvas ─────────────────────────────────────────────────────────────

/**
 * Draws four animated layers:
 *   1. Stars        — white (DB history) + purple (live session), 1 star per 5 min, fixed seed
 *   2. Planet       — grows with each completed round
 *   3. Nebula       — pulsing ring visible only during breaks
 *   4. Progress ring — arc around planet, fills as phase progresses
 */
@Composable
fun GalaxyCanvas(state: FocusState, allTimeSecs: Long, modifier: Modifier = Modifier) {
    val pastStarCount  = (allTimeSecs / SECS_PER_STAR).toInt().coerceAtMost(STAR_POOL)
    val totalStarCount = ((allTimeSecs + state.sessionFocusSecs) / SECS_PER_STAR).toInt().coerceAtMost(STAR_POOL)

    val animatedTotal by animateIntAsState(totalStarCount, tween(1500, easing = EaseOutCubic), label = "stars")
    val animatedProgress by animateFloatAsState(state.progress, tween(1200, easing = EaseInOutCubic), label = "progress")
    val planetRadius by animateFloatAsState(
        44f + (state.round - 1) * 16f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "planet"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "nebula")
    val nebulaPulse by infiniteTransition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse), label = "pulse"
    )
    val stars = remember { generateStars(count = STAR_POOL, seed = 42L) }

    Canvas(modifier = modifier.background(SpaceBackground)) {
        drawStars(stars, animatedTotal, pastStarCount)
        if (state.isOnBreak) drawNebula(planetRadius, nebulaPulse)
        drawPlanet(planetRadius)
        drawProgressRing(state, animatedProgress, planetRadius)
    }
}

private data class Star(val normX: Float, val normY: Float, val radius: Float, val alpha: Float)

private fun generateStars(count: Int, seed: Long): List<Star> {
    val rng = Random(seed)
    return List(count) { Star(rng.nextFloat(), rng.nextFloat(), rng.nextFloat() * 1.8f + 0.4f, rng.nextFloat() * 0.65f + 0.35f) }
}

private fun DrawScope.drawStars(stars: List<Star>, totalVisible: Int, pastCount: Int) {
    stars.take(totalVisible).forEachIndexed { i, s ->
        val isPast = i < pastCount
        val color  = if (isPast) StarWhite else StarSession
        drawCircle(color.copy(alpha = s.alpha * if (isPast) 1f else 0.8f), s.radius,
            Offset(s.normX * size.width, s.normY * size.height))
    }
}

private fun DrawScope.drawPlanet(radius: Float) {
    drawCircle(Brush.radialGradient(listOf(PlanetCore, PlanetEdge), center, radius), radius, center)
}

private fun DrawScope.drawNebula(planetRadius: Float, pulse: Float) {
    val r = planetRadius + 28f + pulse * 22f
    drawCircle(NebulaGlow, r, center, style = Stroke(10f + pulse * 5f))
    drawCircle(NebulaGlow.copy(alpha = 0.18f), r + 24f, center, style = Stroke(28f))
}

private fun DrawScope.drawProgressRing(state: FocusState, progress: Float, planetRadius: Float) {
    val color = if (state.isOnBreak) BreakRing else WorkRing
    val r = planetRadius + 18f
    val tl = Offset(center.x - r, center.y - r)
    val sz = Size(r * 2, r * 2)
    drawArc(color.copy(alpha = 0.15f), -90f, 360f, false, tl, sz, style = Stroke(5f))
    if (progress > 0f)
        drawArc(color, -90f, progress * 360f, false, tl, sz, style = Stroke(5f, cap = StrokeCap.Round))
}

// ── Controls ──────────────────────────────────────────────────────────────────

@Composable
private fun FocusControls(
    state: FocusState, onStart: () -> Unit, onPause: () -> Unit,
    onReset: () -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(40.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.phaseLabel, color = Color.White.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Light, letterSpacing = 4.sp)
                Spacer(Modifier.height(12.dp))
                RoundDots(current = state.round, total = state.totalRounds)
            }
            IconButton(onClick = onSettings, enabled = !state.isRunning) {
                Icon(Icons.Default.Settings, contentDescription = "Settings",
                    tint = if (state.isRunning) Color.White.copy(alpha = 0.2f)
                           else Color.White.copy(alpha = 0.55f))
            }
        }

        Text(
            text = state.formattedTime,
            fontSize = 84.sp, fontWeight = FontWeight.Thin,
            fontFamily = FontFamily.Monospace, color = Color.White
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 52.dp)) {
            if (state.sessionFocusSecs > 0) {
                Text(
                    "${state.formattedSessionTime} focused · ${state.pauseCount} pauses",
                    color = Color.White.copy(alpha = 0.45f),
                    style = MaterialTheme.typography.bodySmall, letterSpacing = 1.sp
                )
                Spacer(Modifier.height(16.dp))
            }
            Button(
                onClick  = { if (state.isRunning) onPause() else onStart() },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRunning) BreakRing else WorkRing
                )
            ) {
                Text(if (state.isRunning) "PAUSE" else "START",
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick  = onReset,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(16.dp),
                border   = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
            ) {
                Text("RESET", color = Color.White.copy(alpha = 0.6f), letterSpacing = 2.sp)
            }
        }
    }
}

@Composable
private fun RoundDots(current: Int, total: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(total) { i ->
            Box(Modifier
                .size(if (i < current) 11.dp else 8.dp)
                .background(if (i < current) AccentPurple else Color.White.copy(alpha = 0.22f), CircleShape))
        }
    }
}

// ── Settings Sheet ────────────────────────────────────────────────────────────

@Composable
private fun SettingsSheet(
    settings: FocusSettings,
    isRunning: Boolean,
    onChange: (FocusSettings) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 48.dp)
    ) {
        Text("Session Settings", color = Color.White,
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Changes apply after Reset", color = TextDim,
            style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(24.dp))

        SettingRow("Work", settings.workMinutes, 1..90, "min") {
            onChange(settings.copy(workMinutes = it))
        }
        SettingRow("Short Break", settings.shortBreakMinutes, 1..30, "min") {
            onChange(settings.copy(shortBreakMinutes = it))
        }
        SettingRow("Long Break", settings.longBreakMinutes, 5..60, "min") {
            onChange(settings.copy(longBreakMinutes = it))
        }
        SettingRow("Rounds", settings.rounds, 1..8, "") {
            onChange(settings.copy(rounds = it))
        }
    }
}

@Composable
private fun SettingRow(
    label: String, value: Int, range: IntRange,
    unit: String, onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Text(label, color = Color.White.copy(alpha = 0.85f))
            Text(
                if (unit.isNotEmpty()) "$value $unit" else "$value",
                color = AccentPurple, fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value         = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange    = range.first.toFloat()..range.last.toFloat(),
            colors        = SliderDefaults.colors(thumbColor = AccentPurple, activeTrackColor = AccentPurple)
        )
        Spacer(Modifier.height(4.dp))
    }
}
