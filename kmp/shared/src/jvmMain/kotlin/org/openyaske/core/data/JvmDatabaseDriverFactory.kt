package org.openyaske.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.openyaske.core.db.OpenYaskeDatabase

/**
 * JVM / デスクトップ用。`path` が null ならインメモリ。
 */
class JvmDatabaseDriverFactory(private val path: String? = null) : DatabaseDriverFactory {
    override fun create(): SqlDriver {
        val driver = if (path == null) {
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        } else {
            JdbcSqliteDriver("jdbc:sqlite:$path")
        }
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        OpenYaskeDatabase.Schema.create(driver)
        return driver
    }
}
