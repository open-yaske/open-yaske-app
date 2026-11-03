package org.openyaske.core.validation

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.openyaske.core.model.AcademicYear
import org.openyaske.core.model.AttendanceRecord
import org.openyaske.core.model.CalendarEvent
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseGrade
import org.openyaske.core.model.CourseSession
import org.openyaske.core.model.CourseTask
import org.openyaske.core.model.ExternalSyncLink
import org.openyaske.core.model.GpaEntry
import org.openyaske.core.model.GpaScenario
import org.openyaske.core.model.GradeState
import org.openyaske.core.model.SchoolEvent
import org.openyaske.core.model.SessionStatus
import org.openyaske.core.model.SubTask
import org.openyaske.core.model.SyncLocalType
import org.openyaske.core.model.TaskStatus
import org.openyaske.core.model.Term
import org.openyaske.core.model.TermKind
import org.openyaske.core.model.Todo
import org.openyaske.core.model.TodoList
import org.openyaske.core.model.UserData
import org.openyaske.core.model.Weekday

/** 正規化で落とした件数・統合した件数。 */
data class NormalizeReport(
    val dropped: Map<String, Int> = emptyMap(),
    val merged: Map<String, Int> = emptyMap()
) {
    val hasIssues: Boolean
        get() = dropped.values.any { it > 0 } || merged.values.any { it > 0 }

    val totalDropped: Int get() = dropped.values.sum()
    val totalMerged: Int get() = merged.values.sum()
}

data class NormalizeResult(
    val data: UserData,
    val report: NormalizeReport = NormalizeReport()
)

/**
 * バックアップ / 同期データの読み書き。
 *
 * 復元データは外部入力として扱う:
 * - 壊れた 1 レコードで全体を失敗させず、そのレコードだけ落とす
 * - 既知の範囲へクランプできる値は補正する
 * - 同一キーは新しい方を残して統合する
 * - 科目を消したのに授業回・課題・出席が残る孤児は落とす
 */
object UserDataCodec {
    private const val MAX_CREDITS = 30
    private val TIME = Regex("""^([01]\d|2[0-3]):[0-5]\d$""")

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun encode(data: UserData): String = json.encodeToString(UserData.serializer(), data)

    /** JSON 文字列を読み、正規化まで済ませて返す。壊れた JSON は空データとして扱う。 */
    fun decode(raw: String): NormalizeResult {
        val root = runCatching { json.parseToJsonElement(raw) }.getOrNull()
        if (root !is JsonObject) {
            return NormalizeResult(
                data = UserData(),
                report = NormalizeReport(dropped = mapOf("root" to 1))
            )
        }

        val dropped = mutableMapOf<String, Int>()
        val decoded = UserData(
            schemaVersion = (root["schemaVersion"] as? JsonPrimitive)?.content?.toIntOrNull()
                ?: UserData.CURRENT_SCHEMA_VERSION,
            terms = decodeList(root, "terms", Term.serializer(), dropped),
            courses = decodeList(root, "courses", Course.serializer(), dropped),
            sessions = decodeList(root, "sessions", CourseSession.serializer(), dropped),
            todos = decodeList(root, "todos", Todo.serializer(), dropped),
            todoLists = decodeList(root, "todoLists", TodoList.serializer(), dropped),
            events = decodeList(root, "events", CalendarEvent.serializer(), dropped),
            schoolEvents = decodeList(root, "schoolEvents", SchoolEvent.serializer(), dropped),
            courseTasks = decodeList(root, "courseTasks", CourseTask.serializer(), dropped),
            attendance = decodeList(root, "attendance", AttendanceRecord.serializer(), dropped),
            courseGrades = decodeList(root, "courseGrades", CourseGrade.serializer(), dropped),
            gpaScenarios = decodeList(root, "gpaScenarios", GpaScenario.serializer(), dropped),
            gpaEntries = decodeList(root, "gpaEntries", GpaEntry.serializer(), dropped),
            academicYears = decodeList(root, "academicYears", AcademicYear.serializer(), dropped),
            syncLinks = decodeList(root, "syncLinks", ExternalSyncLink.serializer(), dropped)
        )

        val normalized = normalize(decoded)
        return normalized.copy(
            report = NormalizeReport(
                dropped = mergeCounts(dropped, normalized.report.dropped),
                merged = normalized.report.merged
            )
        )
    }

