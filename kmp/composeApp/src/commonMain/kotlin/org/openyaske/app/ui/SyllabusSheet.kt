package org.openyaske.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.ModalBackHandler
import org.openyaske.app.ui.design.SheetDismissHaptic
import org.openyaske.app.ui.design.PrimaryButton
import org.openyaske.app.ui.design.Space
import org.openyaske.core.model.SyllabusEntry

/**
 * シラバスの 1 件を開くシート。検索結果の行を押すと出る。
 * ここから履修に追加する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyllabusSheet(
    entry: SyllabusEntry,
    added: Boolean,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SheetDismissHaptic(sheetState)
    val meta = listOfNotNull(
        entry.code.ifBlank { null },
        entry.instructor.ifBlank { null }
    ).joinToString(" · ")

    ModalBackHandler(onDismiss)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xl)
                .padding(bottom = Space.xxl),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleLarge,
                color = colors.text
            )
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            }
            HairLine(modifier = Modifier.padding(vertical = Space.xs))
            SheetLine(t["day_${entry.weekday?.isoNumber ?: 1}"], t["search_weekday"])
            entry.period?.let { line -> SheetLine("$line" + t["search_period_suffix"], t["search_period"]) }
            entry.room?.let { SheetLine(it, t["course_room"]) }
            if (entry.semester.isNotBlank()) SheetLine(entry.semester, t["timetable_term"])
            entry.programType?.let { SheetLine(it, t["search_program"]) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (added) {
                    Text(
                        text = t["search_added"],
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
                    )
                } else {
                    PrimaryButton(text = t["search_add"], onClick = onAdd)
                }
                TextButton(onClick = onDismiss) { Text(t["close"]) }
            }
        }
    }
}

@Composable
private fun SheetLine(value: String, label: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.text)
    }
}
