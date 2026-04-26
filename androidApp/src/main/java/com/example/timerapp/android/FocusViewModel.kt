package com.example.timerapp.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timerapp.shared.DatabaseDriverFactory
import com.example.timerapp.shared.FocusRepository
import com.example.timerapp.shared.FocusSessionRepository
import com.example.timerapp.shared.FocusSettings
import com.example.timerapp.shared.FocusState
import kotlinx.coroutines.Dispatchers
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
class FocusViewModel(app: Application) : AndroidViewModel(app) {

    private val focusRepo   = FocusRepository()
    private val sessionRepo = FocusSessionRepository(DatabaseDriverFactory(app))

    private val _settings = MutableStateFlow(FocusSettings())
    val settings: StateFlow<FocusSettings> = _settings.asStateFlow()

    private val _state = MutableStateFlow(focusRepo.getState())
    val state: StateFlow<FocusState> = _state.asStateFlow()

    private val _allTimeSecs = MutableStateFlow(0L)
    val allTimeSecs: StateFlow<Long> = _allTimeSecs.asStateFlow()

    private var tickJob: Job? = null
    private var lastSavedCycles  = 0
    private var sessionStartedAt = 0L

    init {
        focusRepo.applySettings(_settings.value)
        _state.value = focusRepo.getState()
        viewModelScope.launch(Dispatchers.IO) {
            _allTimeSecs.value = sessionRepo.getAllTimeFocusSecs()
        }
    }

    fun updateSettings(settings: FocusSettings) {
        if (_state.value.isRunning) return
        _settings.value = settings
        focusRepo.applySettings(settings)
        _state.value = focusRepo.getState()
    }

    fun start() {
        if (sessionStartedAt == 0L) sessionStartedAt = System.currentTimeMillis()
        focusRepo.start()
        _state.value = focusRepo.getState()
        startTickLoop()
    }

    fun pause() {
        focusRepo.pause()
        tickJob?.cancel()
        _state.value = focusRepo.getState()
    }

    fun reset() {
        val snap = _state.value
        if (snap.sessionFocusSecs > 0L) persistSession(snap)
        focusRepo.reset()
        tickJob?.cancel()
        _state.value = focusRepo.getState()
        sessionStartedAt = 0L
        lastSavedCycles  = 0
    }

    private fun startTickLoop() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val prev = _state.value
                val next = focusRepo.tick()
                _state.value = next
                if (next.completedCycles > lastSavedCycles) {
                    lastSavedCycles = next.completedCycles
                    if (prev.sessionFocusSecs > 0L) {
                        persistSession(prev)
                        sessionStartedAt = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    private fun persistSession(state: FocusState) {
        if (state.sessionFocusSecs <= 0L) return
        val startedAt = if (sessionStartedAt > 0L) sessionStartedAt else System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            sessionRepo.saveSession(
                date       = todayDate(),
                focusSecs  = state.sessionFocusSecs,
                pauseCount = state.pauseCount,
                roundsDone = state.completedRounds,
                startedAt  = startedAt
            )
            _allTimeSecs.value = sessionRepo.getAllTimeFocusSecs()
        }
    }

    private fun todayDate(): String {
        val c = java.util.Calendar.getInstance()
        val y = c.get(java.util.Calendar.YEAR)
        val m = (c.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
        val d = c.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
        return "$y-$m-$d"
    }
}
