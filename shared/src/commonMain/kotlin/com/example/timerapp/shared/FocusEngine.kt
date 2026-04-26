package com.example.timerapp.shared

/**
 * Countdown state machine for the Pomodoro focus cycle.
 * Pure Kotlin — no platform APIs, no coroutines.
 * The caller (platform ViewModel) is responsible for driving tick() every second.
 *
 * Cycle: WORK → SHORT_BREAK → WORK → ... → LONG_BREAK → finished
 */
class FocusEngine(
    private val workMillis: Long       = 10_000L,   // TEST: 10s  (prod: 25 * 60_000L)
    private val shortBreakMillis: Long = 6_000L,    // TEST: 6s   (prod: 5  * 60_000L)
    private val longBreakMillis: Long  = 8_000L,    // TEST: 8s   (prod: 15 * 60_000L)
    private val totalRounds: Int       = 2          // TEST: 2    (prod: 4)
) {
    private var remainingMillis: Long    = workMillis
    private var currentPhase: FocusPhase = FocusPhase.WORK
    private var currentRound: Int        = 1
    private var running: Boolean         = false
    private var finished: Boolean        = false

    fun start() { if (!finished) running = true }
    fun pause() { running = false }

    fun reset() {
        running = false; finished = false
        currentRound = 1; currentPhase = FocusPhase.WORK
        remainingMillis = workMillis
    }

    /** Called every second by the platform ViewModel. Handles phase transitions automatically. */
    fun tick() {
        if (!running || finished) return
        remainingMillis -= 1_000L
        if (remainingMillis <= 0L) advancePhase()
    }

    private fun advancePhase() {
        when (currentPhase) {
            FocusPhase.WORK -> {
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
                finished = true; running = false
            }
        }
    }

    /** Returns an immutable snapshot — always call after tick() to get fresh data. */
    fun getCurrentState() = FocusState(
        remainingMillis = remainingMillis,
        totalMillis     = currentPhaseDuration(),
        phase           = currentPhase,
        round           = currentRound,
        totalRounds     = totalRounds,
        isRunning       = running,
        isFinished      = finished
    )

    private fun currentPhaseDuration() = when (currentPhase) {
        FocusPhase.WORK        -> workMillis
        FocusPhase.SHORT_BREAK -> shortBreakMillis
        FocusPhase.LONG_BREAK  -> longBreakMillis
    }
}
