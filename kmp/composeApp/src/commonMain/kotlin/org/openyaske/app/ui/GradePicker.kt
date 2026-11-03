package org.openyaske.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Radius
import org.openyaske.app.ui.design.Space
import org.openyaske.core.logic.GradeCatalog
import org.openyaske.core.logic.GradeScheme
import org.openyaske.core.model.GpaGrade

/**
 * 成績の選択。SABCD・秀優良可不可・4.0〜0.0 のどれでも同じ 5 段階を指す。
 * 表示は設定の表記に従い、選ぶと `GpaGrade` で保存する。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun GradePicker(
    current: GpaGrade?,
    scheme: GradeScheme,
    onSelect: (GpaGrade) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    androidx.compose.material3.ButtonGroup(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        GradeCatalog.options(scheme).forEach { option ->
            val selected = option.grade == current
            androidx.compose.material3.ToggleButton(
                checked = selected,
                onCheckedChange = { onSelect(option.grade) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = GradeCatalog.formatPoint(option.point),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

/** 一覧に出す短い成績表示。 */
@Composable
fun GradeBadge(grade: GpaGrade?, scheme: GradeScheme) {
    val colors = LocalAppColors.current
    Text(
        text = grade?.let { GradeCatalog.label(it, scheme) }
            ?: LocalUiStrings.current["grade_not_set"],
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = if (grade == null) colors.textTertiary else colors.primary,
        modifier = Modifier.padding(horizontal = Space.xs)
    )
}
