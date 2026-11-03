package org.openyaske.core.api.services

import org.openyaske.core.api.CoreException
import org.openyaske.core.data.CoreRepository
import org.openyaske.core.model.*
import org.openyaske.core.validation.IsoDate
import org.openyaske.core.logic.PeriodTimes
import org.openyaske.core.logic.SessionGenerator

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
        // 学期を後から入れた場合に備え、その学期の科目の授業回を展開する。
        store.snapshot.courses
            .filter { it.termId == prepared.id && it.deletedAtMillis == null }
            .forEach { regenerateSessions(it) }
        return prepared
    }

    /** 学期を指定しなかったときは、いちばん新しい学期にぶら下げる。 */
    private fun defaultTermId(): String? =
        store.snapshot.terms.maxByOrNull { it.startDate }?.id

    /**
     * 週次パターンから授業回を展開する。
     *
     * 既にある回は触らない（休講・振替を上書きしない）。
     * 曜日・時限を変えたときに古い回が残らないよう、生成対象外の通常回だけ論理削除する。
     */
    private fun regenerateSessions(course: Course) {
        val termId = course.termId ?: return
        val term = store.snapshot.terms.firstOrNull { it.id == termId } ?: return
        val generated = SessionGenerator.generate(course, term)
        val existing = store.snapshot.sessions.filter {
            it.courseId == course.id &&
                it.deletedAtMillis == null &&
                it.kind == SessionKind.REGULAR
        }
        fun slotOf(date: String, period: Int?, time: String?, kind: SessionKind) =
            "$date|${period ?: 0}|${time ?: ""}|$kind"
        val existingSlots = existing.map {
            slotOf(it.date, it.startPeriod, it.startTime, it.kind)
        }.toSet()
        val keep = generated.map { it.id }.toSet()
        // 曜日・時限を変えたときに古い回が残らないよう、生成対象外の通常回だけ論理削除する。
        existing.filter { it.id !in keep }.forEach {
            store.repository.softDeleteSession(it.id, store.clock())
        }
        // 同じスロットに既に回があれば、ID が違っても足さない。
        generated.filter {
            slotOf(it.date, it.startPeriod, it.startTime, it.kind) !in existingSlots
        }.forEach {
            store.repository.saveSession(it)
        }
    }

    fun saveCourse(course: Course): Course {
        val existing = store.snapshot.courses.firstOrNull { it.id == course.id }
        val now = store.clock()
        val prepared = store.normalizeCourse(
            course.copy(
                termId = course.termId?.takeIf { it.isNotBlank() } ?: defaultTermId(),
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
        regenerateSessions(prepared)
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
        // 同じスロット（科目・日付・時限・時刻・種別）の回が既にあれば、その ID で上書きする。
        val sameSlot = if (session.id.isBlank()) {
            store.snapshot.sessions.firstOrNull {
                it.deletedAtMillis == null &&
                    it.courseId == session.courseId &&
                    it.date == session.date &&
                    it.startPeriod == session.startPeriod &&
                    it.startTime == session.startTime &&
                    it.kind == session.kind
            }
        } else {
            null
        }
        val effective = if (sameSlot != null) session.copy(id = sameSlot.id) else session
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
        val existing = store.snapshot.sessions.firstOrNull { it.id == effective.id }
        val now = store.clock()
        val prepared = effective.copy(
            id = effective.id.ifBlank { store.ids.newId() },
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
