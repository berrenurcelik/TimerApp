package com.example.timerapp.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.timerapp.shared.DailySummary
import com.example.timerapp.shared.DatabaseDriverFactory
import com.example.timerapp.shared.FocusSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(app: Application) : AndroidViewModel(app) {

    private val sessionRepo = FocusSessionRepository(DatabaseDriverFactory(app))

    private val _summaries = MutableStateFlow<List<DailySummary>>(emptyList())
    val summaries: StateFlow<List<DailySummary>> = _summaries.asStateFlow()

    private val _allTimeSecs = MutableStateFlow(0L)
    val allTimeSecs: StateFlow<Long> = _allTimeSecs.asStateFlow()

    init { load() }

    fun refresh() { load() }

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            sessionRepo.clearAll()
            load()
        }
    }

    private fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _summaries.value   = sessionRepo.getDailySummaries()
            _allTimeSecs.value = sessionRepo.getAllTimeFocusSecs()
        }
    }
}
