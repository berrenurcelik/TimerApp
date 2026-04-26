package com.example.timerapp.shared

/**
 * The three phases of a Pomodoro cycle.
 * Lives in commonMain — compiled for both Android (Kotlin/JVM) and iOS (Kotlin/Native).
 */
enum class FocusPhase { WORK, SHORT_BREAK, LONG_BREAK }

/**
 * Immutable snapshot of the focus timer.
 * Created fresh on every state change instead of mutating — safe for StateFlow / @Published.
 */
data class FocusState(
    val remainingMillis: Long,
    val totalMillis: Long,       // total duration of current phase, used for progress
    val phase: FocusPhase,
    val round: Int,              // 1-based
    val totalRounds: Int,
    val isRunning: Boolean,
    val isFinished: Boolean = false, // engine cycles infinitely — kept for UI compatibility
    val sessionFocusSecs: Long = 0L,
    val pauseCount: Int = 0,
    val completedRounds: Int = 0,
    val completedCycles: Int = 0
) {
    /** 0.0 → phase just started, 1.0 → phase complete. Drives galaxy animation. */
    val progress: Float
        get() = if (totalMillis == 0L) 0f
                else ((totalMillis - remainingMillis).toFloat() / totalMillis).coerceIn(0f, 1f)

    /** True during any break phase. Exposed as Bool so iOS avoids raw enum comparison. */
    val isOnBreak: Boolean get() = phase != FocusPhase.WORK

    /** Display label — computed once here so Android and iOS show identical text. */
    val phaseLabel: String
        get() = when (phase) {
            FocusPhase.WORK        -> "FOCUS"
            FocusPhase.SHORT_BREAK -> "SHORT BREAK"
            FocusPhase.LONG_BREAK  -> "LONG BREAK"
        }

    val formattedTime: String
        get() {
            val minutes = remainingMillis / 60_000L
            val seconds = (remainingMillis % 60_000L) / 1_000L
            return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }

    val formattedSessionTime: String
        get() {
            val h = sessionFocusSecs / 3600
            val m = (sessionFocusSecs % 3600) / 60
            val s = sessionFocusSecs % 60
            return when {
                h > 0 -> "${h}h ${m}m"
                m > 0 -> "${m}m ${s}s"
                else  -> "${s}s"
            }
        }
}
