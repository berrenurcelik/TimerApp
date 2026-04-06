package com.example.timerapp.shared

data class TimerState(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false
) {
    val formattedTime: String
        get() {
            val totalSeconds = elapsedMillis / 1000
            val minutes = (totalSeconds / 60) % 60
            val seconds = totalSeconds % 60
            return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }

    val formattedMillis: String
        get() {
            val millis = (elapsedMillis % 1000) / 10
            return ".${millis.toString().padStart(2, '0')}"
        }
}