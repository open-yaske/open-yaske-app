package org.openyaske.core.logic

import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseKind
import org.openyaske.core.model.CourseSession
import org.openyaske.core.model.HolidayRule
import org.openyaske.core.model.SessionKind
import org.openyaske.core.model.SessionOverride
import org.openyaske.core.model.SessionStatus
import org.openyaske.core.model.Term
import org.openyaske.core.validation.IsoDate

/**
 * 週次パターンから学期分の授業回を生成する。
 *
 * 休日と上書きを適用し、通常回・休講・振替の状態を反映する。
 * 集中講義・連携大学・補講はこの生成器を使わず、`CourseSession` を直接追加する。
 */
object SessionGenerator {
    fun generate(
        course: Course,
        term: Term,
        holidays: List<HolidayRule> = emptyList(),
        overrides: List<SessionOverride> = emptyList()
    ): List<CourseSession> {
        val weekday = course.weekday ?: return emptyList()
        val period = course.period ?: return emptyList()
        if (period !in Course.MIN_PERIOD..Course.MAX_PERIOD) return emptyList()
        val periodCount = course.periodCount.coerceIn(
            1,
            (Course.MAX_PERIOD - period + 1).coerceAtMost(Course.MAX_PERIOD_COUNT)
        )
        if (course.kind == CourseKind.EXTRA ||
            course.kind == CourseKind.INTENSIVE ||
            course.kind == CourseKind.PARTNER
        ) {
            return emptyList()
        }
        if (!IsoDate.isValid(term.startDate) || !IsoDate.isValid(term.endDate)) return emptyList()
        if (IsoDate.isAfter(term.startDate, term.endDate)) return emptyList()

        val holidaysByDate = holidays.groupBy { it.date }
        val overridesByDate = overrides.groupBy { it.date }
        val sessions = mutableListOf<CourseSession>()

        var day = term.startDate
        val lastDay = term.endDate
        while (!IsoDate.isAfter(day, lastDay)) {
            if (IsoDate.dayOfWeekIso(day) == weekday.isoNumber &&
                !isHoliday(day, period, periodCount, holidaysByDate)
            ) {
                val override = overridesByDate[day]?.firstOrNull()
                val status = override?.status ?: SessionStatus.SCHEDULED
                sessions += CourseSession(
                    id = sessionId(course.id, day, period),
                    courseId = course.id,
                    date = day,
                    startPeriod = period,
                    periodCount = periodCount,
                    room = course.room,
                    kind = SessionKind.REGULAR,
                    status = status,
                    note = override?.note
                )
            }
            day = IsoDate.plusDays(day, 1)
        }

        return sessions
    }

    fun sessionId(courseId: String, date: String, period: Int): String = "$courseId@$date@$period"

    private fun isHoliday(
        date: String,
        period: Int,
        periodCount: Int,
        holidaysByDate: Map<String, List<HolidayRule>>
    ): Boolean {
        val rules = holidaysByDate[date] ?: return false
        val occupied = period..(period + periodCount - 1)
        return rules.any { rule ->
            val periods = rule.periods
            periods == null || periods.any { it in occupied }
        }
    }
}
