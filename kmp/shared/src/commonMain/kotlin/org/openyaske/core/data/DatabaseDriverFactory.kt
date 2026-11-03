package org.openyaske.core.data

import app.cash.sqldelight.db.SqlDriver
import org.openyaske.core.db.OpenYaskeDatabase

/**
 * プラットフォームごとの SQLite ドライバ生成。
 * 実装側でスキーマ作成と `PRAGMA foreign_keys = ON` まで済ませる。
 */
interface DatabaseDriverFactory {
    fun create(): SqlDriver
}

fun openDatabase(factory: DatabaseDriverFactory): OpenYaskeDatabase =
    OpenYaskeDatabase(factory.create())
