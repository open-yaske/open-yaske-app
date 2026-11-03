package org.openyaske.core

import org.openyaske.core.model.AcademicYear
import org.openyaske.core.model.AttendanceRecord
import org.openyaske.core.model.AttendanceStatus
import org.openyaske.core.model.CalendarEvent
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseType
import org.openyaske.core.model.Semester
import org.openyaske.core.model.UserData
import org.openyaske.core.model.Weekday
import org.openyaske.core.validation.UserDataCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserDataCodecTest {
    @Test
    fun decodeDropsOnlyBrokenRecords() {
        val raw = """
            {
              "schemaVersion": 1,
              "unknownField": "ignored",
              "courses": [
                {"id": "c1", "name": "情報数学", "credits": 4, "period": 3, "weekday": "tue"},
                {"id": "c2", "name": "   "},
                {"id": "c3"}
              ],
              "attendance": [
                {"id": "a1", "courseId": "c1", "date": "2026-04-01", "status": "present"},
                {"id": "a2", "courseId": "c1", "date": "2026-02-30", "status": "present"},
                {"id": "a3", "courseId": "c1", "date": "2026-04-02", "status": "nope"}
              ]
            }
        """.trimIndent()

        val result = UserDataCodec.decode(raw)

        assertEquals(1, result.data.courses.size)
        assertEquals(4, result.data.courses.single().credits)
        assertEquals(Weekday.TUE, result.data.courses.single().weekday)
        assertEquals(1, result.data.attendance.size)
        assertEquals(2, result.report.dropped["courses"])
        assertEquals(2, result.report.dropped["attendance"])
    }

    @Test
    fun normalizeClampsPeriodRangeSoCoursesCannotOverflowTheWeek() {
        val course = Course(id = "c1", name = "実験", weekday = Weekday.MON, period = 7, periodCount = 3)
        val result = UserDataCodec.normalize(UserData(courses = listOf(course)))

        assertEquals(7, result.data.courses.single().period)
        assertEquals(1, result.data.courses.single().periodCount)
    }

    @Test
    fun normalizeKeepsNewestAttendanceForTheSameDate() {
        val older = AttendanceRecord(id = "a1", courseId = "c1", date = "2026-04-01", status = AttendanceStatus.ABSENT, recordedAtMillis = 100)
        val newer = AttendanceRecord(id = "a2", courseId = "c1", date = "2026-04-01", status = AttendanceStatus.PRESENT, recordedAtMillis = 200)

        val result = UserDataCodec.normalize(
            UserData(
                courses = listOf(Course(id = "c1", name = "数学")),
                attendance = listOf(older, newer)
            )
        )

        assertEquals(1, result.data.attendance.size)
        assertEquals(AttendanceStatus.PRESENT, result.data.attendance.single().status)
        assertEquals(1, result.report.merged["attendance"])
    }

    @Test
    fun normalizeRepairsInvertedEventRange() {
        val event = CalendarEvent(id = "e1", title = "予定", startAtMillis = 1_000, endAtMillis = 500)
        val result = UserDataCodec.normalize(UserData(events = listOf(event)))

        assertEquals(1_000, result.data.events.single().endAtMillis)
    }

    @Test
    fun academicYearIdIsDerivedFromYearAndSemester() {
        val a = AcademicYear(id = "custom-a", year = 2026, semester = Semester.SPRING)
        val b = AcademicYear(id = "custom-b", year = 2026, semester = Semester.SPRING)

        val result = UserDataCodec.normalize(UserData(academicYears = listOf(a, b)))

        assertEquals(1, result.data.academicYears.size)
        assertEquals("2026-spring", result.data.academicYears.single().id)
    }

    @Test
    fun blankCreditsAreNotRequiredAndDefaultToFree() {
        val course = Course(id = "c1", name = "一般教養", credits = 0, type = CourseType.FREE)
        val result = UserDataCodec.normalize(UserData(courses = listOf(course)))

        assertEquals(CourseType.FREE, result.data.courses.single().type)
        assertFalse(result.report.hasIssues)
    }

    @Test
    fun jsonRoundTripPreservesData() {
        val data = UserData(
            courses = listOf(
                Course(
                    id = "c1",
                    name = "線形代数",
                    credits = 2,
                    weekday = Weekday.MON,
                    period = 1,
                    periodCount = 2,
                    type = CourseType.REQUIRED
                )
            )
        )

        val restored = UserDataCodec.decode(UserDataCodec.encode(data))

        assertEquals(data.courses, restored.data.courses)
        assertTrue(restored.report.dropped.isEmpty())
    }

    @Test
    fun malformedRootDoesNotThrow() {
        val result = UserDataCodec.decode("{not json")

        assertEquals(UserData(), result.data)
        assertEquals(1, result.report.dropped["root"])
    }
}
