package org.openyaske.core.api.services

import org.openyaske.core.api.CoreException
import org.openyaske.core.logic.PeriodTimes
import org.openyaske.core.model.*
import org.openyaske.core.validation.IsoDate

class TaskService internal constructor(private val store: CoreStore) {

    fun saveTask(task: CourseTask): CourseTask {
        // 科目なしの TODO を許す。紐づく場合だけ存在を確かめる。
        task.courseId?.let { store.requireCourse(it) }
        if (task.title.isBlank()) throw CoreException("task title is required")
        if (task.dueDate != null && !IsoDate.isValid(task.dueDate)) {
            throw CoreException("dueDate must be YYYY-MM-DD")
        }
        if (task.dueTime != null && PeriodTimes.parseTimeToMinutes(task.dueTime) == null) {
            throw CoreException("dueTime must be HH:MM")
        }
        if (task.sessionId != null && store.snapshot.sessions.none {
                it.id == task.sessionId && it.courseId == task.courseId
            }
        ) {
            throw CoreException("task session does not belong to the course")
        }
        val existing = store.snapshot.courseTasks.firstOrNull { it.id == task.id }
        val now = store.clock()
        val prepared = task.copy(
            id = task.id.ifBlank { store.ids.newId() },
            title = task.title.trim(),
            createdAtMillis = when {
                task.createdAtMillis != 0L -> task.createdAtMillis
                existing != null -> existing.createdAtMillis
                else -> now
            },
            updatedAtMillis = now
        )
        store.repository.saveCourseTask(prepared)
        store.notifyChanged()
        return prepared
    }

    fun setTaskStatus(taskId: String, status: TaskStatus) {
        val task = store.snapshot.courseTasks.firstOrNull { it.id == taskId && it.deletedAtMillis == null }
            ?: throw CoreException("task not found: $taskId")
        saveTask(task.copy(status = status))
        if (status == TaskStatus.DONE) createNextOccurrence(task)
    }

    /** 繰り返しの課題を完了にしたら、次の 1 件を未完了で作る。 */
    private fun createNextOccurrence(task: CourseTask) {
        val pattern = task.repeat ?: return
        val base = task.dueDate ?: return
        val next = when (pattern) {
            RepeatPattern.NONE -> return
            RepeatPattern.DAILY -> IsoDate.plusDays(base, 1)
            RepeatPattern.WEEKLY -> IsoDate.plusDays(base, 7)
            RepeatPattern.BIWEEKLY -> IsoDate.plusDays(base, 14)
            RepeatPattern.MONTHLY -> plusMonths(base, 1)
        }
        val duplicate = store.snapshot.courseTasks.any {
            it.deletedAtMillis == null &&
                it.status == TaskStatus.OPEN &&
                it.courseId == task.courseId &&
                it.title == task.title &&
                it.dueDate == next
        }
        if (duplicate) return
        saveTask(
            task.copy(
                id = "",
                status = TaskStatus.OPEN,
                dueDate = next,
                createdAtMillis = 0,
                updatedAtMillis = 0
            )
        )
    }

    /** YYYY-MM-DD に月を足す。月末はその月の末日に丸める。 */
    private fun plusMonths(date: String, months: Int): String {
        val year = date.substring(0, 4).toInt()
        val month = date.substring(5, 7).toInt()
        val day = date.substring(8, 10).toInt()
        val zeroBased = month - 1 + months
        val newYear = year + zeroBased / 12
        val newMonth = zeroBased % 12 + 1
        val newDay = day.coerceAtMost(IsoDate.daysInMonth(newYear, newMonth))
        return buildString {
            append(newYear.toString().padStart(4, '0'))
            append('-')
            append(newMonth.toString().padStart(2, '0'))
            append('-')
            append(newDay.toString().padStart(2, '0'))
        }
    }

    fun deleteTask(taskId: String) {
        store.repository.softDeleteCourseTask(taskId, store.clock())
        store.notifyChanged()
    }

    fun deleteSchoolEvent(eventId: String) {
        store.repository.softDeleteSchoolEvent(eventId, store.clock())
        store.notifyChanged()
    }

    fun saveSchoolEvent(event: SchoolEvent): SchoolEvent {
        if (event.title.isBlank()) throw CoreException("event title is required")
        if (!IsoDate.isValid(event.date)) throw CoreException("event date must be YYYY-MM-DD")
        if (event.startTime != null && PeriodTimes.parseTimeToMinutes(event.startTime) == null) {
            throw CoreException("startTime must be HH:MM")
        }
        if (event.endTime != null && PeriodTimes.parseTimeToMinutes(event.endTime) == null) {
            throw CoreException("endTime must be HH:MM")
        }
        if (event.courseId != null) store.requireCourse(event.courseId)
        if (event.sessionId != null && store.snapshot.sessions.none { it.id == event.sessionId }) {
            throw CoreException("event session does not exist")
        }
        val existing = store.snapshot.schoolEvents.firstOrNull { it.id == event.id }
        val now = store.clock()
        val prepared = event.copy(
            id = event.id.ifBlank { store.ids.newId() },
            title = event.title.trim(),
            createdAtMillis = when {
                event.createdAtMillis != 0L -> event.createdAtMillis
                existing != null -> existing.createdAtMillis
                else -> now
            },
            updatedAtMillis = now
        )
        store.repository.saveSchoolEvent(prepared)
        store.notifyChanged()
        return prepared
    }

    // ----- grades / gpa -----
}
