package org.openyaske.core.logic

import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseSession
import org.openyaske.core.model.CourseTask
import org.openyaske.core.model.SchoolEvent
import org.openyaske.core.model.TaskStatus
import org.openyaske.core.validation.IsoDate

/** 学校アジェンダに並べる要素。 */
sealed interface AgendaItem {
    data class Class(val session: CourseSession, val course: Course) : AgendaItem
    data class Event(val event: SchoolEvent) : AgendaItem
    data class Task(val task: CourseTask, val course: Course?) : AgendaItem
}

data class AgendaEntry(
    val date: String,
    /** 同日中の並び順（分）。 */
    val minutes: Int,
    val item: AgendaItem
)

/**
 * 授業回・学事・科目課題を 1 本の学校アジェンダに射影する。
 *
 * 個人の予定や TODO は対象外。呼び出し側が学校のデータだけを渡す。
 */
object AgendaProjector {
    fun forDate(
        date: String,
        courses: Collection<Course>,
        sessions: Collection<CourseSession>,
        events: Collection<SchoolEvent>,
        tasks: Collection<CourseTask>,
        periodStartMinutes: Map<Int, Int> = PeriodTimes.uecStartMinutes
    ): List<AgendaEntry> = range(date, date, courses, sessions, events, tasks, periodStartMinutes)

    fun range(
        startDate: String,
        endDate: String,
        courses: Collection<Course>,
        sessions: Collection<CourseSession>,
        events: Collection<SchoolEvent>,
        tasks: Collection<CourseTask>,
        periodStartMinutes: Map<Int, Int> = PeriodTimes.uecStartMinutes
    ): List<AgendaEntry> {
        if (!IsoDate.isValid(startDate) || !IsoDate.isValid(endDate)) return emptyList()
        if (IsoDate.isAfter(startDate, endDate)) return emptyList()

        val coursesById = courses.associateBy { it.id }
        val entries = mutableListOf<AgendaEntry>()

        fun inRange(date: String): Boolean =
            !IsoDate.isBefore(date, startDate) && !IsoDate.isAfter(date, endDate)

        for (session in sessions) {
            if (session.deletedAtMillis != null) continue
            if (!inRange(session.date)) continue
            val course = coursesById[session.courseId] ?: continue
            entries += AgendaEntry(
                date = session.date,
                minutes = minutesOfClass(session, periodStartMinutes),
                item = AgendaItem.Class(session, course)
            )
        }

        for (event in events) {
            if (event.deletedAtMillis != null) continue
            if (!inRange(event.date)) continue
            entries += AgendaEntry(
                date = event.date,
                minutes = if (event.allDay) -1 else (PeriodTimes.parseTimeToMinutes(event.startTime) ?: 0),
                item = AgendaItem.Event(event)
            )
        }

        for (task in tasks) {
            if (task.deletedAtMillis != null) continue
            if (task.status != TaskStatus.OPEN) continue
            val dueDate = task.dueDate ?: continue
            if (!inRange(dueDate)) continue
            entries += AgendaEntry(
                date = dueDate,
                minutes = PeriodTimes.parseTimeToMinutes(task.dueTime) ?: -1,
                item = AgendaItem.Task(task, coursesById[task.courseId])
            )
        }

        return entries.sortedWith(
            compareBy<AgendaEntry>(
                { it.date },
                { it.minutes },
                { itemOrder(it.item) },
                { titleOf(it.item) }
            )
        )
    }

    private fun minutesOfClass(session: CourseSession, periodStartMinutes: Map<Int, Int>): Int {
        PeriodTimes.parseTimeToMinutes(session.startTime)?.let { return it }
        val period = session.startPeriod
        if (period != null) {
            periodStartMinutes[period]?.let { return it }
            return 8 * 60 + period * 60
        }
        return 8 * 60
    }

    private fun itemOrder(item: AgendaItem): Int = when (item) {
        is AgendaItem.Event -> 0
        is AgendaItem.Class -> 1
        is AgendaItem.Task -> 2
    }

    private fun titleOf(item: AgendaItem): String = when (item) {
        is AgendaItem.Event -> item.event.title
        is AgendaItem.Class -> item.course.name
        is AgendaItem.Task -> item.task.title
    }
}
