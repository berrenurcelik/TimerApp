package com.example.timerapp.shared

/**
 * Public facade over FocusEngine — the only shared class platform code touches.
 * Recreates the engine on applySettings() so new durations take effect after Reset.
 */
class FocusRepository {
    private var engine = FocusEngine()

    fun applySettings(settings: FocusSettings) {
        engine = FocusEngine(
            workMillis       = settings.workMinutes.toLong()       * 60_000L,
            shortBreakMillis = settings.shortBreakMinutes.toLong() * 60_000L,
            longBreakMillis  = settings.longBreakMinutes.toLong()  * 60_000L,
            totalRounds      = settings.rounds
        )
    }

    fun start()  = engine.start()
    fun pause()  = engine.pause()
    fun reset()  = engine.reset()

    /** Advances engine by 1 s and returns the new state in one call. */
    fun tick(): FocusState {
        engine.tick()
        return engine.getCurrentState()
    }

    fun getState(): FocusState = engine.getCurrentState()
}
