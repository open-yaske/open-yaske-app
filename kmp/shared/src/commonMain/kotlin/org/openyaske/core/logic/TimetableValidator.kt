package org.openyaske.core.logic

import org.openyaske.core.model.Course
import org.openyaske.core.model.TimetableSlot

sealed interface TimetableIssue {
    /** 時限・コマ数が時間割の範囲外。 */
    data class OutOfRange(
        val courseId: String,
        val period: Int,
        val periodCount: Int
    ) : TimetableIssue

    /** 同じセルを 2 つ以上の科目が占有している。 */
    data class Conflict(
        val slot: TimetableSlot,
        val firstCourseId: String,
        val secondCourseId: String
    ) : TimetableIssue
}

/**
 * 時間割の不変条件を検証する。
 *
 * APK 版は `timetable_cells` に UNIQUE 制約も期間の CHECK 制約もなく、
 * 同じセルに複数の科目を保存できた。ここでは保存前に必ず検証する。
 */
object TimetableValidator {
    fun validate(courses: List<Course>): List<TimetableIssue> {
        val issues = mutableListOf<TimetableIssue>()
        val occupancy = linkedMapOf<TimetableSlot, String>()

        for (course in courses) {
            val period = course.period ?: continue
            val weekday = course.weekday ?: continue
            val end = period + course.periodCount - 1
            if (period !in Course.MIN_PERIOD..Course.MAX_PERIOD ||
                course.periodCount !in 1..Course.MAX_PERIOD_COUNT ||
                end > Course.MAX_PERIOD
            ) {
                issues += TimetableIssue.OutOfRange(
                    courseId = course.id,
                    period = period,
                    periodCount = course.periodCount
                )
                continue
            }

            for (slot in course.occupiedSlots()) {
                val owner = occupancy[slot]
                if (owner == null) {
                    occupancy[slot] = course.id
                } else if (owner != course.id) {
                    issues += TimetableIssue.Conflict(slot, owner, course.id)
                }
            }
        }

        return issues
    }

    fun hasConflict(courses: List<Course>): Boolean =
        validate(courses).any { it is TimetableIssue.Conflict }
}
