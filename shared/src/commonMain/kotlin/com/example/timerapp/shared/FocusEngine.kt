package com.example.timerapp.shared

/**
 * Countdown state machine for the Pomodoro focus cycle.
 * Pure Kotlin — no platform APIs, no coroutines.
 * The caller (platform ViewModel) is responsible for driving tick() every second.
 *
 * Cycle: WORK → SHORT_BREAK → WORK → ... → LONG_BREAK → restart (infinite)
 */
class FocusEngine(
    private val workMillis: Long       = 25 * 60_000L,
    private val shortBreakMillis: Long = 5  * 60_000L,
    private val longBreakMillis: Long  = 15 * 60_000L,
    private val totalRounds: Int       = 4
) {
    private var remainingMillis: Long     = workMillis
    private var currentPhase: FocusPhase = FocusPhase.WORK
    private var currentRound: Int         = 1
    private var running: Boolean          = false

    private var workSecsAccumulated: Long = 0L
    private var pauseCount: Int           = 0
    private var completedRounds: Int      = 0
    private var completedCycles: Int      = 0

    fun start() { running = true }

    fun pause() {
        if (running) { pauseCount++; running = false }
    }

    fun reset() {
        running = false
        currentRound = 1
        currentPhase = FocusPhase.WORK
        remainingMillis = workMillis
        workSecsAccumulated = 0L
        pauseCount = 0
        completedRounds = 0
        completedCycles = 0
    }

    /** Called every second by the platform ViewModel. Handles phase transitions automatically. */
    fun tick() {
        if (!running) return
        if (currentPhase == FocusPhase.WORK) workSecsAccumulated++
        remainingMillis -= 1_000L
        if (remainingMillis <= 0L) advancePhase()
    }

    private fun advancePhase() {
        when (currentPhase) {
            FocusPhase.WORK -> {
                completedRounds++
                if (currentRound >= totalRounds) {
                    currentPhase = FocusPhase.LONG_BREAK
                    remainingMillis = longBreakMillis
                } else {
                    currentPhase = FocusPhase.SHORT_BREAK
                    remainingMillis = shortBreakMillis
                }
            }
            FocusPhase.SHORT_BREAK -> {
                currentRound++
                currentPhase = FocusPhase.WORK
                remainingMillis = workMillis
            }
            FocusPhase.LONG_BREAK -> {
                completedCycles++
                currentRound = 1
                currentPhase = FocusPhase.WORK
                remainingMillis = workMillis
                workSecsAccumulated = 0L
                pauseCount = 0
                completedRounds = 0
            }
        }
    }

    /** Returns an immutable snapshot — always call after tick() to get fresh data. */
    fun getCurrentState() = FocusState(
        remainingMillis  = remainingMillis,
        totalMillis      = currentPhaseDuration(),
        phase            = currentPhase,
        round            = currentRound,
        totalRounds      = totalRounds,
        isRunning        = running,
        sessionFocusSecs = workSecsAccumulated,
        pauseCount       = pauseCount,
        completedRounds  = completedRounds,
        completedCycles  = completedCycles
    )

    private fun currentPhaseDuration() = when (currentPhase) {
        FocusPhase.WORK        -> workMillis
        FocusPhase.SHORT_BREAK -> shortBreakMillis
        FocusPhase.LONG_BREAK  -> longBreakMillis
    }
}
