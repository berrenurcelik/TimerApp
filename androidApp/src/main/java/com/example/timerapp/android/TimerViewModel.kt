package com.example.timerapp.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timerapp.shared.TimerRepository
import com.example.timerapp.shared.TimerState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * TimerViewModel acts as a bridge between the Shared Module (Logic) and the Android UI.
 * It manages the lifecycle of the timer and provides an observable state to the Compose UI.
 */
class TimerViewModel : ViewModel() {

    // Access point to the shared logic
    private val repository = TimerRepository()

    // Internal mutable state flow to hold the current timer data
    private val _timerState = MutableStateFlow(TimerState())

    // Read-only state flow exposed to the UI (Compose)
    val timerState = _timerState.asStateFlow()

    // Coroutine job to manage the 10ms update loop
    private var timerJob: Job? = null

    /**
     * Starts the timer in the shared module and begins the UI update loop.
     */
    fun startTimer() {
        repository.start()
        startTicking()
    }

    /**
     * Pauses the timer in the shared module and stops the UI update loop.
     */
    fun pauseTimer() {
        repository.pause()
        timerJob?.cancel() // Stop the coroutine loop
        _timerState.value = repository.getState() // Sync the latest state
    }

    /**
     * Resets the timer values in the shared module and stops the UI update loop.
     */
    fun resetTimer() {
        repository.reset()
        timerJob?.cancel() // Stop the coroutine loop
        _timerState.value = repository.getState() // Reset the UI state to 00:00
    }

    /**
     * A private helper that launches a coroutine to poll the shared module for updates every 10ms.
     */
    private fun startTicking() {
        timerJob?.cancel() // Ensure no duplicate loops are running
        timerJob = viewModelScope.launch {
            while (true) {
                // Request the latest calculated state from the Shared Module (CommonMain)
                _timerState.value = repository.tick()

                // Wait for 10 milliseconds before the next update for smooth UI
                delay(10)
            }
        }
    }
    fun addLap() {
        // Accessing the private repository to add a lap
        repository.addLap()

        /**
         * We update the private _state.value by calling tick().
         * tick() returns the latest TimerState from the repository.
         */
        _timerState.value = repository.tick()
    }
}