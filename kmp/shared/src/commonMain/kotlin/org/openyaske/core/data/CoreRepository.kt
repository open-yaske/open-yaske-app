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
 * 画面 API が依存する保存層。
 *
 * SQLDelight 実装と、Wasm 用のインメモリ / localStorage 実装を差し替えられる。
 */
interface CoreRepository {
    fun loadAll(): UserData

    fun loadSettingsJson(): String?
    fun saveSettingsJson(json: String, schemaVersion: Int, nowMillis: Long)

    fun saveTerm(term: Term)

    fun deleteTerm(id: String)
    fun saveCourse(course: Course)
    fun saveSession(session: CourseSession)
    fun saveAttendance(record: AttendanceRecord)
    fun saveSchoolEvent(event: SchoolEvent)
    fun saveCourseTask(task: CourseTask)
    fun saveCourseGrade(grade: CourseGrade)
    fun saveGpaScenario(scenario: GpaScenario)
    fun saveSyncLink(link: ExternalSyncLink)

    fun deleteGrade(courseId: String)
    fun deleteScenario(scenarioId: String)

    fun replaceAll(data: UserData)

    fun softDeleteCourse(id: String, nowMillis: Long)
    fun softDeleteSession(id: String, nowMillis: Long)
    fun softDeleteAttendance(id: String, nowMillis: Long)
    fun softDeleteSchoolEvent(id: String, nowMillis: Long)
    fun softDeleteCourseTask(id: String, nowMillis: Long)
    fun softDeleteSyncLink(id: String, nowMillis: Long)
}
