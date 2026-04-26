package com.example.timerapp.shared

data class DailySummary(
    val date: String,
    val totalFocusSecs: Long,
    val totalPauses: Int,
    val sessionCount: Int
) {
    /** "30m"  /  "1.5h"  /  "2h" — no seconds shown in history */
    val formattedDuration: String get() {
        val totalMinutes = totalFocusSecs / 60
        if (totalMinutes < 1)  return "< 1m"
        if (totalMinutes < 60) return "${totalMinutes}m"
        val h      = totalMinutes / 60
        val tenths = (totalMinutes % 60) * 10 / 60
        return if (tenths == 0L) "${h}h" else "${h}.${tenths}h"
    }

    val starCount: Int get() = (totalFocusSecs / 300).toInt() // 1 star per 5 min (prod)
}