    /** すでにデコード済みのデータを正規化する。 */
    fun normalize(data: UserData, nowMillis: Long = 0L): NormalizeResult {
        val dropped = mutableMapOf<String, Int>()
        val merged = mutableMapOf<String, Int>()
        var generatedId = 0

        fun nextId(): String = "restored-$nowMillis-${generatedId++}"
        fun drop(name: String) {
            dropped[name] = (dropped[name] ?: 0) + 1
        }
        fun merge(name: String, count: Int) {
            if (count > 0) merged[name] = (merged[name] ?: 0) + count
        }

        val terms = data.terms.mapNotNull { term ->
            if (term.year !in 1..9999 ||
                !IsoDate.isValid(term.startDate) ||
                !IsoDate.isValid(term.endDate) ||
                IsoDate.isAfter(term.startDate, term.endDate)
            ) {
                drop("terms")
                return@mapNotNull null
            }
            val derivedId = if (term.kind == TermKind.SEMESTER && term.season != null) {
                Term.idOf(term.year, term.season)
            } else {
                term.id.ifBlank { nextId() }
            }
            term.copy(
                id = derivedId,
                startDate = term.startDate,
                endDate = term.endDate,
                label = term.label.trim()
            )
        }.let { dedupe(it, { t -> t.id }, { 0L }) }
            .also { merge("terms", it.second) }
            .first

        val termIds = terms.map { it.id }.toSet()

        val courses = data.courses.mapNotNull { course ->
            if (course.name.isBlank()) {
                drop("courses")
                return@mapNotNull null
            }
            val weekday: Weekday? = course.weekday
            val period: Int? = if (weekday == null) null else course.period?.coerceIn(
                Course.MIN_PERIOD,
                Course.MAX_PERIOD
            )
            val maxCount = period?.let { (Course.MAX_PERIOD - it + 1).coerceAtMost(Course.MAX_PERIOD_COUNT) } ?: 1
            course.copy(
                id = course.id.ifBlank { nextId() },
                name = course.name.trim(),
                code = course.code?.trim()?.ifBlank { null },
                instructor = course.instructor?.trim()?.ifBlank { null },
                room = course.room?.trim()?.ifBlank { null },
                url = course.url?.trim()?.ifBlank { null },
                memo = course.memo?.trim()?.ifBlank { null },
                institution = course.institution?.trim()?.ifBlank { null },
                credits = course.credits.coerceIn(0, MAX_CREDITS),
                weekday = if (period == null) null else weekday,
                period = period,
                periodCount = if (period == null) 1 else course.periodCount.coerceIn(1, maxCount),
                color = course.color.ifBlank { Course.DEFAULT_COLOR },
                termId = course.termId?.trim()?.ifBlank { null }?.takeIf { it in termIds },
                academicYearId = course.academicYearId?.trim()?.ifBlank { null },
                createdAtMillis = course.createdAtMillis.coerceAtLeast(0L),
                updatedAtMillis = course.updatedAtMillis.coerceAtLeast(0L),
                deletedAtMillis = course.deletedAtMillis?.takeIf { it >= 0L }
            )
        }.let { dedupe(it, { c -> c.id }, { c -> c.updatedAtMillis }) }
            .also { merge("courses", it.second) }
            .first

        val courseIds = courses.map { it.id }.toSet()

        val sessions = data.sessions.mapNotNull { session ->
            if (session.courseId !in courseIds || !IsoDate.isValid(session.date)) {
                drop("sessions")
                return@mapNotNull null
            }
            if (session.startTime != null && !TIME.matches(session.startTime)) {
                drop("sessions")
                return@mapNotNull null
            }
            if (session.endTime != null && !TIME.matches(session.endTime)) {
                drop("sessions")
                return@mapNotNull null
            }
            val startPeriod = session.startPeriod?.takeIf { it in Course.MIN_PERIOD..Course.MAX_PERIOD }
            if (startPeriod == null && session.startTime == null) {
                drop("sessions")
                return@mapNotNull null
            }
            session.copy(
                id = session.id.ifBlank { nextId() },
                startPeriod = startPeriod,
                periodCount = if (startPeriod == null) 1 else session.periodCount.coerceIn(1, Course.MAX_PERIOD_COUNT),
                room = session.room?.trim()?.ifBlank { null },
                note = session.note?.trim()?.ifBlank { null },
                replacesSessionId = session.replacesSessionId?.trim()?.ifBlank { null },
                createdAtMillis = session.createdAtMillis.coerceAtLeast(0L),
                updatedAtMillis = session.updatedAtMillis.coerceAtLeast(0L),
                deletedAtMillis = session.deletedAtMillis?.takeIf { it >= 0L }
            )
        }.let { dedupe(it, { s -> s.id }, { s -> s.updatedAtMillis }) }
            .also { merge("sessions", it.second) }
            .first

        val sessionIds = sessions.map { it.id }.toSet()

        val todos = data.todos.mapNotNull { todo ->
            if (todo.title.isBlank()) {
                drop("todos")
                return@mapNotNull null
            }
            val subtasks = dedupe(
                todo.subtasks.filter { it.title.isNotBlank() }.map {
                    it.copy(id = it.id.ifBlank { nextId() }, title = it.title.trim())
                },
                { it.id },
                { 0L }
            ).first
            todo.copy(
                id = todo.id.ifBlank { nextId() },
                title = todo.title.trim(),
                courseId = todo.courseId?.trim()?.ifBlank { null },
                listId = todo.listId?.trim()?.ifBlank { null },
                details = todo.details?.trim()?.ifBlank { null },
                dueAtMillis = todo.dueAtMillis?.takeIf { it >= 0L },
                notifyMinutesBefore = todo.notifyMinutesBefore?.coerceIn(0, 60 * 24 * 30),
                repeat = todo.repeat?.let { rule -> rule.copy(count = rule.count?.takeIf { it > 0 }) },
                subtasks = subtasks,
                parentTodoId = todo.parentTodoId?.trim()?.ifBlank { null },
                trashedAtMillis = todo.trashedAtMillis?.takeIf { it >= 0L },
                createdAtMillis = todo.createdAtMillis.coerceAtLeast(0L),
                updatedAtMillis = todo.updatedAtMillis.coerceAtLeast(0L)
            )
        }.let { dedupe(it, { t -> t.id }, { t -> t.updatedAtMillis }) }
            .also { merge("todos", it.second) }
            .first

        val todoLists = data.todoLists.filter { it.name.isNotBlank() }
            .map {
                it.copy(
                    id = it.id.ifBlank { nextId() },
                    name = it.name.trim(),
                    systemKey = it.systemKey?.trim()?.ifBlank { null },
                    createdAtMillis = it.createdAtMillis.coerceAtLeast(0L)
                )
            }
            .let { dedupe(it, { list -> list.systemKey ?: list.id }, { 0L }) }
            .also { merge("todoLists", it.second) }
            .first

        val events = data.events.mapNotNull { event ->
            if (event.title.isBlank() || event.startAtMillis < 0L) {
                drop("events")
                return@mapNotNull null
            }
            event.copy(
                id = event.id.ifBlank { nextId() },
                title = event.title.trim(),
                endAtMillis = event.endAtMillis.coerceAtLeast(event.startAtMillis),
                location = event.location?.trim()?.ifBlank { null },
                memo = event.memo?.trim()?.ifBlank { null },
                calendarId = event.calendarId.ifBlank { "default" },
                color = event.color.ifBlank { Course.DEFAULT_COLOR },
                createdAtMillis = event.createdAtMillis.coerceAtLeast(0L),
                updatedAtMillis = event.updatedAtMillis.coerceAtLeast(0L)
            )
        }.let { dedupe(it, { e -> e.id }, { e -> e.updatedAtMillis }) }
            .also { merge("events", it.second) }
            .first

        val schoolEvents = data.schoolEvents.mapNotNull { event ->
            if (event.title.isBlank() || !IsoDate.isValid(event.date)) {
                drop("schoolEvents")
                return@mapNotNull null
            }
            if (event.startTime != null && !TIME.matches(event.startTime)) {
                drop("schoolEvents")
                return@mapNotNull null
            }
            if (event.endTime != null && !TIME.matches(event.endTime)) {
                drop("schoolEvents")
                return@mapNotNull null
            }
            event.copy(
                id = event.id.ifBlank { nextId() },
                title = event.title.trim(),
                startTime = event.startTime?.takeIf { it.isNotBlank() },
                endTime = event.endTime?.takeIf { it.isNotBlank() },
                courseId = event.courseId?.takeIf { it in courseIds },
                sessionId = event.sessionId?.takeIf { it in sessionIds },
                note = event.note?.trim()?.ifBlank { null },
                createdAtMillis = event.createdAtMillis.coerceAtLeast(0L),
                updatedAtMillis = event.updatedAtMillis.coerceAtLeast(0L),
                deletedAtMillis = event.deletedAtMillis?.takeIf { it >= 0L }
            )
        }.let { dedupe(it, { e -> e.id }, { e -> e.updatedAtMillis }) }
            .also { merge("schoolEvents", it.second) }
            .first

        val courseTasks = data.courseTasks.mapNotNull { task ->
            if (task.title.isBlank() || task.courseId !in courseIds) {
                drop("courseTasks")
                return@mapNotNull null
            }
            if (task.dueDate != null && !IsoDate.isValid(task.dueDate)) {
                drop("courseTasks")
                return@mapNotNull null
            }
            if (task.dueTime != null && !TIME.matches(task.dueTime)) {
                drop("courseTasks")
                return@mapNotNull null
            }
            task.copy(
                id = task.id.ifBlank { nextId() },
                title = task.title.trim(),
                sessionId = task.sessionId?.trim()?.ifBlank { null }?.takeIf { it in sessionIds },
                dueDate = task.dueDate,
                dueTime = task.dueTime?.takeIf { it.isNotBlank() },
                notes = task.notes?.trim()?.ifBlank { null },
                url = task.url?.trim()?.ifBlank { null },
                deletedAtMillis = task.deletedAtMillis?.takeIf { it >= 0L },
                createdAtMillis = task.createdAtMillis.coerceAtLeast(0L),
                updatedAtMillis = task.updatedAtMillis.coerceAtLeast(0L)
            )
        }.let { dedupe(it, { t -> t.id }, { t -> t.updatedAtMillis }) }
            .also { merge("courseTasks", it.second) }
            .first

        val attendance = data.attendance.mapNotNull { record ->
            if (record.courseId !in courseIds || !IsoDate.isValid(record.date)) {
                drop("attendance")
                return@mapNotNull null
            }
            record.copy(
                id = record.id.ifBlank { nextId() },
                courseId = record.courseId.trim(),
                sessionId = record.sessionId?.trim()?.ifBlank { null }?.takeIf { it in sessionIds },
                recordedAtMillis = record.recordedAtMillis.coerceAtLeast(0L),
                deletedAtMillis = record.deletedAtMillis?.takeIf { it >= 0L }
            )
        }.let { records ->
            dedupe(
                records,
                { r -> r.sessionId ?: "${r.courseId}\u0000${r.date}" },
                { r -> r.recordedAtMillis }
            )
        }.also { merge("attendance", it.second) }
            .first

        val courseGrades = data.courseGrades.mapNotNull { grade ->
            if (grade.courseId !in courseIds) {
                drop("courseGrades")
                return@mapNotNull null
            }
            if (grade.state == GradeState.GRADED && grade.grade == null) {
                drop("courseGrades")
                return@mapNotNull null
            }
            grade.copy(
                creditsEarned = grade.creditsEarned?.coerceIn(0, MAX_CREDITS),
                note = grade.note?.trim()?.ifBlank { null },
                updatedAtMillis = grade.updatedAtMillis.coerceAtLeast(0L)
            )
        }.let { dedupe(it, { g -> g.courseId }, { g -> g.updatedAtMillis }) }
            .also { merge("courseGrades", it.second) }
            .first

        val gpaScenarios = data.gpaScenarios.filter { it.name.isNotBlank() }
            .map { scenario ->
                scenario.copy(
                    id = scenario.id.ifBlank { nextId() },
                    name = scenario.name.trim(),
                    overrides = scenario.overrides.filterKeys { it in courseIds },
                    createdAtMillis = scenario.createdAtMillis.coerceAtLeast(0L)
                )
            }
            .let { dedupe(it, { s -> s.id }, { 0L }) }
            .also { merge("gpaScenarios", it.second) }
            .first

        val gpaEntries = data.gpaEntries.mapNotNull { entry ->
            if (entry.courseTitle.isBlank()) {
                drop("gpaEntries")
                return@mapNotNull null
            }
            entry.copy(
                id = entry.id.ifBlank { nextId() },
                courseTitle = entry.courseTitle.trim(),
                credits = entry.credits.coerceIn(0, MAX_CREDITS)
            )
        }.let { dedupe(it, { e -> e.id }, { 0L }) }
            .also { merge("gpaEntries", it.second) }
            .first

        val academicYears = data.academicYears.mapNotNull { year ->
            if (year.year !in 1..9999) {
                drop("academicYears")
                return@mapNotNull null
            }
            year.copy(
                id = AcademicYear.idOf(year.year, year.semester),
                grade = year.grade?.trim()?.ifBlank { null }
            )
        }.let { dedupe(it, { y -> y.id }, { 0L }) }
            .also { merge("academicYears", it.second) }
            .first

        val taskIds = courseTasks.map { it.id }.toSet()
        val eventIds = schoolEvents.map { it.id }.toSet()

        val syncLinks = data.syncLinks.mapNotNull { link ->
            if (link.localId.isBlank() || link.externalId.isBlank()) {
                drop("syncLinks")
                return@mapNotNull null
            }
            val exists = when (link.localType) {
                SyncLocalType.SESSION -> link.localId in sessionIds
                SyncLocalType.EVENT -> link.localId in eventIds
                SyncLocalType.TASK -> link.localId in taskIds
            }
            if (!exists) {
                drop("syncLinks")
                return@mapNotNull null
            }
            link.copy(
                id = link.id.ifBlank { nextId() },
                localId = link.localId.trim(),
                externalId = link.externalId.trim(),
                containerId = link.containerId?.trim()?.ifBlank { null },
                lastSyncedAtMillis = link.lastSyncedAtMillis.coerceAtLeast(0L),
                deletedAtMillis = link.deletedAtMillis?.takeIf { it >= 0L }
            )
        }.let { dedupe(it, { s -> "${s.provider}:${s.localType}:${s.localId}" }, { s -> s.lastSyncedAtMillis }) }
            .also { merge("syncLinks", it.second) }
            .first

        return NormalizeResult(
            data = data.copy(
                schemaVersion = UserData.CURRENT_SCHEMA_VERSION,
                terms = terms,
                courses = courses,
                sessions = sessions,
                todos = todos,
                todoLists = todoLists,
                events = events,
                schoolEvents = schoolEvents,
                courseTasks = courseTasks,
                attendance = attendance,
                courseGrades = courseGrades,
                gpaScenarios = gpaScenarios,
                gpaEntries = gpaEntries,
                academicYears = academicYears,
                syncLinks = syncLinks
            ),
            report = NormalizeReport(dropped = dropped, merged = merged)
        )
    }

