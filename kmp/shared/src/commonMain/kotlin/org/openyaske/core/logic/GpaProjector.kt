package org.openyaske.core.logic

import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseGrade
import org.openyaske.core.model.GpaScenario

/** GPA 画面に出す 1 行。 */
data class GpaRow(
    val course: Course,
    val grade: CourseGrade?,
    val included: Boolean,
    val effectiveGrade: org.openyaske.core.model.GpaGrade?
)

/** GPA 画面の材料。 */
data class GpaProjection(
    val base: GpaSummary,
    val scenario: GpaScenario?,
    val scenarioSummary: GpaSummary?,
    val rows: List<GpaRow>
)

/**
 * 成績の集計と一覧の組み立て。
 *
 * 副作用を持たないので、画面の都合で何度呼んでも安全。
 * 保存は `CoreApi` 側が持ち、ここは読むだけにしている。
 */
object GpaProjector {
    fun project(
        courses: List<Course>,
        grades: List<CourseGrade>,
        scenario: GpaScenario?
    ): GpaProjection {
        val visible = courses.filter { it.deletedAtMillis == null }
        val coursesById = visible.associateBy { it.id }
        val base = GpaCalculator.calculateGrades(grades, coursesById)
        val scenarioSummary = scenario?.let {
            GpaCalculator.calculateGrades(grades, coursesById, it)
        }
        val gradesByCourse = grades.associateBy { it.courseId }
        val rows = visible.mapNotNull { course ->
            val grade = gradesByCourse[course.id]
            val override = scenario?.overrides?.get(course.id)
            if (grade == null && override == null) return@mapNotNull null
            val effective = override ?: grade?.grade
            val included = override != null ||
                (grade?.state == org.openyaske.core.model.GradeState.GRADED &&
                    grade.countsTowardGpa &&
                    course.countsTowardGpa)
            GpaRow(course, grade, included, effective)
        }.sortedBy { it.course.name }
        return GpaProjection(base, scenario, scenarioSummary, rows)
    }
}
