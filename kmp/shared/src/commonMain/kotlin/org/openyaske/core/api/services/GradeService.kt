package org.openyaske.core.api.services

import org.openyaske.core.api.CoreException
import org.openyaske.core.model.*
import org.openyaske.core.api.GpaCourseRow
import org.openyaske.core.api.GpaScreenState
import org.openyaske.core.logic.GpaProjector

class GradeService internal constructor(private val store: CoreStore) {

    fun saveGrade(grade: CourseGrade): CourseGrade {
        store.requireCourse(grade.courseId)
        if (grade.state == GradeState.GRADED && grade.grade == null) {
            throw CoreException("graded state requires a grade")
        }
        val prepared = grade.copy(
            creditsEarned = grade.creditsEarned?.coerceIn(0, 30),
            updatedAtMillis = store.clock()
        )
        store.repository.saveCourseGrade(prepared)
        store.notifyChanged()
        return prepared
    }

    fun deleteGrade(courseId: String) {
        databaseDeleteGrade(courseId)
        store.notifyChanged()
    }

    fun saveScenario(name: String, overrides: Map<String, GpaGrade>): GpaScenario {
        if (name.isBlank()) throw CoreException("scenario name is required")
        val known = store.snapshot.courses.map { it.id }.toSet()
        if (overrides.keys.any { it !in known }) throw CoreException("unknown course in scenario")
        val scenario = GpaScenario(
            id = store.ids.newId(),
            name = name.trim(),
            overrides = overrides,
            createdAtMillis = store.clock()
        )
        store.repository.saveGpaScenario(scenario)
        store.notifyChanged()
        return scenario
    }

    fun deleteScenario(scenarioId: String) {
        databaseDeleteScenario(scenarioId)
        store.notifyChanged()
    }

    fun gpa(scenarioId: String? = null): GpaScreenState {
        val scenario = scenarioId?.let { id -> store.snapshot.gpaScenarios.firstOrNull { it.id == id } }
        val projection = org.openyaske.core.logic.GpaProjector.project(
            courses = store.snapshot.courses,
            grades = store.snapshot.courseGrades,
            scenario = scenario
        )
        return GpaScreenState(
            base = projection.base,
            scenario = projection.scenario,
            scenarioSummary = projection.scenarioSummary,
            rows = projection.rows.map { row ->
                GpaCourseRow(row.course, row.grade, row.included, row.effectiveGrade)
            }
        )
    }

    // ----- timetable / agenda / course detail -----

    private fun databaseDeleteGrade(courseId: String) {
        // 成績は 1 科目 1 行なので、論理削除ではなく行削除でよい
        store.repository.deleteGrade(courseId)
    }

    private fun databaseDeleteScenario(scenarioId: String) {
        store.repository.deleteScenario(scenarioId)
    }
}
