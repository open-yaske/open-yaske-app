package org.openyaske.core.model

import kotlinx.serialization.Serializable

/**
 * 履修科目。
 *
 * 通常科目は曜日 + 時限の週次パターンを持ち、学期から授業回を生成する。
 * 時間割外・集中講義・連携大学・ユーザー定義の科目は週次パターンを持たず、
 * `CourseSession` を直接並べる。
 *
 * 設計上の約束:
 * - `period` は 1..7、`periodCount` は 1..3。`period + periodCount - 1` が 7 を超えない。
 * - `credits` は Int。0/1/2 の 3 値に固定しない。
 * - 日付ではなく曜日 + 時限で持ち、実際の日付は授業回で表現する。
 */
@Serializable
data class Course(
    val id: String,
    val name: String,
    val code: String? = null,
    val instructor: String? = null,
    val room: String? = null,
    val url: String? = null,
    val memo: String? = null,
    val type: CourseType = CourseType.FREE,
    val kind: CourseKind = CourseKind.REGULAR,
    val source: CourseSource = CourseSource.USER,
    val institution: String? = null,
    val credits: Int = 0,
    val creditSystem: CreditSystem = CreditSystem.UEC,
    val countsTowardGpa: Boolean = true,
    /** 週次パターン。時間割外・集中・連携科目では null。 */
    val weekday: Weekday? = null,
    val period: Int? = null,
    val periodCount: Int = 1,
    val color: String = DEFAULT_COLOR,
    val syllabusId: String? = null,
    val academicYearId: String? = null,
    val termId: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    /** 端末同期の墓標。null なら有効。 */
    val deletedAtMillis: Long? = null
) {
    val hasWeeklySlot: Boolean get() = weekday != null && period != null

    /** この科目が占有する (曜日, 時限) の集合。週次パターンがなければ空。 */
    fun occupiedSlots(): List<TimetableSlot> {
        val weekday = weekday ?: return emptyList()
        val period = period ?: return emptyList()
        val start = period.coerceIn(MIN_PERIOD, MAX_PERIOD)
        val end = (start + periodCount.coerceIn(1, MAX_PERIOD_COUNT) - 1).coerceAtMost(MAX_PERIOD)
        return (start..end).map { TimetableSlot(weekday, it) }
    }

    companion object {
        const val MIN_PERIOD = 1
        const val MAX_PERIOD = 7
        const val MAX_PERIOD_COUNT = 3
        const val DEFAULT_COLOR = "custom:#007aff"

        /** 同じ時間割セルを指す同一性キー。 */
        fun slotKey(weekday: Weekday, period: Int): String = "${weekday.name}:$period"
    }
}

/** 時間割の 1 セル。 */
data class TimetableSlot(val weekday: Weekday, val period: Int) {
    init {
        require(period in Course.MIN_PERIOD..Course.MAX_PERIOD) {
            "period must be in ${Course.MIN_PERIOD}..${Course.MAX_PERIOD}, was $period"
        }
    }
}
