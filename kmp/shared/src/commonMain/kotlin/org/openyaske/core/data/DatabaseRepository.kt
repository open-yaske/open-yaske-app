package org.openyaske.core.data

import org.openyaske.core.db.Attendance_records
import org.openyaske.core.db.Course_grades
import org.openyaske.core.db.Course_sessions
import org.openyaske.core.db.Course_tasks
import org.openyaske.core.db.Courses
import org.openyaske.core.db.External_sync_links
import org.openyaske.core.db.Gpa_scenarios
import org.openyaske.core.db.OpenYaskeDatabase
import org.openyaske.core.db.School_events
import org.openyaske.core.db.Terms
import org.openyaske.core.model.AttendanceRecord
import org.openyaske.core.model.AttendanceStatus
import org.openyaske.core.model.CalendarEvent
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseGrade
import org.openyaske.core.model.CourseKind
import org.openyaske.core.model.CourseSession
import org.openyaske.core.model.CourseSource
import org.openyaske.core.model.CourseTask
import org.openyaske.core.model.CourseType
import org.openyaske.core.model.CreditSystem
import org.openyaske.core.model.ExternalSyncLink
import org.openyaske.core.model.GpaGrade
import org.openyaske.core.model.GpaScenario
import org.openyaske.core.model.GradeState
import org.openyaske.core.model.SchoolEvent
import org.openyaske.core.model.SchoolEventKind
import org.openyaske.core.model.RepeatPattern
import org.openyaske.core.model.SchoolEventSource
import org.openyaske.core.model.Semester
import org.openyaske.core.model.SessionKind
import org.openyaske.core.model.SessionStatus
import org.openyaske.core.model.SyncDirection
import org.openyaske.core.model.SyncLocalType
import org.openyaske.core.model.SyncProvider
import org.openyaske.core.model.TaskKind
import org.openyaske.core.model.TaskStatus
import org.openyaske.core.model.Term
import org.openyaske.core.model.TermKind
import org.openyaske.core.model.TodoPriority
import org.openyaske.core.model.UserData
import org.openyaske.core.model.Weekday

/**
 * SQLDelight の DB とドメインモデルの変換。
 *
 * 書き込みは insert + update の組み合わせで行う。SQLite の REPLACE は
 * 親行を消して入れ直すため、外部キーの CASCADE で子行まで消える。
 * 親テーブル（学期・科目・授業回・シナリオ・テンプレート）は
 * INSERT OR IGNORE と UPDATE を同じトランザクションで実行する。
 */
class DatabaseRepository(private val database: OpenYaskeDatabase) : CoreRepository {

    override fun loadAll(): UserData = database.transactionWithResult {
        val scenarioOverrides = database.gradesQueries.selectAllGpaScenarioOverrides()
            .executeAsList()
            .groupBy { it.scenario_id }

        UserData(
            schemaVersion = UserData.CURRENT_SCHEMA_VERSION,
            terms = database.coreQueries.selectTerms().executeAsList().map { it.toModel() },
            courses = database.coreQueries.selectAllCourses().executeAsList().map { it.toModel() },
            sessions = database.sessionsQueries.selectAllSessions().executeAsList().map { it.toModel() },
            schoolEvents = database.plannerQueries.selectAllSchoolEvents().executeAsList().map { it.toModel() },
            courseTasks = database.plannerQueries.selectAllCourseTasks().executeAsList().map { it.toModel() },
            attendance = database.sessionsQueries.selectAllAttendance().executeAsList().map { it.toModel() },
            courseGrades = database.gradesQueries.selectCourseGrades().executeAsList().map { it.toModel() },
            gpaScenarios = database.gradesQueries.selectGpaScenarios().executeAsList().map { row ->
                row.toModel(scenarioOverrides[row.id].orEmpty())
            },
            syncLinks = database.syncQueries.selectSyncLinks().executeAsList().map { it.toModel() }
        )
    }

    override fun loadSettingsJson(): String? =
        database.settingsQueries.selectSettings().executeAsOneOrNull()?.json

    override fun saveSettingsJson(json: String, schemaVersion: Int, nowMillis: Long) {
        database.settingsQueries.upsertSettings(schemaVersion.toLong(), json, nowMillis)
    }

