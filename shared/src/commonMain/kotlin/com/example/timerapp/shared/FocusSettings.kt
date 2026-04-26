package com.example.timerapp.shared

/** User-configurable session durations. Passed to FocusRepository.applySettings() after Reset. */
data class FocusSettings(
    val workMinutes: Int       = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int  = 15,
    val rounds: Int            = 4
)
