package org.openyaske.core.logic

import org.openyaske.core.model.GpaGrade

/** 成績の表記。保存は `GpaGrade` のまま、表示だけを切り替える。 */
enum class GradeScheme {
    /** S / A / B / C / D */
    SABCD,

    /** 秀 / 優 / 良 / 可 / 不可 */
    JAPANESE,

    /** 4.0 / 3.0 / 2.0 / 1.0 / 0.0 */
    NUMERIC;

    companion object {
        fun fromKey(key: String?): GradeScheme =
            entries.firstOrNull { it.name == key } ?: SABCD
    }
}

/** 1 つの評価の表示。 */
data class GradeOption(
    val grade: GpaGrade,
    val label: String,
    val point: Double
)

/**
 * 成績表記の対応表。
 *
 * 内部は `GpaGrade`（S/A/B/C/F とポイント）だけを持ち、
 * SABCD・秀優良可不可・4.0〜0.0 はここで相互に変換する。
 * 入力はどの表記でも受け付ける（「秀」「S」「4.0」はすべて S になる）。
 */
object GradeCatalog {
    val order: List<GpaGrade> = listOf(
        GpaGrade.S,
        GpaGrade.A,
        GpaGrade.B,
        GpaGrade.C,
        GpaGrade.F
    )

    fun options(scheme: GradeScheme): List<GradeOption> =
        order.map { grade -> GradeOption(grade, label(grade, scheme), grade.point) }

    fun label(grade: GpaGrade, scheme: GradeScheme): String = when (scheme) {
        GradeScheme.SABCD -> when (grade) {
            GpaGrade.S -> "S"
            GpaGrade.A -> "A"
            GpaGrade.B -> "B"
            GpaGrade.C -> "C"
            GpaGrade.F -> "D"
        }

        GradeScheme.JAPANESE -> when (grade) {
            GpaGrade.S -> "秀"
            GpaGrade.A -> "優"
            GpaGrade.B -> "良"
            GpaGrade.C -> "可"
            GpaGrade.F -> "不可"
        }

        GradeScheme.NUMERIC -> formatPoint(grade.point)
    }

    fun formatPoint(point: Double): String {
        val whole = point.toInt()
        val tenth = ((point - whole) * 10).toInt().coerceIn(0, 9)
        return "$whole.$tenth"
    }

    /** どの表記の入力も `GpaGrade` に寄せる。 */
    fun parse(input: String): GpaGrade? {
        val text = input.trim().uppercase()
        return when (text) {
            "S", "秀", "4", "4.0" -> GpaGrade.S
            "A", "優", "3", "3.0" -> GpaGrade.A
            "B", "良", "2", "2.0" -> GpaGrade.B
            "C", "可", "1", "1.0" -> GpaGrade.C
            "D", "F", "不可", "0", "0.0" -> GpaGrade.F
            else -> null
        }
    }
}
