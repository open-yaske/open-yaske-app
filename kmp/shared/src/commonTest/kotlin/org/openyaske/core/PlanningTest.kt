package org.openyaske.core

import org.openyaske.core.logic.AgendaItem
import org.openyaske.core.logic.AgendaProjector
import org.openyaske.core.logic.GpaCalculator
import org.openyaske.core.logic.SessionGenerator
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
import org.openyaske.core.model.HolidayRule
import org.openyaske.core.model.SchoolEvent
import org.openyaske.core.model.SchoolEventKind
import org.openyaske.core.model.Semester
import org.openyaske.core.model.SessionOverride
import org.openyaske.core.model.SessionStatus
import org.openyaske.core.model.SyncDirection
import org.openyaske.core.model.SyncLocalType
import org.openyaske.core.model.SyncProvider
import org.openyaske.core.model.TaskKind
import org.openyaske.core.model.TaskStatus
import org.openyaske.core.model.Term
import org.openyaske.core.model.UserData
import org.openyaske.core.model.Weekday
import org.openyaske.core.validation.IsoDate
import org.openyaske.core.validation.UserDataCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IsoDateTest {
    @Test
    fun dayOfWeekUsesIsoNumbering() {
        assertEquals(1, IsoDate.dayOfWeekIso("2026-04-06"))
        assertEquals(7, IsoDate.dayOfWeekIso("2026-04-12"))
    }

    @Test
    fun plusDaysCrossesMonthAndYear() {
        assertEquals("2026-05-01", IsoDate.plusDays("2026-04-30", 1))
        assertEquals("2027-01-01", IsoDate.plusDays("2026-12-31", 1))
        assertEquals("2024-02-29", IsoDate.plusDays("2024-02-28", 1))
    }

    @Test
    fun epochDayRoundTrip() {
        for (date in listOf("1970-01-01", "2026-04-06", "2024-02-29", "2099-12-31")) {
            assertEquals(date, IsoDate.fromEpochDay(IsoDate.toEpochDay(date)))
        }
    }
}

class SessionGeneratorTest {
    private val term = Term.semester(2026, Semester.SPRING, "2026-04-06", "2026-04-26")
    private val course = Course(
        id = "c1",
        name = "数学",
        weekday = Weekday.MON,
        period = 1,
        periodCount = 1
    )

    @Test
    fun generatesWeeklySessionsAndAppliesHolidayAndCancel() {
        val sessions = SessionGenerator.generate(
            course = course,
            term = term,
            holidays = listOf(HolidayRule("2026-04-13", name = "休日")),
            overrides = listOf(SessionOverride("2026-04-20", SessionStatus.CANCELLED))
        )

        assertEquals(listOf("2026-04-06", "2026-04-20"), sessions.map { it.date })
        assertEquals(listOf(SessionStatus.SCHEDULED, SessionStatus.CANCELLED), sessions.map { it.status })
    }

    @Test
    fun partialHolidayOnlySkipsAffectedPeriods() {
        val afternoon = HolidayRule("2026-04-13", periods = listOf(3, 4))
        val morning = SessionGenerator.generate(
            course = course,
            term = term,
            holidays = listOf(afternoon)
        )
        assertEquals(3, morning.size)

        val third = course.copy(period = 3)
        val skipped = SessionGenerator.generate(course = third, term = term, holidays = listOf(afternoon))
        assertEquals(2, skipped.size)
    }

    @Test
    fun nonWeeklyCoursesAreNotGenerated() {
        val extra = course.copy(kind = org.openyaske.core.model.CourseKind.EXTRA)
        assertTrue(SessionGenerator.generate(extra, term).isEmpty())
    }
}

class AgendaProjectorTest {
    private val course = Course(id = "c1", name = "数学", weekday = Weekday.MON, period = 1)

    @Test
    fun mergesClassEventAndOpenTaskForTheDay() {
        val session = CourseSession(id = "s1", courseId = "c1", date = "2026-04-06", startPeriod = 1)
        val event = SchoolEvent(
            id = "e1",
            title = "健康診断",
            kind = SchoolEventKind.ACADEMIC,
            date = "2026-04-06"
        )
        val task = CourseTask(
            id = "t1",
            courseId = "c1",
            title = "レポート提出",
            kind = TaskKind.REPORT,
            dueDate = "2026-04-06"
        )
        val done = task.copy(id = "t2", status = TaskStatus.DONE)

        val entries = AgendaProjector.forDate(
            date = "2026-04-06",
            courses = listOf(course),
            sessions = listOf(session),
            events = listOf(event),
            tasks = listOf(task, done)
        )

        assertEquals(3, entries.size)
        assertTrue(entries.any { it.item is AgendaItem.Event })
        assertTrue(entries.any { it.item is AgendaItem.Class })
        assertEquals(1, entries.count { it.item is AgendaItem.Task })
    }

    @Test
    fun rangeExcludesOutOfRangeAndDeletedTasks() {
        val session = CourseSession(id = "s1", courseId = "c1", date = "2026-04-07", startPeriod = 1)
        val deleted = CourseTask(
            id = "t1",
            courseId = "c1",
            title = "消した課題",
            dueDate = "2026-04-06",
            deletedAtMillis = 1
        )

        val entries = AgendaProjector.range(
            "2026-04-06",
            "2026-04-06",
            listOf(course),
            listOf(session),
            emptyList(),
            listOf(deleted)
        )

        assertTrue(entries.isEmpty())
    }
}

