package org.openyaske.core.logic

import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseGrade
import org.openyaske.core.model.GpaEntry
import org.openyaske.core.model.GpaScenario
import org.openyaske.core.model.GradeState

data class GpaSummary(
    /** GPA の分母になった単位数。 */
    val totalCredits: Int,
    val totalPoints: Double,
    val gpa: Double,
    /** 修得済み単位数（合否科目・認定科目を含む）。 */
    val earnedCredits: Int = 0
)

object GpaCalculator {
    /**
     * 旧形式の GPA 行を集計する。
     * 0 単位の行は分母に入れない（0 除算を避ける）。
     */
    fun calculate(entries: List<GpaEntry>): GpaSummary {
        var totalCredits = 0
        var totalPoints = 0.0
        var earnedCredits = 0

        for (entry in entries) {
            val credits = entry.credits.coerceAtLeast(0)
            if (credits == 0) continue
            totalCredits += credits
            totalPoints += credits * entry.grade.point
            if (entry.grade.point > 0) earnedCredits += credits
        }

        return GpaSummary(
            totalCredits = totalCredits,
            totalPoints = totalPoints,
            gpa = if (totalCredits == 0) 0.0 else totalPoints / totalCredits,
            earnedCredits = earnedCredits
        )
    }

    /**
     * 科目に紐づいた成績から GPA を計算する。
     *
     * - 未確定・進行中・除外は分母から外す
     * - 合否科目と認定科目は修得単位にだけ数える
     * - 科目側の `countsTowardGpa` が false なら分母から外す
     * - `scenario` がある場合は、その科目だけ成績を差し替えて試算する
     */
    fun calculateGrades(
        grades: List<CourseGrade>,
        courses: Map<String, Course>,
        scenario: GpaScenario? = null
    ): GpaSummary {
        val byCourse = linkedMapOf<String, CourseGrade>()
        for (grade in grades) {
            val existing = byCourse[grade.courseId]
            if (existing == null || grade.updatedAtMillis >= existing.updatedAtMillis) {
                byCourse[grade.courseId] = grade
            }
        }

        var gpaCredits = 0
        var totalPoints = 0.0
        var earnedCredits = 0

        for (grade in byCourse.values) {
            val course = courses[grade.courseId]
            val override = scenario?.overrides?.get(grade.courseId)
            val effectiveState = if (override != null) GradeState.GRADED else grade.state
            val effectiveGrade = override ?: grade.grade
            val credits = grade.creditsEarned ?: course?.credits ?: 0
            if (credits <= 0) continue

            when (effectiveState) {
                GradeState.GRADED -> {
                    if (effectiveGrade == null) continue
                    if (!grade.countsTowardGpa) continue
                    if (course?.countsTowardGpa == false) continue
                    gpaCredits += credits
                    totalPoints += credits * effectiveGrade.point
                    if (effectiveGrade.point > 0) earnedCredits += credits
                }

                GradeState.PASS_FAIL,
                GradeState.TRANSFER -> earnedCredits += credits

                GradeState.NOT_GRADED,
                GradeState.IN_PROGRESS,
                GradeState.EXCLUDED -> Unit
            }
        }

        return GpaSummary(
            totalCredits = gpaCredits,
            totalPoints = totalPoints,
            gpa = if (gpaCredits == 0) 0.0 else totalPoints / gpaCredits,
            earnedCredits = earnedCredits
        )
    }
}
