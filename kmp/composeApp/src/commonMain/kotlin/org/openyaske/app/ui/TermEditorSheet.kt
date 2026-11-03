package org.openyaske.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.openyaske.app.ui.design.DateField
import org.openyaske.app.ui.design.FilterPill
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.ModalBackHandler
import org.openyaske.app.ui.design.PrimaryButton
import org.openyaske.app.ui.design.SheetDismissHaptic
import org.openyaske.app.ui.design.Space
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.CoreException
import org.openyaske.core.model.Semester
import org.openyaske.core.model.Term
import org.openyaske.core.model.TermKind

/**
 * 学期の追加・編集。開始日と終了日を変えると、その学期の科目の授業回が作り直される。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermEditorSheet(
    api: CoreApi,
    term: Term?,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val thisYear = remember { todayIso().substring(0, 4).toIntOrNull() ?: 2026 }
    var year by remember { mutableStateOf((term?.year ?: thisYear).toString()) }
    var season by remember { mutableStateOf(term?.season ?: Semester.SPRING) }
    var fullYear by remember { mutableStateOf(term?.kind == TermKind.FULL_YEAR) }
    var start by remember { mutableStateOf(term?.startDate ?: "$thisYear-04-08") }
    var end by remember { mutableStateOf(term?.endDate ?: "$thisYear-07-29") }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    SheetDismissHaptic(sheetState)
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
                text = if (term == null) t["term_add"] else t["term_edit"],
                style = MaterialTheme.typography.titleLarge,
                color = colors.text
            )
            OutlinedTextField(
                value = year,
                onValueChange = { year = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text(t["term_year"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                FilterPill(
                    text = t["term_spring"],
                    onClick = { fullYear = false; season = Semester.SPRING },
                    selected = !fullYear && season == Semester.SPRING
                )
                FilterPill(
                    text = t["term_fall"],
                    onClick = { fullYear = false; season = Semester.FALL },
                    selected = !fullYear && season == Semester.FALL
                )
                FilterPill(
                    text = t["term_full_year"],
                    onClick = { fullYear = true },
                    selected = fullYear
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t["term_start"], style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                DateField(
                    label = t["term_start"],
                    value = start,
                    onChange = { start = it ?: start },
                    modifier = Modifier.weight(1f).padding(start = Space.sm),
                    allowClear = false
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t["term_end"], style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                DateField(
                    label = t["term_end"],
                    value = end,
                    onChange = { end = it ?: end },
                    modifier = Modifier.weight(1f).padding(start = Space.sm),
                    allowClear = false
                )
            }
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.danger)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (term != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(t["common_delete"], color = colors.danger)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                PrimaryButton(
                    text = t["common_save"],
                    onClick = {
                        val y = year.toIntOrNull()
                        if (y == null) {
                            error = t["term_year"]
                            return@PrimaryButton
                        }
                        try {
                            val prepared = if (fullYear) {
                                Term(
                                    id = term?.id ?: "$y-year",
                                    year = y,
                                    kind = TermKind.FULL_YEAR,
                                    season = null,
                                    startDate = start,
                                    endDate = end
                                )
                            } else {
                                Term.semester(y, season, start, end).let { candidate ->
                                    if (term != null) candidate.copy(id = term.id) else candidate
                                }
                            }
                            api.saveTerm(prepared)
                            onDone()
                        } catch (e: CoreException) {
                            error = e.message
                        } catch (e: IllegalArgumentException) {
                            error = e.message
                        }
                    }
                )
            }
        }
    }

    if (confirmDelete && term != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(t["common_delete"]) },
            text = { Text(t["term_delete_confirm"], style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            api.deleteTerm(term.id)
                            confirmDelete = false
                            onDone()
                        } catch (e: CoreException) {
                            confirmDelete = false
                            error = e.message
                        }
                    }
                ) { Text(t["common_delete"]) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(t["close"]) }
            }
        )
    }
}
