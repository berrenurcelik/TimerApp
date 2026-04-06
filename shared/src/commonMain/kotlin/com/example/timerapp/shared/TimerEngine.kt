package com.example.timerapp.shared

class TimerEngine {
    private var startTimeMillis: Long = 0L
    private var accumulatedMillis: Long = 0L
    private var isRunning: Boolean = false

    fun start() {
        if (isRunning) return
        startTimeMillis = currentTimeMillis()
        isRunning = true
    }

    fun pause() {
        if (!isRunning) return
        accumulatedMillis += currentTimeMillis() - startTimeMillis
        isRunning = false
    }

    fun reset() {
        startTimeMillis = 0L
        accumulatedMillis = 0L
        isRunning = false
    }

    fun tick(): TimerState {
        val currentElapsed = if (isRunning) {
            accumulatedMillis + (currentTimeMillis() - startTimeMillis)
        } else {
            accumulatedMillis
        }

        return TimerState(
            elapsedMillis = currentElapsed,
            isRunning = isRunning
        )
    }

    fun getCurrentState(): TimerState = tick()
}