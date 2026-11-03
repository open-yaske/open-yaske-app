package org.openyaske.core

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.openyaske.core.db.OpenYaskeDatabase
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * SQLDelight のスキーマが制約どおりに動くかを JVM のインメモリ SQLite で検証する。
 */
class DatabaseSchemaTest {
    private var driver: JdbcSqliteDriver? = null

    private fun newDb(): OpenYaskeDatabase {
        val d = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver = d
        d.execute(null, "PRAGMA foreign_keys = ON", 0)
        OpenYaskeDatabase.Schema.create(d)
        return OpenYaskeDatabase(d)
    }

    @AfterTest
    fun close() {
        driver?.close()
    }

    private fun OpenYaskeDatabase.addCourse(
        id: String,
        weekday: Long? = 1,
        period: Long? = 1
    ) {
        coreQueries.insertCourse(
            id = id,
            title = "科目$id",
            code = null,
            instructor = null,
            room = null,
            url = null,
            memo = null,
            course_type = "free",
            kind = "regular",
            source = "user",
            institution = null,
            credits = 2,
            credit_system = "uec",
            counts_toward_gpa = 1,
            weekday = weekday,
            period = period,
            period_count = 1,
            color = "#000000",
            syllabus_id = null,
            term_id = null,
            academic_year_id = null,
            created_at_ms = 1,
            updated_at_ms = 1,
            deleted_at_ms = null
        )
    }

    private fun OpenYaskeDatabase.addSession(id: String, courseId: String = "c1") {
        sessionsQueries.insertSession(
            id = id,
            course_id = courseId,
            date = "2026-04-06",
            start_period = 1,
            period_count = 1,
            start_time = null,
            end_time = null,
            room = null,
            kind = "regular",
            status = "scheduled",
            note = null,
            replaces_session_id = null,
            created_at_ms = 1,
            updated_at_ms = 1,
            deleted_at_ms = null
        )
    }

    @Test
    fun attendanceIsUpsertedPerSession() {
        val db = newDb()
        db.addCourse("c1")
        db.addSession("s1")

        db.sessionsQueries.upsertAttendance("a1", "c1", "s1", "2026-04-06", "present", 1, null)
        db.sessionsQueries.upsertAttendance("a2", "c1", "s1", "2026-04-06", "absent", 2, null)

        val rows = db.sessionsQueries.selectAttendanceBySession("s1").executeAsList()
        assertEquals(1, rows.size)
        assertEquals("absent", rows.single().status)
    }

    @Test
    fun attendanceIsUpsertedPerDayWithoutSession() {
        val db = newDb()
        db.addCourse("c1")

        db.sessionsQueries.upsertAttendance("a1", "c1", null, "2026-04-06", "present", 1, null)
        db.sessionsQueries.upsertAttendance("a2", "c1", null, "2026-04-06", "late", 2, null)

        val rows = db.sessionsQueries.selectAttendanceByCourse("c1").executeAsList()
        assertEquals(1, rows.size)
        assertEquals("late", rows.single().status)
    }

    @Test
    fun hardDeleteCourseCascadesToChildren() {
        val db = newDb()
        val d = driver!!
        db.addCourse("c1")
        db.addSession("s1")
        db.sessionsQueries.upsertAttendance("a1", "c1", "s1", "2026-04-06", "present", 1, null)
        d.execute(
            null,
            "INSERT INTO course_tasks(id, course_id, kind, title, status, created_at_ms, updated_at_ms) " +
                "VALUES ('t1', 'c1', 'assignment', '課題', 'open', 1, 1)",
            0
        )

        d.execute(null, "DELETE FROM courses WHERE id = 'c1'", 0)

        assertTrue(db.sessionsQueries.selectSessionsByCourse("c1").executeAsList().isEmpty())
        assertTrue(db.sessionsQueries.selectAttendanceByCourse("c1").executeAsList().isEmpty())
        assertTrue(db.plannerQueries.selectTasksByCourse("c1").executeAsList().isEmpty())
    }

    @Test
    fun weekdayAndPeriodMustBeBothPresentOrBothNull() {
        val db = newDb()
        val d = driver!!
        assertFailsWith<Exception> {
            d.execute(
                null,
                "INSERT INTO courses(id, title, course_type, kind, source, credits, credit_system, " +
                    "counts_toward_gpa, weekday, period, period_count, color, created_at_ms, updated_at_ms) " +
                    "VALUES ('bad', 'x', 'free', 'regular', 'user', 2, 'uec', 1, NULL, 1, 1, '#000', 1, 1)",
                0
            )
        }
    }

    @Test
    fun sessionMustHavePeriodOrStartTime() {
        val db = newDb()
        db.addCourse("c1")
        val d = driver!!
        assertFailsWith<Exception> {
            d.execute(
                null,
                "INSERT INTO course_sessions(id, course_id, date, period_count, kind, status, created_at_ms, updated_at_ms) " +
                    "VALUES ('bad', 'c1', '2026-04-06', 1, 'intensive', 'scheduled', 1, 1)",
                0
            )
        }
    }

    @Test
    fun termYearAndSeasonAreUnique() {
        val db = newDb()
        val d = driver!!
        d.execute(
            null,
            "INSERT INTO terms(id, year, kind, season, start_date, end_date, label) " +
                "VALUES ('t1', 2026, 'semester', 'spring', '2026-04-06', '2026-07-20', '')",
            0
        )
        assertFailsWith<Exception> {
            d.execute(
                null,
                "INSERT INTO terms(id, year, kind, season, start_date, end_date, label) " +
                    "VALUES ('t2', 2026, 'semester', 'spring', '2026-04-06', '2026-07-20', '')",
                0
            )
        }
    }

    @Test
    fun settingsAreSingleRow() {
        val db = newDb()
        db.settingsQueries.upsertSettings(1, "{}", 1)
        db.settingsQueries.upsertSettings(1, "{\"theme\":\"dark\"}", 2)

        assertEquals(1, db.settingsQueries.selectSettings().executeAsList().size)
        assertEquals("{\"theme\":\"dark\"}", db.settingsQueries.selectSettings().executeAsOne().json)
    }

    @Test
    fun syncLinkIsUpsertedPerLocalEntity() {
        val db = newDb()
        db.addCourse("c1")
        db.addSession("s1")
        db.syncQueries.upsertSyncLink("l1", "session", "s1", "calendar", "ext-1", null, "to_device", 1, null)
        db.syncQueries.upsertSyncLink("l2", "session", "s1", "calendar", "ext-2", null, "to_device", 2, null)

        val rows = db.syncQueries.selectSyncLinks().executeAsList()
        assertEquals(1, rows.size)
        assertEquals("ext-2", rows.single().external_id)
    }
}
