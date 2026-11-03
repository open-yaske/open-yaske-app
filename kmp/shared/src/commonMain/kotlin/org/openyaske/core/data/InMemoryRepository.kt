package org.openyaske.core.data

import org.openyaske.core.model.AttendanceRecord
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseGrade
import org.openyaske.core.model.CourseSession
import org.openyaske.core.model.CourseTask
import org.openyaske.core.model.ExternalSyncLink
import org.openyaske.core.model.GpaScenario
import org.openyaske.core.model.SchoolEvent
import org.openyaske.core.model.Term
import org.openyaske.core.model.UserData

/**
 * 画面 API 用のインメモリ保存層。
 *
 * Wasm では localStorage、テストではそのまま使う。
 * `onChange` を渡すと、変更のたびに現在のスナップショットを永続化できる。
 */
class InMemoryRepository(
    initial: UserData = UserData(),
    private var settingsJson: String? = null,
    private val onChange: (UserData) -> Unit = {},
    private val onSettingsChange: (String) -> Unit = {}
) : CoreRepository {
    private var data: UserData = initial

    override fun loadAll(): UserData = data

    override fun loadSettingsJson(): String? = settingsJson

    override fun saveSettingsJson(json: String, schemaVersion: Int, nowMillis: Long) {
        settingsJson = json
        onSettingsChange(json)
    }

    override fun saveTerm(term: Term) = update { current ->
        current.copy(terms = current.terms.upsertBy(term.id, term) { it.id })
    }

    override fun deleteTerm(id: String) = update { current ->
        current.copy(terms = current.terms.filterNot { it.id == id })
    }

    override fun saveCourse(course: Course) = update { current ->
        current.copy(courses = current.courses.upsertBy(course.id, course) { it.id })
    }

    override fun saveSession(session: CourseSession) = update { current ->
        current.copy(sessions = current.sessions.upsertBy(session.id, session) { it.id })
    }

    override fun saveAttendance(record: AttendanceRecord) = update { current ->
        val withoutConflict = current.attendance.filterNot { existing ->
            if (record.sessionId != null) {
                existing.sessionId == record.sessionId && existing.id != record.id
            } else {
                existing.sessionId == null &&
                    existing.courseId == record.courseId &&
                    existing.date == record.date &&
                    existing.id != record.id
            }
        }
        current.copy(attendance = withoutConflict.upsertBy(record.id, record) { it.id })
    }

    override fun saveSchoolEvent(event: SchoolEvent) = update { current ->
        current.copy(schoolEvents = current.schoolEvents.upsertBy(event.id, event) { it.id })
    }

    override fun saveCourseTask(task: CourseTask) = update { current ->
        current.copy(courseTasks = current.courseTasks.upsertBy(task.id, task) { it.id })
    }

    override fun saveCourseGrade(grade: CourseGrade) = update { current ->
        current.copy(courseGrades = current.courseGrades.upsertBy(grade.courseId, grade) { it.courseId })
    }

    override fun saveGpaScenario(scenario: GpaScenario) = update { current ->
        current.copy(gpaScenarios = current.gpaScenarios.upsertBy(scenario.id, scenario) { it.id })
    }

    override fun saveSyncLink(link: ExternalSyncLink) = update { current ->
        val withoutConflict = current.syncLinks.filterNot { existing ->
            existing.id != link.id &&
                existing.provider == link.provider &&
                ((existing.localType == link.localType && existing.localId == link.localId) ||
                    existing.externalId == link.externalId)
        }
        current.copy(syncLinks = withoutConflict.upsertBy(link.id, link) { it.id })
    }

    override fun deleteGrade(courseId: String) = update { current ->
        current.copy(courseGrades = current.courseGrades.filterNot { it.courseId == courseId })
    }

    override fun deleteScenario(scenarioId: String) = update { current ->
        current.copy(gpaScenarios = current.gpaScenarios.filterNot { it.id == scenarioId })
    }

    override fun replaceAll(data: UserData) {
        this.data = data
        onChange(data)
    }

    override fun softDeleteCourse(id: String, nowMillis: Long) = update { current ->
        current.copy(courses = current.courses.map {
            if (it.id == id) it.copy(deletedAtMillis = nowMillis, updatedAtMillis = nowMillis) else it
        })
    }

    override fun softDeleteSession(id: String, nowMillis: Long) = update { current ->
        current.copy(sessions = current.sessions.map {
            if (it.id == id) it.copy(deletedAtMillis = nowMillis, updatedAtMillis = nowMillis) else it
        })
    }

    override fun softDeleteAttendance(id: String, nowMillis: Long) = update { current ->
        current.copy(attendance = current.attendance.map {
            if (it.id == id) it.copy(deletedAtMillis = nowMillis) else it
        })
    }

    override fun softDeleteSchoolEvent(id: String, nowMillis: Long) = update { current ->
        current.copy(schoolEvents = current.schoolEvents.map {
            if (it.id == id) it.copy(deletedAtMillis = nowMillis, updatedAtMillis = nowMillis) else it
        })
    }

    override fun softDeleteCourseTask(id: String, nowMillis: Long) = update { current ->
        current.copy(courseTasks = current.courseTasks.map {
            if (it.id == id) it.copy(deletedAtMillis = nowMillis, updatedAtMillis = nowMillis) else it
        })
    }

    override fun softDeleteSyncLink(id: String, nowMillis: Long) = update { current ->
        current.copy(syncLinks = current.syncLinks.map {
            if (it.id == id) it.copy(deletedAtMillis = nowMillis, lastSyncedAtMillis = nowMillis) else it
        })
    }

    private fun update(block: (UserData) -> UserData) {
        data = block(data)
        onChange(data)
    }

    private fun <T> List<T>.upsertBy(key: String, value: T, keyOf: (T) -> String): List<T> {
        val index = indexOfFirst { keyOf(it) == key }
        if (index < 0) return this + value
        return toMutableList().also { it[index] = value }
    }
}
