package org.openyaske.core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.openyaske.core.data.DatabaseRepository
import org.openyaske.core.db.OpenYaskeDatabase
import org.openyaske.core.model.AttendanceRecord
import org.openyaske.core.model.AttendanceStatus
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseGrade
import org.openyaske.core.model.CourseSession
import org.openyaske.core.model.CourseTask
import org.openyaske.core.model.ExternalSyncLink
import org.openyaske.core.model.GpaGrade
import org.openyaske.core.model.GpaScenario
import org.openyaske.core.model.GradeState
import org.openyaske.core.model.SchoolEvent
import org.openyaske.core.model.SchoolEventKind
import org.openyaske.core.model.Semester
import org.openyaske.core.model.SessionKind
import org.openyaske.core.model.SessionStatus
import org.openyaske.core.model.SyncDirection
import org.openyaske.core.model.SyncLocalType
import org.openyaske.core.model.SyncProvider
import org.openyaske.core.model.TaskKind
import org.openyaske.core.model.Term
import org.openyaske.core.model.TodoPriority
import org.openyaske.core.model.Weekday
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DatabaseRepositoryTest {
    private var driver: JdbcSqliteDriver? = null

    private fun newRepository(): Pair<OpenYaskeDatabase, DatabaseRepository> {
        val d = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver = d
        d.execute(null, "PRAGMA foreign_keys = ON", 0)
        OpenYaskeDatabase.Schema.create(d)
        val db = OpenYaskeDatabase(d)
        return db to DatabaseRepository(db)
    }

    @AfterTest
    fun close() {
        driver?.close()
    }

    private fun sampleTerm() = Term.semester(2026, Semester.SPRING, "2026-04-06", "2026-07-20")

    private fun sampleCourse(termId: String) = Course(
        id = "c1",
        name = "数学",
        code = "21000001",
        instructor = "山田",
        room = "筑5-109",
        type = org.openyaske.core.model.CourseType.REQUIRED,
        kind = org.openyaske.core.model.CourseKind.REGULAR,
        source = org.openyaske.core.model.CourseSource.PUBLIC_SYLLABUS,
        credits = 2,
        weekday = Weekday.MON,
        period = 1,
        color = "#123456",
        termId = termId,
        createdAtMillis = 1,
        updatedAtMillis = 1
    )

    @Test
    fun modelsRoundTripThroughTheDatabase() {
        val (_, repository) = newRepository()
        val term = sampleTerm()
        val course = sampleCourse(term.id)
        val session = CourseSession(
            id = "s1",
            courseId = "c1",
            date = "2026-04-06",
            startPeriod = 1,
            kind = SessionKind.REGULAR,
            status = SessionStatus.SCHEDULED,
            createdAtMillis = 1,
            updatedAtMillis = 1
        )
        val attendance = AttendanceRecord(
            id = "a1",
            courseId = "c1",
            sessionId = "s1",
            date = "2026-04-06",
            status = AttendanceStatus.PRESENT,
            recordedAtMillis = 1
        )
        val event = SchoolEvent(
            id = "e1",
            title = "健康診断",
            kind = SchoolEventKind.ACADEMIC,
            date = "2026-04-06"
        )
        val task = CourseTask(
            id = "t1",
            courseId = "c1",
            sessionId = "s1",
            kind = TaskKind.REPORT,
            title = "レポート",
            dueDate = "2026-04-10",
            dueTime = "23:59",
            priority = TodoPriority.HIGH
        )
        val grade = CourseGrade("c1", GradeState.GRADED, GpaGrade.S)
        val scenario = GpaScenario("g1", "S 想定", mapOf("c1" to GpaGrade.S), 1)
        val link = ExternalSyncLink(
            id = "l1",
            localType = SyncLocalType.TASK,
            localId = "t1",
            provider = SyncProvider.CALENDAR,
            externalId = "ext-1",
            direction = SyncDirection.TO_DEVICE,
            lastSyncedAtMillis = 1
        )

        repository.saveTerm(term)
        repository.saveCourse(course)
        repository.saveSession(session)
        repository.saveAttendance(attendance)
        repository.saveSchoolEvent(event)
        repository.saveCourseTask(task)
        repository.saveCourseGrade(grade)
        repository.saveGpaScenario(scenario)
        repository.saveSyncLink(link)

        val loaded = repository.loadAll()

        assertEquals(term, loaded.terms.single())
        assertEquals(course, loaded.courses.single())
        assertEquals(session, loaded.sessions.single())
        assertEquals(attendance, loaded.attendance.single())
        assertEquals(event, loaded.schoolEvents.single())
        assertEquals(task, loaded.courseTasks.single())
        assertEquals(grade, loaded.courseGrades.single())
        assertEquals(scenario, loaded.gpaScenarios.single())
        assertEquals(link, loaded.syncLinks.single())
    }

    @Test
    fun updatingCourseDoesNotCascadeDeleteSessions() {
        val (_, repository) = newRepository()
        val term = sampleTerm()
        val course = sampleCourse(term.id)
        repository.saveTerm(term)
        repository.saveCourse(course)
        repository.saveSession(
            CourseSession(id = "s1", courseId = "c1", date = "2026-04-06", startPeriod = 1)
        )

        repository.saveCourse(course.copy(name = "数学 II", updatedAtMillis = 2))

        val loaded = repository.loadAll()
        assertEquals("数学 II", loaded.courses.single().name)
        assertEquals(1, loaded.sessions.size)
    }

    @Test
    fun softDeleteKeepsTheTombstoneButHidesTheCourseFromUiQueries() {
        val (db, repository) = newRepository()
        val term = sampleTerm()
        repository.saveTerm(term)
        repository.saveCourse(sampleCourse(term.id))

        repository.softDeleteCourse("c1", 5)

        assertEquals(5, repository.loadAll().courses.single().deletedAtMillis)
        assertTrue(db.coreQueries.selectCourses().executeAsList().isEmpty())
    }

    @Test
    fun settingsJsonRoundTrips() {
        val (_, repository) = newRepository()
        repository.saveSettingsJson("{\"theme\":\"dark\"}", 2, 10)

        assertEquals("{\"theme\":\"dark\"}", repository.loadSettingsJson())
    }
}
