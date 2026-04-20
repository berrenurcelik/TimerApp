package com.example.timerapp.shared

class TimerEngine {

    private var state = TimerState()

    fun start() {
        if (state.isRunning) return
        state = state.copy(isRunning = true)
    }

    fun pause() {
        if (!state.isRunning) return
        state = state.copy(isRunning = false)
    }

    fun addLap() {
        // We use the already calculated formatted strings from the state
        if (state.elapsedMillis > 0) {
            val lapTime = "${state.formattedTime}${state.formattedMillis}"
            val newLaps = listOf(lapTime) + state.laps
            state = state.copy(laps = newLaps)
        }
    }

    fun reset() {
        // Completely reset the state to its initial values
        state = TimerState(laps = emptyList())
    }

    fun tick(): TimerState {
        // If the timer is active, increment the time
        if (state.isRunning) {
            state = state.copy(elapsedMillis = state.elapsedMillis + 10)
        }
        return state
    }

    fun restore(elapsedMillis: Long, laps: List<String>) {
        state = TimerState(
            elapsedMillis = elapsedMillis,
            isRunning = false,
            laps = laps
        )
    }

    fun getCurrentState(): TimerState = state
}