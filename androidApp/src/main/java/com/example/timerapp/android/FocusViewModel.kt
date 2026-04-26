package com.example.timerapp.android

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.timerapp.shared.FocusRepository
import com.example.timerapp.shared.FocusState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Android bridge between shared FocusRepository and Compose UI.
 * Drives tick() every second via a viewModelScope coroutine.
 * Flow: FocusRepository → StateFlow → Compose (one-way, read-only for UI).
 */
class FocusViewModel : ViewModel() {

    private val repository = FocusRepository()
    private val _state = MutableStateFlow(repository.getState())
    val state: StateFlow<FocusState> = _state.asStateFlow()
    private var tickJob: Job? = null

    fun start() {
        repository.start()
        _state.value = repository.getState()
        startTickLoop()
    }

    fun pause() {
        repository.pause()
        tickJob?.cancel()
        _state.value = repository.getState()
    }

    fun reset() {
        repository.reset()
        tickJob?.cancel()
        _state.value = repository.getState()
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                _state.value = repository.tick()
            }
        }
    }
}
