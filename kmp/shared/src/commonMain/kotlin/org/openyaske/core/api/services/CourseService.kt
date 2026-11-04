package org.openyaske.core.api.services

import org.openyaske.core.api.CoreException
import org.openyaske.core.data.CoreRepository
import org.openyaske.core.model.*
import org.openyaske.core.validation.IsoDate
import org.openyaske.core.logic.AcademicCalendar
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
        // 学期の範囲が変わったら、その学期の科目の授業回を作り直す。
        // スナップショットの反映を待たずに済むよう、DB から読み直した値を使う。
        val current = store.repository.loadAll()
        current.courses
            .filter { it.termId == prepared.id && it.deletedAtMillis == null }
            .forEach { regenerateSessions(it, prepared, current.sessions) }
        store.notifyChanged()
        return prepared
    }

    /**
     * 既定の学期を選ぶ。
     *
     * (a) 今日を含む学期 → (b) 今日以降で最も近い学期 → (c) その学年の前学期。
     * 「開始日がいちばん新しい学期」を選ぶと、11 月に前期のつもりの科目が
     * 後学期へ入ってしまうため、この順に変えた。
     */
    internal fun resolveTerm(today: String, terms: List<Term>): Term? {
        terms.firstOrNull { today >= it.startDate && today <= it.endDate }?.let { return it }
        terms.filter { it.startDate > today }.minByOrNull { it.startDate }?.let { return it }
        val academicYear = AcademicCalendar.academicYearOf(today)
        terms.firstOrNull { it.year == academicYear && it.season == Semester.SPRING }
            ?.let { return it }
        // 2027〜2050 は暫定日程、それ以外の年度は日付を作らず学期画面で入力を促す。
        return AcademicCalendar.term(academicYear, Semester.SPRING)
    }

    /** 既定の学期の ID。無ければ作って保存する。 */
    fun resolveTermId(today: String): String? {
        val term = resolveTerm(today, store.snapshot.terms) ?: return null
        if (store.snapshot.terms.none { it.id == term.id }) saveTerm(term)
        return term.id
    }

    /**
     * 週次パターンから授業回を展開する。
     *
     * 既にある回は触らない（休講・振替を上書きしない）。
     * 曜日・時限を変えたときに古い回が残らないよう、生成対象外の通常回だけ論理削除する。
     */
    internal fun regenerateSessions(
        course: Course,
        termOverride: Term? = null,
        sessionsOverride: List<CourseSession>? = null
    ): Int {
        val termId = course.termId ?: return 0
        val term = termOverride ?: store.snapshot.terms.firstOrNull { it.id == termId } ?: return 0
        val generated = SessionGenerator.generate(course, term)
        val source = sessionsOverride ?: store.snapshot.sessions
        val existing = source.filter {
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
        val added = generated.filter {
            slotOf(it.date, it.startPeriod, it.startTime, it.kind) !in existingSlots
        }
        added.forEach { store.repository.saveSession(it) }
        // 呼び出し側が「何件生成したか」を検知できるように件数を返す。
        return added.size
    }

    fun saveCourse(course: Course, today: String? = null): Course {
        val existing = store.snapshot.courses.firstOrNull { it.id == course.id }
        val now = store.clock()
        // 学期が 1 つも無いまま科目だけ増えると授業回が作れない。今日から既定の学期を作る。
        val resolvedTermId = course.termId?.takeIf { it.isNotBlank() }
            ?: today?.let { date -> resolveTermId(date) }
            // today を渡さない古い呼び出しは、従来どおり最新の学期に寄せる。
            ?: store.snapshot.terms.maxByOrNull { it.startDate }?.id
        val prepared = store.normalizeCourse(
            course.copy(
                termId = resolvedTermId,
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

    /** 画面やテストから、生成件数を確かめられるようにする。 */
    fun rebuildSessions(courseId: String): Int {
        val course = store.requireCourse(courseId)
        val count = regenerateSessions(course)
        store.notifyChanged()
        return count
    }

    /** 学期を削除する。その学期の科目が残っているときは拒否する。 */
    fun deleteTerm(termId: String) {
        val used = store.snapshot.courses.any { it.termId == termId && it.deletedAtMillis == null }
        if (used) throw CoreException("term is in use by courses")
        store.repository.deleteTerm(termId)
        store.notifyChanged()
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
