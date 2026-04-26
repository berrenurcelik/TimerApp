package com.example.timerapp.shared

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.example.timerapp.db.TimerDatabase

/** iOS actual — stores DB in Application Support (sandboxed, survives updates). */
actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(TimerDatabase.Schema, "timerapp.db")
}