    override fun saveTerm(term: Term) {
        database.transaction {
            database.coreQueries.insertTerm(
                term.id, term.year.toLong(), enumName(term.kind),
                term.season?.let(::enumName), term.startDate, term.endDate, term.label
            )
            database.coreQueries.updateTerm(
                term.year.toLong(), enumName(term.kind), term.season?.let(::enumName),
                term.startDate, term.endDate, term.label, term.id
            )
            check(database.coreQueries.selectTermById(term.id).executeAsOneOrNull() != null) {
                "term could not be saved: ${term.id}"
            }
        }
    }

    override fun deleteTerm(id: String) {
        database.coreQueries.deleteTerm(id)
    }

    override fun saveCourse(course: Course) {
        database.transaction {
            database.coreQueries.insertCourse(
                id = course.id,
                title = course.name,
                code = course.code,
                instructor = course.instructor,
                room = course.room,
                url = course.url,
                memo = course.memo,
                course_type = enumName(course.type),
                kind = enumName(course.kind),
                source = enumName(course.source),
                institution = course.institution,
                credits = course.credits.toLong(),
                credit_system = enumName(course.creditSystem),
                counts_toward_gpa = bool(course.countsTowardGpa),
                weekday = course.weekday?.isoNumber?.toLong(),
                period = course.period?.toLong(),
                period_count = course.periodCount.toLong(),
                color = course.color,
                syllabus_id = course.syllabusId,
                term_id = course.termId,
                academic_year_id = course.academicYearId,
                created_at_ms = course.createdAtMillis,
                updated_at_ms = course.updatedAtMillis,
                deleted_at_ms = course.deletedAtMillis
            )
            database.coreQueries.updateCourse(
                title = course.name,
                code = course.code,
                instructor = course.instructor,
                room = course.room,
                url = course.url,
                memo = course.memo,
                course_type = enumName(course.type),
                kind = enumName(course.kind),
                source = enumName(course.source),
                institution = course.institution,
                credits = course.credits.toLong(),
                credit_system = enumName(course.creditSystem),
                counts_toward_gpa = bool(course.countsTowardGpa),
                weekday = course.weekday?.isoNumber?.toLong(),
                period = course.period?.toLong(),
                period_count = course.periodCount.toLong(),
                color = course.color,
                syllabus_id = course.syllabusId,
                term_id = course.termId,
                academic_year_id = course.academicYearId,
                updated_at_ms = course.updatedAtMillis,
                deleted_at_ms = course.deletedAtMillis,
                id = course.id
            )
            check(database.coreQueries.selectCourseById(course.id).executeAsOneOrNull() != null) {
                "course could not be saved: ${course.id}"
            }
        }
    }

    override fun saveSession(session: CourseSession) {
        database.transaction {
            database.sessionsQueries.insertSession(
                session.id, session.courseId, session.date, session.startPeriod?.toLong(),
                session.periodCount.toLong(), session.startTime, session.endTime, session.room,
                enumName(session.kind), enumName(session.status), session.note,
                session.replacesSessionId, session.createdAtMillis, session.updatedAtMillis,
                session.deletedAtMillis
            )
            database.sessionsQueries.updateSession(
                session.courseId, session.date, session.startPeriod?.toLong(),
                session.periodCount.toLong(), session.startTime, session.endTime, session.room,
                enumName(session.kind), enumName(session.status), session.note,
                session.replacesSessionId, session.updatedAtMillis,
                session.deletedAtMillis, session.id
            )
            check(database.sessionsQueries.selectSessionById(session.id).executeAsOneOrNull() != null) {
                "session could not be saved: ${session.id}"
            }
        }
    }

    override fun saveAttendance(record: AttendanceRecord) {
        database.sessionsQueries.upsertAttendance(
            record.id, record.courseId, record.sessionId, record.date,
            enumName(record.status), record.recordedAtMillis, record.deletedAtMillis
        )
    }

    override fun saveSchoolEvent(event: SchoolEvent) {
        database.plannerQueries.upsertSchoolEvent(
            event.id, event.title, enumName(event.kind), event.date, event.startTime,
            event.endTime, bool(event.allDay), event.location, event.courseId, event.sessionId,
            event.note, enumName(event.source), event.createdAtMillis, event.updatedAtMillis,
            event.deletedAtMillis
        )
    }

