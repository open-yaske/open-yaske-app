package org.openyaske.core.api.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import org.openyaske.core.api.AppSettings
import org.openyaske.core.api.CoreException
import org.openyaske.core.api.IdGenerator
import org.openyaske.core.api.SyllabusCatalog
import org.openyaske.core.data.CoreRepository
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseSession
import org.openyaske.core.model.UserData

/**
 * 機能サービスが共有する最小限の土台。
 *
 * リポジトリ、時計、ID、現在のスナップショット、変更通知だけを持つ。
 * 業務ルールは各サービスに置き、ここには置かない。
 */
internal class CoreStore(
    val repository: CoreRepository,
    val clock: () -> Long,
    val ids: IdGenerator,
    val json: Json,
    val settings: MutableStateFlow<AppSettings>,
    val catalog: SyllabusCatalog,
    val syllabusCount: MutableStateFlow<Int>,
    private val stateProvider: () -> UserData,
    private val changed: () -> Unit
) {
    val snapshot: UserData get() = stateProvider()

    fun notifyChanged() = changed()

    fun requireCourse(courseId: String): Course =
        snapshot.courses.firstOrNull { it.id == courseId && it.deletedAtMillis == null }
            ?: throw CoreException("course not found: $courseId")

    fun requireSession(sessionId: String): CourseSession =
        snapshot.sessions.firstOrNull { it.id == sessionId && it.deletedAtMillis == null }
            ?: throw CoreException("session not found: $sessionId")

    fun normalizeCourse(course: Course): Course {
        val name = course.name.trim()
        if (name.isEmpty()) throw CoreException("course name is required")
        val weekday = course.weekday
        val period = course.period
        if ((weekday == null) != (period == null)) {
            throw CoreException("weekday and period must be both set or both null")
        }
        if (period != null && period !in Course.MIN_PERIOD..Course.MAX_PERIOD) {
            throw CoreException("period must be 1..7")
        }
        val maxCount = period?.let {
            (Course.MAX_PERIOD - it + 1).coerceAtMost(Course.MAX_PERIOD_COUNT)
        } ?: 1
        if (course.periodCount !in 1..maxCount) {
            throw CoreException("periodCount must be 1..$maxCount")
        }
        if (course.credits !in 0..30) throw CoreException("credits must be 0..30")
        val termId = course.termId?.let { id ->
            if (snapshot.terms.none { it.id == id }) throw CoreException("term not found: $id")
            id
        }
        return course.copy(
            name = name,
            code = course.code?.trim()?.ifBlank { null },
            instructor = course.instructor?.trim()?.ifBlank { null },
            room = course.room?.trim()?.ifBlank { null },
            url = course.url?.trim()?.ifBlank { null },
            memo = course.memo?.trim()?.ifBlank { null },
            institution = course.institution?.trim()?.ifBlank { null },
            period = period,
            periodCount = if (period == null) 1 else course.periodCount,
            termId = termId,
            color = course.color.ifBlank { Course.DEFAULT_COLOR }
        )
    }
}
