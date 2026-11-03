package org.openyaske.core.model

import kotlinx.serialization.Serializable

/**
 * 同期・バックアップの単位。
 *
 * `schemaVersion` を持たせ、未知フィールドは読み飛ばす（前方互換）。
 * v2 で授業回・学事・課題・成績・外部同期リンクを追加した。
 */
@Serializable
data class UserData(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val terms: List<Term> = emptyList(),
    val courses: List<Course> = emptyList(),
    val sessions: List<CourseSession> = emptyList(),
    val todos: List<Todo> = emptyList(),
    val todoLists: List<TodoList> = emptyList(),
    val events: List<CalendarEvent> = emptyList(),
    val schoolEvents: List<SchoolEvent> = emptyList(),
    val courseTasks: List<CourseTask> = emptyList(),
    val attendance: List<AttendanceRecord> = emptyList(),
    val courseGrades: List<CourseGrade> = emptyList(),
    val gpaScenarios: List<GpaScenario> = emptyList(),
    val gpaEntries: List<GpaEntry> = emptyList(),
    val academicYears: List<AcademicYear> = emptyList(),
    val syncLinks: List<ExternalSyncLink> = emptyList()
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 2
    }
}
