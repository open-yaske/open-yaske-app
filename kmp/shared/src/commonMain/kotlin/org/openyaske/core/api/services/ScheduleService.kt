package org.openyaske.core.api.services

import org.openyaske.core.api.CoreException
import org.openyaske.core.model.*
import org.openyaske.core.api.CourseDetailView
import org.openyaske.core.api.TimetableCellView
import org.openyaske.core.api.TimetableScreenState
import org.openyaske.core.logic.AgendaEntry
import org.openyaske.core.logic.AgendaProjector
import org.openyaske.core.logic.AttendanceCalculator
import org.openyaske.core.logic.TimetableIssue
import org.openyaske.core.logic.TimetableValidator

class ScheduleService internal constructor(private val store: CoreStore) {

    fun timetable(termId: String? = null): TimetableScreenState {
        val settings = store.settings.value
        val days = settings.visibleWeekdays.sortedBy { it.isoNumber }
        val periods = (1..settings.maxPeriod).toList()
        val courses = store.snapshot.courses.filter {
            it.deletedAtMillis == null && (termId == null || it.termId == termId)
        }
        val cells = linkedMapOf<TimetableSlot, TimetableCellView>()
        for (course in courses) {
            val slots = course.occupiedSlots().filter { it.weekday in days && it.period in periods }
            if (slots.isEmpty()) continue
            val sessions = store.snapshot.sessions.filter {
                it.courseId == course.id && it.deletedAtMillis == null
            }
            for (slot in slots) {
                if (cells[slot] == null) {
                    cells[slot] = TimetableCellView(course, slots, sessions)
                }
            }
        }
        // どのセルにも入らなかった科目を「時間割外」に出す。
        // 土曜や 7 限を設定で隠していても、合計単位数だけが増えて見えなくなることはない。
        val visibleCourseIds = cells.values.map { it.course.id }.toSet()
        val unscheduled = courses.filter { it.id !in visibleCourseIds }
        return TimetableScreenState(
            termId = termId,
            days = days,
            periods = periods,
            cells = cells,
            unscheduled = unscheduled,
            issues = TimetableValidator.validate(courses),
            totalCredits = courses.sumOf { it.credits }
        )
    }

    fun agenda(startDate: String, endDate: String): List<AgendaEntry> =
        AgendaProjector.range(
            startDate,
            endDate,
            store.snapshot.courses,
            store.snapshot.sessions,
            store.snapshot.schoolEvents,
            store.snapshot.courseTasks,
            store.settings.value.periodStartMinutes
        )

    fun courseDetail(courseId: String): CourseDetailView {
        val course = store.requireCourse(courseId)
        val records = store.snapshot.attendance.filter { it.courseId == courseId && it.deletedAtMillis == null }
        return CourseDetailView(
            course = course,
            sessions = store.snapshot.sessions.filter { it.courseId == courseId && it.deletedAtMillis == null }
                .sortedWith(compareBy({ it.date }, { it.startPeriod ?: 0 })),
            tasks = store.snapshot.courseTasks.filter { it.courseId == courseId && it.deletedAtMillis == null }
                .sortedWith(compareBy({ it.dueDate ?: "" }, { it.dueTime ?: "" })),
            attendance = AttendanceCalculator.summarize(records, store.settings.value.attendancePolicy),
            grade = store.snapshot.courseGrades.firstOrNull { it.courseId == courseId }
        )
    }

    // ----- syllabus / settings / backup -----
}
