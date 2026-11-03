package org.openyaske.core.model

import kotlinx.serialization.Serializable

/**
 * 学年 + 学期。
 *
 * `id` は `year-semester` から導出する。APK 版は `instanceId` を主キーにしていたため、
 * 同じ year + semester の行を複数作れてしまっていた。
 */
@Serializable
data class AcademicYear(
    val id: String,
    val year: Int,
    val semester: Semester,
    val grade: String? = null
) {
    companion object {
        fun idOf(year: Int, semester: Semester): String = "$year-${semester.name.lowercase()}"

        fun of(year: Int, semester: Semester, grade: String? = null): AcademicYear =
            AcademicYear(id = idOf(year, semester), year = year, semester = semester, grade = grade)
    }
}

@Serializable
data class CalendarEvent(
    val id: String,
    val title: String,
    val allDay: Boolean = false,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val location: String? = null,
    val memo: String? = null,
    val calendarId: String = "default",
    val color: String = Course.DEFAULT_COLOR,
    val cancelled: Boolean = false,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L
) {
    /** 終了が開始より前の予定は 0 分予定として扱う。 */
    val normalizedEndAtMillis: Long get() = if (endAtMillis < startAtMillis) startAtMillis else endAtMillis
}
