package org.openyaske.app.ui.timetable

/**
 * 時間割の背景画像。
 *
 * 画像は設定 JSON に入れない（肥大化するため）。保存先はプラットフォームごとに分ける。
 * - Web: IndexedDB
 * - Android: filesDir
 * - iOS: Documents
 */
expect object TimetableBackground {
    /** 保存済みの JPEG を読む。無ければ null。 */
    suspend fun load(): ByteArray?

    /** 画像を選び、長辺 1600px・JPEG 80% にして保存する。選ばなかったら false。 */
    suspend fun pickAndSave(): Boolean

    /** 保存済みの画像を消す。 */
    suspend fun clear()
}
