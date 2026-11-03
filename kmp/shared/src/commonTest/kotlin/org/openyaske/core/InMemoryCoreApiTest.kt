package org.openyaske.core

import org.openyaske.core.api.CoreApi
import org.openyaske.core.data.InMemoryRepository
import org.openyaske.core.model.AttendanceStatus
import org.openyaske.core.model.Course
import org.openyaske.core.model.Semester
import org.openyaske.core.model.Term
import org.openyaske.core.model.Weekday
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * DB を使わない保存層でも CoreApi が動くことを確認する。
 * このテストは JVM と Wasm の両方で実行される。
 */
class InMemoryCoreApiTest {
    @Test
    fun coreApiWorksWithoutSqlDelight() {
        val repository = InMemoryRepository()
        val api = CoreApi(repository, clock = { 1L })
        val term = api.saveTerm(Term.semester(2026, Semester.SPRING, "2026-04-06", "2026-07-20"))
        val course = api.saveCourse(
            Course(
                id = "",
                name = "情報数学",
                weekday = Weekday.MON,
                period = 1,
                termId = term.id
            )
        )
        api.setAttendance(course.id, AttendanceStatus.PRESENT, date = "2026-04-06")

        assertEquals(1, api.state.value.courses.size)
        assertEquals(1, api.attendance(course.id).records.size)
        assertEquals(1, api.timetable(term.id).cells.size)
    }
}