class CourseGradeTest {
    private val courses = listOf(
        Course(id = "c1", name = "S", credits = 2),
        Course(id = "c2", name = "A", credits = 2),
        Course(id = "c3", name = "F", credits = 1),
        Course(id = "c4", name = "未確定", credits = 2),
        Course(id = "c5", name = "合否", credits = 2)
    ).associateBy { it.id }

    private val grades = listOf(
        CourseGrade("c1", GradeState.GRADED, GpaGrade.S),
        CourseGrade("c2", GradeState.GRADED, GpaGrade.A),
        CourseGrade("c3", GradeState.GRADED, GpaGrade.F),
        CourseGrade("c4", GradeState.NOT_GRADED),
        CourseGrade("c5", GradeState.PASS_FAIL)
    )

    @Test
    fun notGradedIsExcludedAndPassFailOnlyCountsCredits() {
        val summary = GpaCalculator.calculateGrades(grades, courses)

        assertEquals(5, summary.totalCredits)
        assertEquals(14.0, summary.totalPoints)
        assertEquals(2.8, summary.gpa)
        assertEquals(6, summary.earnedCredits)
    }

    @Test
    fun scenarioOverridesGradeForWhatIf() {
        val scenario = GpaScenario(
            id = "s",
            name = "A が S だったら",
            overrides = mapOf("c2" to GpaGrade.S)
        )

        val summary = GpaCalculator.calculateGrades(grades, courses, scenario)

        assertEquals(16.0, summary.totalPoints)
        assertEquals(3.2, summary.gpa)
    }

    @Test
    fun excludedCourseIsIgnored() {
        val grades = listOf(
            CourseGrade("c1", GradeState.GRADED, GpaGrade.S, countsTowardGpa = false),
            CourseGrade("c2", GradeState.EXCLUDED, GpaGrade.A)
        )
        val summary = GpaCalculator.calculateGrades(grades, courses)

        assertEquals(0, summary.totalCredits)
        assertEquals(0.0, summary.gpa)
    }
}

class UserDataV2CodecTest {
    @Test
    fun roundTripKeepsNewCollections() {
        val data = UserData(
            terms = listOf(Term.semester(2026, Semester.SPRING, "2026-04-06", "2026-07-20")),
            courses = listOf(Course(id = "c1", name = "数学", weekday = Weekday.MON, period = 1)),
            sessions = listOf(CourseSession(id = "s1", courseId = "c1", date = "2026-04-06", startPeriod = 1)),
            schoolEvents = listOf(
                SchoolEvent(id = "e1", title = "健康診断", date = "2026-04-06")
            ),
            courseTasks = listOf(
                CourseTask(id = "t1", courseId = "c1", title = "レポート", dueDate = "2026-04-10")
            ),
            attendance = listOf(
                AttendanceRecord(
                    id = "a1",
                    courseId = "c1",
                    sessionId = "s1",
                    date = "2026-04-06",
                    status = AttendanceStatus.PRESENT
                )
            ),
            courseGrades = listOf(CourseGrade("c1", GradeState.GRADED, GpaGrade.S)),
            gpaScenarios = listOf(GpaScenario("g1", "S 想定", mapOf("c1" to GpaGrade.S))),
            syncLinks = listOf(
                ExternalSyncLink(
                    id = "l1",
                    localType = SyncLocalType.TASK,
                    localId = "t1",
                    provider = SyncProvider.CALENDAR,
                    externalId = "ext-1",
                    direction = SyncDirection.TO_DEVICE
                )
            )
        )

        val restored = UserDataCodec.decode(UserDataCodec.encode(data)).data

        assertEquals(2, restored.schemaVersion)
        assertEquals(1, restored.terms.size)
        assertEquals(1, restored.sessions.size)
        assertEquals(1, restored.schoolEvents.size)
        assertEquals(1, restored.courseTasks.size)
        assertEquals("s1", restored.attendance.single().sessionId)
        assertEquals(1, restored.courseGrades.size)
        assertEquals(1, restored.gpaScenarios.size)
        assertEquals(1, restored.syncLinks.size)
    }

    @Test
    fun orphanedRowsAreDropped() {
        val result = UserDataCodec.normalize(
            UserData(
                sessions = listOf(CourseSession(id = "s1", courseId = "missing", date = "2026-04-06")),
                courseTasks = listOf(CourseTask(id = "t1", courseId = "missing", title = "迷子")),
                attendance = listOf(
                    AttendanceRecord(
                        id = "a1",
                        courseId = "missing",
                        date = "2026-04-06",
                        status = AttendanceStatus.PRESENT
                    )
                )
            )
        )

        assertTrue(result.data.sessions.isEmpty())
        assertTrue(result.data.courseTasks.isEmpty())
        assertTrue(result.data.attendance.isEmpty())
        assertEquals(1, result.report.dropped["sessions"])
        assertEquals(1, result.report.dropped["courseTasks"])
        assertEquals(1, result.report.dropped["attendance"])
    }
}
