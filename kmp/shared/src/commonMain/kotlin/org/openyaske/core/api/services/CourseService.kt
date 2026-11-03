package org.openyaske.core.api.services

import org.openyaske.core.api.CoreException
import org.openyaske.core.data.CoreRepository
import org.openyaske.core.model.*
import org.openyaske.core.validation.IsoDate
import org.openyaske.core.logic.PeriodTimes

class CourseService internal constructor(private val store: CoreStore) {

    fun saveTerm(term: Term): Term {
        if (!IsoDate.isValid(term.startDate) || !IsoDate.isValid(term.endDate)) {
            throw CoreException("term dates must be YYYY-MM-DD")
        }
        if (IsoDate.isAfter(term.startDate, term.endDate)) {
            throw CoreException("term start must be before end")
        }
        val prepared = term.copy(
            id = term.id.ifBlank {
                term.season?.let { Term.idOf(term.year, it) } ?: store.ids.newId()
            }
        )
        store.repository.saveTerm(prepared)
        store.notifyChanged()
        return prepared
    }

    fun saveCourse(course: Course): Course {
        val existing = store.snapshot.courses.firstOrNull { it.id == course.id }
        val now = store.clock()
        val prepared = store.normalizeCourse(
            course.copy(
                id = course.id.ifBlank { store.ids.newId() },
                createdAtMillis = when {
                    course.createdAtMillis != 0L -> course.createdAtMillis
                    existing != null -> existing.createdAtMillis
                    else -> now
                },
                updatedAtMillis = now
            )
        )
        store.repository.saveCourse(prepared)
        store.notifyChanged()
        return prepared
    }

    fun deleteCourse(courseId: String) {
        store.repository.softDeleteCourse(courseId, store.clock())
        store.notifyChanged()
    }

    // ----- sessions -----

    fun saveSession(session: CourseSession): CourseSession {
        store.requireCourse(session.courseId)
        if (!IsoDate.isValid(session.date)) throw CoreException("session date must be YYYY-MM-DD")
        if (session.startPeriod != null && session.startPeriod !in Course.MIN_PERIOD..Course.MAX_PERIOD) {
            throw CoreException("startPeriod must be 1..7")
        }
        if (session.startPeriod != null) {
            val maxCount = (Course.MAX_PERIOD - session.startPeriod + 1).coerceAtMost(Course.MAX_PERIOD_COUNT)
            if (session.periodCount !in 1..maxCount) {
                throw CoreException("periodCount must be 1..$maxCount")
            }
        }
        if (session.startPeriod == null && session.startTime == null) {
            throw CoreException("session requires startPeriod or startTime")
        }
        if (session.startTime != null && PeriodTimes.parseTimeToMinutes(session.startTime) == null) {
            throw CoreException("startTime must be HH:MM")
        }
        if (session.endTime != null && PeriodTimes.parseTimeToMinutes(session.endTime) == null) {
            throw CoreException("endTime must be HH:MM")
        }
        if (session.replacesSessionId != null &&
            store.snapshot.sessions.none { it.id == session.replacesSessionId }
        ) {
            throw CoreException("replacesSessionId does not exist")
        }
        val existing = store.snapshot.sessions.firstOrNull { it.id == session.id }
        val now = store.clock()
        val prepared = session.copy(
            id = session.id.ifBlank { store.ids.newId() },
            periodCount = if (session.startPeriod == null) 1 else session.periodCount.coerceIn(1, 3),
            createdAtMillis = when {
                session.createdAtMillis != 0L -> session.createdAtMillis
                existing != null -> existing.createdAtMillis
                else -> now
            },
            updatedAtMillis = now
        )
        store.repository.saveSession(prepared)
        store.notifyChanged()
        return prepared
    }

    fun cancelSession(sessionId: String) {
        val session = store.requireSession(sessionId)
        saveSession(session.copy(status = SessionStatus.CANCELLED))
    }

    fun addMakeupSession(
        courseId: String,
        date: String,
        startPeriod: Int? = null,
        startTime: String? = null,
        endTime: String? = null,
        room: String? = null,
        replacesSessionId: String? = null
    ): CourseSession = saveSession(
        CourseSession(
            id = "",
            courseId = courseId,
            date = date,
            startPeriod = startPeriod,
            startTime = startTime,
            endTime = endTime,
            room = room,
            kind = SessionKind.MAKEUP,
            status = SessionStatus.SCHEDULED,
            replacesSessionId = replacesSessionId
        )
    )

    // ----- attendance -----
}