    private fun <T> decodeList(
        root: JsonObject,
        key: String,
        serializer: KSerializer<T>,
        dropped: MutableMap<String, Int>
    ): List<T> {
        val array = root[key] as? JsonArray ?: return emptyList()
        val values = ArrayList<T>(array.size)
        for (element in array) {
            val decoded = runCatching { json.decodeFromJsonElement(serializer, element) }.getOrNull()
            if (decoded == null) {
                dropped[key] = (dropped[key] ?: 0) + 1
            } else {
                values += decoded
            }
        }
        return values
    }

    private fun <T> dedupe(
        items: List<T>,
        key: (T) -> String,
        updatedAt: (T) -> Long
    ): Pair<List<T>, Int> {
        val byKey = LinkedHashMap<String, T>(items.size)
        var merged = 0
        for (item in items) {
            val k = key(item)
            val existing = byKey[k]
            if (existing == null) {
                byKey[k] = item
            } else {
                merged++
                if (updatedAt(item) >= updatedAt(existing)) byKey[k] = item
            }
        }
        return byKey.values.toList() to merged
    }

    private fun mergeCounts(a: Map<String, Int>, b: Map<String, Int>): Map<String, Int> {
        val result = a.toMutableMap()
        for ((key, value) in b) result[key] = (result[key] ?: 0) + value
        return result.filterValues { it > 0 }
    }
}
