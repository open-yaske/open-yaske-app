package org.openyaske.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * シラバス TSV の 1 コマ分。
 *
 * `period` が null のものは曜日時限を持たない科目（集中講義、応用科目など）。
 * APK 版は「月1, 月2」のような複数コマ表記から最初の 1 コマしか作らず、
 * 2 コマ分の授業が 1 コマとして登録されていた。
 */
@Serializable
data class SyllabusEntry(
    @SerialName("i") val id: String,
    @SerialName("y") val year: Int,
    @SerialName("c") val code: String,
    @SerialName("t") val title: String,
    @SerialName("n") val instructor: String = "",
    @SerialName("r") val room: String? = null,
    @SerialName("w") val weekday: Weekday? = null,
    @SerialName("p") val period: Int? = null,
    /** 前学期 / 後学期 / 通年 / TSV の表記そのまま */
    @SerialName("s") val semester: String = "",
    @SerialName("s1") val semester1: String? = null,
    @SerialName("s2") val semester2: String? = null,
    /** 昼間 / 夜間 / 修士 / 博士。科目コードの先頭 2 桁で決まる。 */
    @SerialName("g") val programType: String? = null
) {
    val hasSlot: Boolean get() = weekday != null && period != null
}
