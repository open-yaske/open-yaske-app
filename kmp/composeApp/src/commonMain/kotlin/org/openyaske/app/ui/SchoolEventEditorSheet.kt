package org.openyaske.app.ui

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.ModalBackHandler
import org.openyaske.app.ui.design.PrimaryButton
import org.openyaske.app.ui.design.SheetDismissHaptic
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.TimeField
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.CoreException
import org.openyaske.core.model.SchoolEvent

/**
 * 学事予定の編集。更新は同じ id の上書き、削除は既存の論理削除を使う。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolEventEditorSheet(
    api: CoreApi,
    event: SchoolEvent?,
    initialDate: String,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val isNew = event == null
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(event?.title ?: "") }
    var date by remember { mutableStateOf(event?.date ?: initialDate) }
    var allDay by remember { mutableStateOf(event?.allDay ?: true) }
    var startTime by remember { mutableStateOf(event?.startTime) }
    var endTime by remember { mutableStateOf(event?.endTime) }
    var location by remember { mutableStateOf(event?.location ?: "") }
    var note by remember { mutableStateOf(event?.note ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    SheetDismissHaptic(sheetState)
    ModalBackHandler(onDismiss)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.xl)
                .padding(bottom = Space.xxl)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Text(
                text = if (isNew) t["calendar_add"] else t["event_edit"],
                style = MaterialTheme.typography.titleLarge,
                color = colors.text
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(t["event_title"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(t["event_date"], style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            DateField(
                label = t["event_date"],
                value = date,
                onChange = { date = it ?: date },
                allowClear = false
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = t["event_allday"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = allDay, onCheckedChange = { allDay = it })
            }
            if (!allDay) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimeField(
                        label = t["event_start"],
                        value = startTime,
                        onChange = { startTime = it },
                        modifier = Modifier.weight(1f)
                    )
                    TimeField(
                        label = t["event_end"],
                        value = endTime,
                        onChange = { endTime = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(t["event_location"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(t["event_note"]) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.danger)
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isNew) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(t["common_delete"], color = colors.danger)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                PrimaryButton(
                    text = t["common_save"],
                    onClick = {
                        if (title.isBlank()) {
                            error = t["event_title"]
                            return@PrimaryButton
                        }
                        try {
                            val base = event ?: SchoolEvent(
                                id = "",
                                title = title.trim(),
                                kind = org.openyaske.core.model.SchoolEventKind.ACADEMIC,
                                date = date
                            )
                            api.saveSchoolEvent(
                                base.copy(
                                    title = title.trim(),
                                    date = date,
                                    allDay = allDay,
                                    startTime = if (allDay) null else startTime,
                                    endTime = if (allDay) null else endTime,
                                    location = location.ifBlank { null },
                                    note = note.ifBlank { null }
                                )
                            )
                            onDone()
                        } catch (e: CoreException) {
                            error = e.message
                        }
                    }
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(t["event_delete"]) },
            text = { Text(t["event_delete_confirm"], style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        event?.let { api.deleteSchoolEvent(it.id) }
                        confirmDelete = false
                        onDone()
                    }
                ) { Text(t["common_delete"]) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(t["close"]) }
            }
        )
    }
}
