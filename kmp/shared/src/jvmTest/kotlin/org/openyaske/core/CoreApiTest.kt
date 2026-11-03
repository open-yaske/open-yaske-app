package org.openyaske.core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.CoreException
import org.openyaske.core.api.ThemeMode
import org.openyaske.core.data.DatabaseRepository
import org.openyaske.core.db.OpenYaskeDatabase
import org.openyaske.core.model.AttendanceStatus
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseGrade
import org.openyaske.core.model.CourseTask
import org.openyaske.core.model.GpaGrade
import org.openyaske.core.model.GradeState
import org.openyaske.core.model.SchoolEvent
import org.openyaske.core.model.SchoolEventKind
import org.openyaske.core.model.Semester
import org.openyaske.core.model.SyllabusEntry
import org.openyaske.core.model.TaskKind
import org.openyaske.core.model.TaskStatus
import org.openyaske.core.model.Term
import org.openyaske.core.model.Weekday
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CoreApiTest {
    private var driver: JdbcSqliteDriver? = null

    private fun newApi(syllabus: List<SyllabusEntry> = emptyList()): CoreApi {
        val d = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver = d
        d.execute(null, "PRAGMA foreign_keys = ON", 0)
        OpenYaskeDatabase.Schema.create(d)
        val database = OpenYaskeDatabase(d)
        return CoreApi(
            repository = DatabaseRepository(database),
            syllabus = syllabus
        )
    }

    @AfterTest
    fun close() {
        driver?.close()
    }

    private fun semester() = Term.semester(2026, Semester.SPRING, "2026-04-06", "2026-07-20")

    private fun course(termId: String) = Course(
        id = "",
        name = "線形代数",
        code = "21016136",
        instructor = "山田",
        credits = 2,
        weekday = Weekday.MON,
        period = 1,
        termId = termId
    )

    @Test
    fun courseAndSessionAppearInTimetable() {
        val api = newApi()
        val term = api.saveTerm(semester())
        val saved = api.saveCourse(course(term.id))
        api.saveSession(
            org.openyaske.core.model.CourseSession(
                id = "",
                courseId = saved.id,
                date = "2026-04-06",
                startPeriod = 1
            )
        )

        val timetable = api.timetable(term.id)

        assertEquals(1, timetable.cells.size)
        assertEquals(saved.id, timetable.cells.values.single().course.id)
        assertEquals(2, timetable.totalCredits)
        assertTrue(timetable.issues.isEmpty())
        assertEquals(1, api.courseDetail(saved.id).sessions.size)
    }

    @Test
    fun attendanceUpsertsPerSessionAndSummarizes() {
        val api = newApi()
        val term = api.saveTerm(semester())
        val saved = api.saveCourse(course(term.id))
        val session = api.saveSession(
            org.openyaske.core.model.CourseSession(
                id = "",
                courseId = saved.id,
                date = "2026-04-06",
                startPeriod = 1
            )
        )

        api.setAttendance(saved.id, AttendanceStatus.PRESENT, sessionId = session.id)
        api.setAttendance(saved.id, AttendanceStatus.LATE, sessionId = session.id)

        val view = api.attendance(saved.id)
        assertEquals(1, view.records.size)
        assertEquals(1, view.summary.total)
        assertEquals(1, view.summary.late)
        assertEquals(50.0, view.summary.rate)
    }

    @Test
    fun pastAttendanceWithoutSessionIsAllowed() {
        val api = newApi()
        val term = api.saveTerm(semester())
        val saved = api.saveCourse(course(term.id))

        api.setAttendance(saved.id, AttendanceStatus.ABSENT, date = "2026-04-13")

        val view = api.attendance(saved.id)
        assertEquals(1, view.records.size)
        assertEquals("2026-04-13", view.records.single().date)
        assertEquals(1, view.summary.absent)
    }

    @Test
    fun unscheduledCourseRecordsAttendanceByDate() {
        val api = newApi()
        val term = api.saveTerm(semester())
        // 時間割外（週次パターンなし）の科目。授業回は作られない。
        val saved = api.saveCourse(
            course(term.id).copy(weekday = null, period = null)
        )
        assertEquals(0, api.courseDetail(saved.id).sessions.size)

        api.setAttendance(saved.id, AttendanceStatus.PRESENT, date = "2026-11-03")
        api.setAttendance(saved.id, AttendanceStatus.ABSENT, date = "2026-11-10")

        val view = api.attendance(saved.id)
        assertEquals(2, view.records.size)
        assertTrue(view.records.all { it.sessionId == null })
        assertEquals(2, view.summary.total)
        assertEquals(1, view.summary.absent)
        assertEquals(1, view.summary.absenceLimit)
        assertTrue(view.summary.atRisk)
    }

    @Test
    fun cancelledSessionRejectsAttendance() {
        val api = newApi()
        val term = api.saveTerm(semester())
        val saved = api.saveCourse(course(term.id))
        val session = api.saveSession(
            org.openyaske.core.model.CourseSession(
                id = "",
                courseId = saved.id,
                date = "2026-04-06",
                startPeriod = 1
            )
        )
        api.cancelSession(session.id)

        assertFailsWith<CoreException> {
            api.setAttendance(saved.id, AttendanceStatus.PRESENT, sessionId = session.id)
        }
    }

    @Test
    fun tasksAndEventsAppearInAgendaAndDoneTasksDisappear() {
        val api = newApi()
        val term = api.saveTerm(semester())
        val saved = api.saveCourse(course(term.id))
        val task = api.saveTask(
            CourseTask(
                id = "",
                courseId = saved.id,
                kind = TaskKind.REPORT,
                title = "レポート",
                dueDate = "2026-04-06"
            )
        )
        api.saveSchoolEvent(
            SchoolEvent(id = "", title = "健康診断", kind = SchoolEventKind.ACADEMIC, date = "2026-04-06")
        )

        assertEquals(2, api.agenda("2026-04-06", "2026-04-06").size)

        api.setTaskStatus(task.id, TaskStatus.DONE)
        assertEquals(1, api.agenda("2026-04-06", "2026-04-06").size)
    }

    @Test
    fun gpaUsesCourseGradesAndScenario() {
        val api = newApi()
        val term = api.saveTerm(semester())
        val a = api.saveCourse(course(term.id).copy(name = "S の科目"))
        val b = api.saveCourse(course(term.id).copy(id = "", name = "A の科目", code = "21016137"))
        api.saveGrade(CourseGrade(a.id, GradeState.GRADED, GpaGrade.S))
        api.saveGrade(CourseGrade(b.id, GradeState.GRADED, GpaGrade.A))

        assertEquals(3.5, api.gpa().base.gpa)

        val scenario = api.saveScenario("A が S", mapOf(b.id to GpaGrade.S))
        assertEquals(4.0, api.gpa(scenario.id).scenarioSummary?.gpa)
    }

    @Test
    fun syllabusSearchFindsCodeAndJapaneseTitle() {
        val entries = listOf(
            SyllabusEntry(
                id = "1",
                year = 2026,
                code = "21016136",
                title = "線形代数第一",
                instructor = "山田"
            ),
            SyllabusEntry(
                id = "2",
                year = 2026,
                code = "21322109",
                title = "基盤鋼構造物のよこ振動",
                instructor = "貝野"
            )
        )
        val api = newApi(entries)

        assertEquals("21016136", api.searchSyllabus("2101").first().entry.code)
        assertEquals("線形代数第一", api.searchSyllabus("線形").first().entry.title)
        assertEquals("基盤鋼構造物のよこ振動", api.searchSyllabus("構造物").first().entry.title)
    }

    @Test
    fun settingsAreNormalizedAndPersisted() {
        val api = newApi()
        api.updateSettings {
            it.copy(
                theme = ThemeMode.DARK,
                visibleWeekdays = emptyList(),
                maxPeriod = 99,
                notifications = it.notifications.copy(leadMinutes = 9999)
            )
        }

        assertEquals(ThemeMode.DARK, api.settings.value.theme)
        assertEquals(6, api.settings.value.visibleWeekdays.size)
        assertEquals(7, api.settings.value.maxPeriod)
        assertEquals(1440, api.settings.value.notifications.leadMinutes)

        val another = CoreApi(DatabaseRepository(OpenYaskeDatabase(driver!!)))
        assertEquals(ThemeMode.DARK, another.settings.value.theme)
    }

    @Test
    fun backupRoundTripsThroughTheApi() {
        val api = newApi()
        val term = api.saveTerm(semester())
        val saved = api.saveCourse(course(term.id))
        api.saveSession(
            org.openyaske.core.model.CourseSession(
                id = "",
                courseId = saved.id,
                date = "2026-04-06",
                startPeriod = 1
            )
        )
        api.saveTask(
            CourseTask(id = "", courseId = saved.id, title = "課題", dueDate = "2026-04-10")
        )
        val json = api.exportJson()

        val restoredApi = newApi()
        restoredApi.importJson(json)

        assertEquals(1, restoredApi.state.value.terms.size)
        assertEquals(1, restoredApi.state.value.courses.size)
        assertEquals(1, restoredApi.state.value.sessions.size)
        assertEquals(1, restoredApi.state.value.courseTasks.size)
    }

    @Test
    fun invalidCourseShapeIsRejected() {
        val api = newApi()
        val term = api.saveTerm(semester())
        assertFailsWith<CoreException> {
            api.saveCourse(course(term.id).copy(weekday = null, period = 1))
        }
        assertFailsWith<CoreException> {
            api.saveCourse(course(term.id).copy(credits = 99))
        }
    }
}
