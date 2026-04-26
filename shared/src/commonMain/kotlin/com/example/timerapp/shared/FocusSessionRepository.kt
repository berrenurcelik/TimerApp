package com.example.timerapp.shared

import com.example.timerapp.db.TimerDatabase

/** Persists completed focus sessions to SQLite and exposes aggregate queries for History UI. */
class FocusSessionRepository(driverFactory: DatabaseDriverFactory) {

    private val queries = TimerDatabase(driverFactory.createDriver()).focusSessionQueries

    fun saveSession(
        date: String,
        focusSecs: Long,
        pauseCount: Int,
        roundsDone: Int,
        startedAt: Long
    ) {
        queries.insertSession(
            date       = date,
            focusSecs  = focusSecs,
            pauseCount = pauseCount.toLong(),
            roundsDone = roundsDone.toLong(),
            startedAt  = startedAt
        )
    }

    fun getAllTimeFocusSecs(): Long =
        queries.getAllTimeFocusSecs().executeAsOne()

    fun getTodayFocusSecs(date: String): Long =
        queries.getTodayFocusSecs(date).executeAsOne()

    fun clearAll() = queries.clearAll()

    fun getDailySummaries(): List<DailySummary> =
        queries.getDailySummaries().executeAsList().map {
            DailySummary(
                date           = it.date,
                totalFocusSecs = it.totalFocusSecs,
                totalPauses    = it.totalPauseCount.toInt(),
                sessionCount   = it.sessionCount.toInt()
            )
        }
}
