package org.openyaske.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import org.openyaske.core.db.OpenYaskeDatabase

/**
 * iOS / Kotlin/Native 用。スキーマ作成は NativeSqliteDriver に任せる。
 * Apple ターゲットは macOS でのみコンパイルできる。
 */
class IosDatabaseDriverFactory(
    private val databaseName: String = "openyaske.db"
) : DatabaseDriverFactory {
    override fun create(): SqlDriver {
        val driver = NativeSqliteDriver(OpenYaskeDatabase.Schema, databaseName)
        driver.execute(null, "PRAGMA foreign_keys = ON", 0)
        return driver
    }
}
