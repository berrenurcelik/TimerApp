package com.example.timerapp.shared

import app.cash.sqldelight.db.SqlDriver

/** expect/actual: Android gets Context, iOS gets no-arg. Both produce a SqlDriver for TimerDatabase. */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}
