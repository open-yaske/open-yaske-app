package org.openyaske.core.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import org.openyaske.core.db.OpenYaskeDatabase

/**
 * Android 用。スキーマ作成は AndroidSqliteDriver に任せる。
 */
class AndroidDatabaseDriverFactory(
    private val context: Context,
    private val databaseName: String = "openyaske.db"
) : DatabaseDriverFactory {
    override fun create(): SqlDriver {
        val driver = AndroidSqliteDriver(OpenYaskeDatabase.Schema, context, databaseName)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        return driver
    }
}
