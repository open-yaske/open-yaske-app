package org.openyaske.core.model

import kotlinx.serialization.Serializable

/** GPA 用の 1 科目分の入力。 */
@Serializable
data class GpaEntry(
    val id: String,
    val courseTitle: String,
    val credits: Int,
    val grade: GpaGrade,
    val academicYearId: String? = null
)
