package org.openyaske.core

import org.openyaske.core.logic.AttendanceCalculator
import org.openyaske.core.logic.AttendancePolicy
import org.openyaske.core.logic.GpaCalculator
import org.openyaske.core.logic.TimetableIssue
import org.openyaske.core.logic.TimetableValidator
import org.openyaske.core.model.AttendanceRecord
import org.openyaske.core.model.AttendanceStatus
import org.openyaske.core.model.Course
import org.openyaske.core.model.GpaEntry
import org.openyaske.core.model.GpaGrade
import org.openyaske.core.model.Weekday
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttendanceCalculatorTest {
    private fun record(date: String, status: AttendanceStatus, at: Long = 0) =
        AttendanceRecord(id = date, courseId = "c1", date = date, status = status, recordedAtMillis = at)

    @Test
    fun cancelledIsExcludedAndLateCountsAsHalf() {
        val summary = AttendanceCalculator.summarize(
            listOf(
                record("2026-04-01", AttendanceStatus.PRESENT),
                record("2026-04-02", AttendanceStatus.ABSENT),
                record("2026-04-03", AttendanceStatus.LATE),
                record("2026-04-04", AttendanceStatus.EXCUSED),
                record("2026-04-05", AttendanceStatus.CANCELLED)
            )
        )

        assertEquals(4, summary.total)
        assertEquals(1, summary.cancelled)
        assertEquals(62.5, summary.rate)
        assertEquals(2, summary.absenceLimit)
        assertFalse(summary.atRisk)
    }

    @Test
    fun absenceWarnAtExactlyOneThird() {
        val summary = AttendanceCalculator.summarize(
            listOf(
                record("2026-04-01", AttendanceStatus.PRESENT),
                record("2026-04-02", AttendanceStatus.PRESENT),
                record("2026-04-03", AttendanceStatus.ABSENT)
            )
        )
        // 1 / (2 + 1) = 1/3 ちょうどで警告。
        assertTrue(summary.atRisk)
        assertEquals(1, summary.absenceLimit)
    }

    @Test
    fun absenceBelowOneThirdIsSafe() {
        val summary = AttendanceCalculator.summarize(
            listOf(
                record("2026-04-01", AttendanceStatus.PRESENT),
                record("2026-04-02", AttendanceStatus.PRESENT),
                record("2026-04-03", AttendanceStatus.PRESENT),
                record("2026-04-04", AttendanceStatus.ABSENT)
            )
        )
        // 1 / 4 = 0.25 < 1/3。
        assertFalse(summary.atRisk)
        assertEquals(2, summary.absenceLimit)
    }

    @Test
    fun cancelledDoesNotAffectWarningRatio() {
        val withCancelled = AttendanceCalculator.summarize(
            listOf(
                record("2026-04-01", AttendanceStatus.PRESENT),
                record("2026-04-02", AttendanceStatus.PRESENT),
                record("2026-04-03", AttendanceStatus.ABSENT),
                record("2026-04-04", AttendanceStatus.CANCELLED),
                record("2026-04-05", AttendanceStatus.CANCELLED)
            )
        )
        assertTrue(withCancelled.atRisk)
        assertEquals(3, withCancelled.total)
        assertEquals(2, withCancelled.cancelled)
    }

    @Test
    fun warnRatioCanBeChanged() {
        val summary = AttendanceCalculator.summarize(
            listOf(
                record("2026-04-01", AttendanceStatus.PRESENT),
                record("2026-04-02", AttendanceStatus.PRESENT),
                record("2026-04-03", AttendanceStatus.ABSENT)
            ),
            AttendancePolicy(absenceWarnRatio = 0.5)
        )
        // 1/3 < 0.5 なので、0.5 基準では警告にならない。
        assertFalse(summary.atRisk)
        assertEquals(2, summary.absenceLimit)
    }

    @Test
    fun sameDateIsCountedOnceAndNewestWins() {
        val summary = AttendanceCalculator.summarize(
            listOf(
                record("2026-04-01", AttendanceStatus.ABSENT, at = 100),
                record("2026-04-01", AttendanceStatus.PRESENT, at = 200)
            )
        )

        assertEquals(1, summary.total)
        assertEquals(1, summary.present)
        assertEquals(100.0, summary.rate)
    }

    @Test
    fun lateWeightCanBeConfiguredToOne() {
        val summary = AttendanceCalculator.summarize(
            listOf(
                record("2026-04-01", AttendanceStatus.PRESENT),
                record("2026-04-02", AttendanceStatus.ABSENT),
                record("2026-04-03", AttendanceStatus.LATE),
                record("2026-04-04", AttendanceStatus.EXCUSED)
            ),
            AttendancePolicy(lateWeight = 1.0)
        )

        assertEquals(75.0, summary.rate)
    }

    @Test
    fun emptyRecordsProduceZeroRateNotDivisionByZero() {
        val summary = AttendanceCalculator.summarize(emptyList())
        assertEquals(0.0, summary.rate)
        assertEquals(0, summary.total)
    }
}

class GpaCalculatorTest {
    @Test
    fun gpaIsCreditWeighted() {
        val summary = GpaCalculator.calculate(
            listOf(
                GpaEntry("1", "S の科目", 2, GpaGrade.S),
                GpaEntry("2", "A の科目", 2, GpaGrade.A),
                GpaEntry("3", "F の科目", 1, GpaGrade.F)
            )
        )

        assertEquals(5, summary.totalCredits)
        assertEquals(14.0, summary.totalPoints)
        assertEquals(2.8, summary.gpa)
    }

    @Test
    fun zeroCreditEntryIsIgnored() {
        val summary = GpaCalculator.calculate(listOf(GpaEntry("1", "0 単位", 0, GpaGrade.S)))
        assertEquals(0, summary.totalCredits)
        assertEquals(0.0, summary.gpa)
    }
}

class TimetableValidatorTest {
    @Test
    fun detectsOverlappingCourses() {
        val a = Course(id = "a", name = "A", weekday = Weekday.MON, period = 1, periodCount = 2)
        val b = Course(id = "b", name = "B", weekday = Weekday.MON, period = 2)

        val issues = TimetableValidator.validate(listOf(a, b))

        assertTrue(issues.any { it is TimetableIssue.Conflict })
    }

    @Test
    fun detectsOutOfRangePeriodCount() {
        val course = Course(id = "a", name = "A", weekday = Weekday.MON, period = 7, periodCount = 2)
        val issues = TimetableValidator.validate(listOf(course))

        assertTrue(issues.any { it is TimetableIssue.OutOfRange })
    }

    @Test
    fun adjacentCoursesDoNotConflict() {
        val a = Course(id = "a", name = "A", weekday = Weekday.TUE, period = 1, periodCount = 2)
        val b = Course(id = "b", name = "B", weekday = Weekday.TUE, period = 3)

        assertTrue(TimetableValidator.validate(listOf(a, b)).isEmpty())
    }
}
