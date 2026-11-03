package org.openyaske.core.model

import kotlinx.serialization.Serializable

/**
 * 出席記録。
 *
 * 日付は epoch millis ではなく ISO 日付文字列で持つ。
 * 原則として `sessionId` に紐づけ、補講・振替・集中講義でも同じ仕組みで扱う。
 * 時間割外の科目など授業回を作らない場合は `sessionId` を null にして日付だけで記録する。
 */
@Serializable
data class AttendanceRecord(
    val id: String,
    val courseId: String,
    /** 出席対象の授業回。日付だけの記録では null。 */
    val sessionId: String? = null,
    /** YYYY-MM-DD */
    val date: String,
    val status: AttendanceStatus,
    val recordedAtMillis: Long = 0L,
    /** 端末同期の墓標。null なら有効。 */
    val deletedAtMillis: Long? = null
)
