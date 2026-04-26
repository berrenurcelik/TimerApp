package com.example.timerapp.shared

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.example.timerapp.db.TimerDatabase

/** Android actual — stores DB at /data/data/<pkg>/databases/timerapp.db (app-private). */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(TimerDatabase.Schema, context, "timerapp.db")
}
