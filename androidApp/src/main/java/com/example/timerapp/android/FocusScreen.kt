package com.example.timerapp.android

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.timerapp.shared.FocusState
import kotlin.random.Random

private val SpaceBackground = Color(0xFF050A18)
private val StarWhite       = Color(0xFFE8EAF6)
private val PlanetCore      = Color(0xFF7C4DFF)
private val PlanetEdge      = Color(0xFF1A0050)
private val NebulaGlow      = Color(0x55BB86FC)
private val WorkRing        = Color(0xFF6200EE)
private val BreakRing       = Color(0xFF00BCD4)

@Composable
fun FocusScreen(viewModel: FocusViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    Box(modifier = Modifier.fillMaxSize()) {
        GalaxyCanvas(state = state, modifier = Modifier.fillMaxSize())
        FocusControls(
            state   = state,
            onStart = viewModel::start,
            onPause = viewModel::pause,
            onReset = viewModel::reset,
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
        )
    }
}

// ── Galaxy Canvas ─────────────────────────────────────────────────────────────

/**
 * Draws three animated layers:
 *   1. Stars     — appear as progress (0→1) increases
 *   2. Planet    — grows with each completed round
 *   3. Nebula    — pulsing ring visible only during breaks
 *   4. Progress ring — arc around planet, fills as phase progresses
 */
@Composable
fun GalaxyCanvas(state: FocusState, modifier: Modifier = Modifier) {

    val animatedProgress by animateFloatAsState(
        targetValue   = state.progress,
        animationSpec = tween(1200, easing = EaseInOutCubic),
        label         = "progress"
    )
    val planetRadius by animateFloatAsState(
        targetValue   = 44f + (state.round - 1) * 16f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label         = "planet"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "nebula")
    val nebulaPulse by infiniteTransition.animateFloat(
        initialValue  = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Reverse),
        label         = "pulse"
    )

    // Stars regenerated once per round — stable within a round, fresh pattern each new round
    val stars = remember(state.round) { generateStars(count = 160, seed = state.round.toLong()) }

    Canvas(modifier = modifier.background(SpaceBackground)) {
        drawStars(stars, animatedProgress)
        if (state.isOnBreak) drawNebula(planetRadius, nebulaPulse)
        drawPlanet(planetRadius)
        drawProgressRing(state, animatedProgress, planetRadius)
    }
}

private data class Star(val normX: Float, val normY: Float, val radius: Float, val alpha: Float)

private fun generateStars(count: Int, seed: Long): List<Star> {
    val rng = Random(seed)
    return List(count) {
        Star(rng.nextFloat(), rng.nextFloat(), rng.nextFloat() * 1.8f + 0.4f, rng.nextFloat() * 0.65f + 0.35f)
    }
}

private fun DrawScope.drawStars(stars: List<Star>, progress: Float) {
    val visible = (progress * stars.size).toInt().coerceAtLeast(8)
    stars.take(visible).forEachIndexed { i, s ->
        val frac = (i.toFloat() / visible).coerceIn(0f, 1f)
        drawCircle(
            color  = StarWhite.copy(alpha = s.alpha * frac * (0.3f + progress * 0.7f)),
            radius = s.radius,
            center = Offset(s.normX * size.width, s.normY * size.height)
        )
    }
}

private fun DrawScope.drawPlanet(radius: Float) {
    drawCircle(
        brush  = Brush.radialGradient(listOf(PlanetCore, PlanetEdge), center = center, radius = radius),
        radius = radius, center = center
    )
}

private fun DrawScope.drawNebula(planetRadius: Float, pulse: Float) {
    val r = planetRadius + 28f + pulse * 22f
    drawCircle(color = NebulaGlow, radius = r, center = center, style = Stroke(10f + pulse * 5f))
    drawCircle(color = NebulaGlow.copy(alpha = 0.18f), radius = r + 24f, center = center, style = Stroke(28f))
}

private fun DrawScope.drawProgressRing(state: FocusState, progress: Float, planetRadius: Float) {
    val color  = if (state.isOnBreak) BreakRing else WorkRing
    val r      = planetRadius + 18f
    val tl     = Offset(center.x - r, center.y - r)
    val sz     = Size(r * 2, r * 2)
    drawArc(color.copy(alpha = 0.15f), -90f, 360f, false, tl, sz, style = Stroke(5f))
    if (progress > 0f)
        drawArc(color, -90f, progress * 360f, false, tl, sz, style = Stroke(5f, cap = StrokeCap.Round))
}

// ── Controls ──────────────────────────────────────────────────────────────────

@Composable
private fun FocusControls(
    state: FocusState, onStart: () -> Unit, onPause: () -> Unit,
    onReset: () -> Unit, modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 56.dp)) {
            Text(state.phaseLabel, color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Light, letterSpacing = 4.sp)
            Spacer(Modifier.height(12.dp))
            RoundDots(current = state.round, total = state.totalRounds)
        }

        Text(
            text = if (state.isFinished) "DONE!" else state.formattedTime,
            fontSize = 84.sp, fontWeight = FontWeight.Thin,
            fontFamily = FontFamily.Monospace, color = Color.White
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 52.dp)) {
            Button(
                onClick = { if (state.isRunning) onPause() else onStart() },
                enabled = !state.isFinished,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRunning) BreakRing else WorkRing,
                    disabledContainerColor = Color.White.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    text = when { state.isFinished -> "Cycle Complete!"; state.isRunning -> "PAUSE"; else -> "START" },
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp
                )
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
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
            Box(
                modifier = Modifier
                    .size(if (i < current) 11.dp else 8.dp)
                    .background(
                        color = if (i < current) Color(0xFFBB86FC) else Color.White.copy(alpha = 0.22f),
                        shape = CircleShape
                    )
            )
        }
    }
}
