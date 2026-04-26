package com.example.timerapp.shared

/**
 * Public facade over FocusEngine — the only shared class platform code touches.
 * No-arg constructor: Kotlin default params are not visible to Swift/Objective-C,
 * so the engine is created internally (same pattern as TimerRepository).
 */
class FocusRepository {
    private val engine = FocusEngine()

    fun start() = engine.start()
    fun pause() = engine.pause()
    fun reset() = engine.reset()

    /** Advances engine by 1 s and returns the new state in one call. */
    fun tick(): FocusState {
        engine.tick()
        return engine.getCurrentState()
    }

    fun getState(): FocusState = engine.getCurrentState()
}