    override fun saveCourseTask(task: CourseTask) {
        database.plannerQueries.upsertCourseTask(
            task.id, task.courseId, task.sessionId, enumName(task.kind), task.title,
            task.dueDate, task.dueTime, enumName(task.status), task.priority?.let(::enumName),
            task.repeat?.let(::enumName), task.notes, task.url,
            task.createdAtMillis, task.updatedAtMillis, task.deletedAtMillis
        )
    }

    override fun saveCourseGrade(grade: CourseGrade) {
        database.gradesQueries.upsertCourseGrade(
            grade.courseId, enumName(grade.state), grade.grade?.name,
            grade.creditsEarned?.toLong(), bool(grade.countsTowardGpa), grade.note,
            grade.updatedAtMillis
        )
    }

    override fun saveGpaScenario(scenario: GpaScenario) {
        database.transaction {
            database.gradesQueries.insertGpaScenario(
                scenario.id, scenario.name, scenario.createdAtMillis
            )
            database.gradesQueries.updateGpaScenario(
                scenario.name, scenario.createdAtMillis, scenario.id
            )
            for ((courseId, grade) in scenario.overrides) {
                database.gradesQueries.upsertGpaScenarioOverride(
                    scenario.id, courseId, grade.name
                )
            }
        }
    }

    override fun saveSyncLink(link: ExternalSyncLink) {
        database.syncQueries.upsertSyncLink(
            link.id, enumName(link.localType), link.localId, enumName(link.provider),
            link.externalId, link.containerId, enumName(link.direction),
            link.lastSyncedAtMillis, link.deletedAtMillis
        )
    }

    override fun deleteGrade(courseId: String) {
        database.gradesQueries.deleteCourseGrade(courseId)
    }

    override fun deleteScenario(scenarioId: String) {
        database.gradesQueries.deleteGpaScenario(scenarioId)
    }

    /**
     * バックアップからの復元。子から順に消してから入れ直す。
     * 保存メソッドは同じトランザクション内で呼ばれる（SQLDelight の入れ子トランザクション）。
     */
    override fun replaceAll(data: UserData) {
        database.transaction {
            database.syncQueries.deleteAllSyncLinks()
            database.gradesQueries.deleteAllGpaScenarioOverrides()
            database.gradesQueries.deleteAllGpaScenarios()
            database.gradesQueries.deleteAllCourseGrades()
            database.plannerQueries.deleteAllCourseTasks()
            database.plannerQueries.deleteAllSchoolEvents()
            database.sessionsQueries.deleteAllAttendance()
            database.sessionsQueries.deleteAllSessions()
            database.coreQueries.deleteAllCourses()
            database.coreQueries.deleteAllTerms()

            data.terms.forEach { saveTerm(it) }
            data.courses.forEach { saveCourse(it) }
            data.sessions.forEach { saveSession(it) }
            data.attendance.forEach { saveAttendance(it) }
            data.schoolEvents.forEach { saveSchoolEvent(it) }
            data.courseTasks.forEach { saveCourseTask(it) }
            data.courseGrades.forEach { saveCourseGrade(it) }
            data.gpaScenarios.forEach { saveGpaScenario(it) }
            data.syncLinks.forEach { saveSyncLink(it) }
        }
    }

    override fun softDeleteCourse(id: String, nowMillis: Long) {
        database.coreQueries.softDeleteCourse(nowMillis, nowMillis, id)
    }

    override fun softDeleteSession(id: String, nowMillis: Long) {
        database.sessionsQueries.softDeleteSession(nowMillis, nowMillis, id)
    }

    override fun softDeleteAttendance(id: String, nowMillis: Long) {
        database.sessionsQueries.softDeleteAttendance(nowMillis, id)
    }

    override fun softDeleteSchoolEvent(id: String, nowMillis: Long) {
        database.plannerQueries.softDeleteSchoolEvent(nowMillis, nowMillis, id)
    }

    override fun softDeleteCourseTask(id: String, nowMillis: Long) {
        database.plannerQueries.softDeleteCourseTask(nowMillis, nowMillis, id)
    }

    override fun softDeleteSyncLink(id: String, nowMillis: Long) {
        database.syncQueries.softDeleteSyncLink(nowMillis, nowMillis, id)
    }

    // ----- mapping -----

