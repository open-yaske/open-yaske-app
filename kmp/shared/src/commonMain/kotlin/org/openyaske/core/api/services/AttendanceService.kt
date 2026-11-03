package org.openyaske.core.api.services

import org.openyaske.core.api.CoreException
import org.openyaske.core.api.AttendanceScreenState
import org.openyaske.core.model.*
import org.openyaske.core.validation.IsoDate
import org.openyaske.core.logic.*

class AttendanceService internal constructor(private val store: CoreStore) {

    fun setAttendance(
        courseId: String,
        status: AttendanceStatus,
        sessionId: String? = null,
        date: String? = null
    ): AttendanceRecord {
        store.requireCourse(courseId)
        val session = when {
            sessionId != null -> store.requireSession(sessionId).also {
                if (it.courseId != courseId) throw CoreException("session belongs to another course")
            }
            date != null -> store.snapshot.sessions.firstOrNull {
                it.courseId == courseId && it.date == date && it.status != SessionStatus.CANCELLED
            }
            else -> null
        }
        if (session != null && session.status == SessionStatus.CANCELLED &&
            status != AttendanceStatus.CANCELLED
        ) {
            throw CoreException("cannot record attendance for a cancelled session")
        }
        val effectiveDate = session?.date ?: date ?: throw CoreException("date is required")
        if (!IsoDate.isValid(effectiveDate)) throw CoreException("date must be YYYY-MM-DD")

        val existing = if (session != null) {
            store.snapshot.attendance.firstOrNull { it.sessionId == session.id }
        } else {
            store.snapshot.attendance.firstOrNull {
                it.sessionId == null && it.courseId == courseId && it.date == effectiveDate
            }
        }
        val record = AttendanceRecord(
            id = existing?.id ?: store.ids.newId(),
            courseId = courseId,
            sessionId = session?.id,
            date = effectiveDate,
            status = status,
            recordedAtMillis = store.clock(),
            deletedAtMillis = null
        )
        store.repository.saveAttendance(record)
        store.notifyChanged()
        return record
    }

    fun removeAttendance(courseId: String, sessionId: String? = null, date: String? = null) {
        val existing = if (sessionId != null) {
            store.snapshot.attendance.firstOrNull { it.sessionId == sessionId }
        } else {
            store.snapshot.attendance.firstOrNull {
                it.courseId == courseId && it.date == date && it.sessionId == null
            }
        } ?: return
        store.repository.softDeleteAttendance(existing.id, store.clock())
        store.notifyChanged()
    }

    fun attendance(courseId: String): AttendanceScreenState {
        val course = store.requireCourse(courseId)
        val records = store.snapshot.attendance
            .filter { it.courseId == courseId && it.deletedAtMillis == null }
            .sortedBy { it.date }
        val sessions = store.snapshot.sessions
            .filter { it.courseId == courseId && it.deletedAtMillis == null }
            .sortedWith(compareBy({ it.date }, { it.startPeriod ?: 0 }, { it.startTime ?: "" }))
        return AttendanceScreenState(
            course = course,
            summary = AttendanceCalculator.summarize(records, store.settings.value.attendancePolicy),
            records = records,
            sessions = sessions
        )
    }

    // ----- tasks / school events -----
}