    private fun Terms.toModel() = Term(
        id = id,
        year = year.toInt(),
        kind = enumValue(kind),
        season = season?.let { enumValue<Semester>(it) },
        startDate = start_date,
        endDate = end_date,
        label = label
    )

    private fun Courses.toModel() = Course(
        id = id,
        name = title,
        code = code,
        instructor = instructor,
        room = room,
        url = url,
        memo = memo,
        type = enumValue(course_type),
        kind = enumValue(kind),
        source = enumValue(source),
        institution = institution,
        credits = credits.toInt(),
        creditSystem = enumValue(credit_system),
        countsTowardGpa = counts_toward_gpa != 0L,
        weekday = weekday?.toInt()?.let(Weekday::fromIsoNumber),
        period = period?.toInt(),
        periodCount = period_count.toInt(),
        color = color,
        syllabusId = syllabus_id,
        academicYearId = academic_year_id,
        termId = term_id,
        createdAtMillis = created_at_ms,
        updatedAtMillis = updated_at_ms,
        deletedAtMillis = deleted_at_ms
    )

    private fun Course_sessions.toModel() = CourseSession(
        id = id,
        courseId = course_id,
        date = date,
        startPeriod = start_period?.toInt(),
        periodCount = period_count.toInt(),
        startTime = start_time,
        endTime = end_time,
        room = room,
        kind = enumValue(kind),
        status = enumValue(status),
        note = note,
        replacesSessionId = replaces_session_id,
        createdAtMillis = created_at_ms,
        updatedAtMillis = updated_at_ms,
        deletedAtMillis = deleted_at_ms
    )

    private fun Attendance_records.toModel() = AttendanceRecord(
        id = id,
        courseId = course_id,
        sessionId = session_id,
        date = date,
        status = enumValue(status),
        recordedAtMillis = recorded_at_ms,
        deletedAtMillis = deleted_at_ms
    )

    private fun School_events.toModel() = SchoolEvent(
        id = id,
        title = title,
        kind = enumValue(kind),
        date = date,
        startTime = start_time,
        endTime = end_time,
        allDay = all_day != 0L,
        location = location,
        courseId = course_id,
        sessionId = session_id,
        note = note,
        source = enumValue(source),
        createdAtMillis = created_at_ms,
        updatedAtMillis = updated_at_ms,
        deletedAtMillis = deleted_at_ms
    )

    private fun Course_tasks.toModel() = CourseTask(
        id = id,
        courseId = course_id,
        sessionId = session_id,
        kind = enumValue(kind),
        title = title,
        dueDate = due_date,
        dueTime = due_time,
        status = enumValue(status),
        priority = priority?.let { enumValue<TodoPriority>(it) },
        repeat = repeat_pattern?.let { enumValue<RepeatPattern>(it) },
        notes = notes,
        url = url,
        deletedAtMillis = deleted_at_ms,
        createdAtMillis = created_at_ms,
        updatedAtMillis = updated_at_ms
    )

    private fun Course_grades.toModel() = CourseGrade(
        courseId = course_id,
        state = enumValue(state),
        grade = grade?.let { enumValue<GpaGrade>(it) },
        creditsEarned = credits_earned?.toInt(),
        countsTowardGpa = counts_toward_gpa != 0L,
        note = note,
        updatedAtMillis = updated_at_ms
    )

    private fun Gpa_scenarios.toModel(overrides: List<org.openyaske.core.db.Gpa_scenario_overrides>) =
        GpaScenario(
            id = id,
            name = name,
            overrides = overrides.associate { it.course_id to enumValue<GpaGrade>(it.grade) },
            createdAtMillis = created_at_ms
        )

    private fun External_sync_links.toModel() = ExternalSyncLink(
        id = id,
        localType = enumValue(local_type),
        localId = local_id,
        provider = enumValue(provider),
        externalId = external_id,
        containerId = container_id,
        direction = enumValue(direction),
        lastSyncedAtMillis = last_synced_at_ms,
        deletedAtMillis = deleted_at_ms
    )

    private inline fun <reified T : Enum<T>> enumValue(value: String): T =
        enumValues<T>().firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: error("unknown value '$value' for enum")

    private fun enumName(value: Enum<*>): String = value.name.lowercase()

    private fun bool(value: Boolean): Long = if (value) 1L else 0L
}
