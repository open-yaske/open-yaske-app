package org.openyaske.app.ui

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.dp
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.DateField
import org.openyaske.app.ui.design.FilterPill
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.ModalBackHandler
import org.openyaske.app.ui.design.SheetDismissHaptic
import org.openyaske.app.ui.design.PrimaryButton
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.TimeField
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.CoreException
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseTask
import org.openyaske.core.model.RepeatPattern
import org.openyaske.core.model.TodoPriority

/** 科目の選択。件数が多くても画面内に収まるよう、検索と高さの上限を持つ。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoursePickerSheet(
    courses: List<Course>,
    onSelect: (Course?) -> Unit,
    onDismiss: () -> Unit
) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SheetDismissHaptic(sheetState)
    var query by remember { mutableStateOf("") }
    val filtered = remember(courses, query) {
        if (query.isBlank()) courses else courses.filter { it.name.contains(query, ignoreCase = true) }
    }

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
            Text(t["course_detail"], style = MaterialTheme.typography.titleLarge, color = colors.text)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(t["search_hint"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp)
            ) {
                if (query.isBlank()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(null) }
                                .padding(vertical = Space.md),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = t["task_no_course"],
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.text
                            )
                        }
                    }
                }
                items(filtered.size) { index ->
                    val course = filtered[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(course) }
                            .padding(vertical = Space.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = course.name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${course.credits}" + t["timetable_credit_unit"],
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * TODO の追加と編集。`task == null` なら追加。
 *
 * 題 → 科目 → 期限（日付・時刻）の順に置く。保存は `api.saveTask` に集約する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    api: CoreApi,
    task: CourseTask?,
    courses: List<Course>,
    onDismiss: () -> Unit
) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(task?.title ?: "") }
    var courseId by remember { mutableStateOf(task?.courseId ?: courses.firstOrNull()?.id) }
    var dueDate by remember { mutableStateOf(task?.dueDate) }
    var dueTime by remember { mutableStateOf(task?.dueTime) }
    var showCoursePicker by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(task?.repeat ?: RepeatPattern.NONE) }
    var priority by remember { mutableStateOf(task?.priority) }
    var notes by remember { mutableStateOf(task?.notes ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val selectedCourse = courses.firstOrNull { it.id == courseId }

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
                text = if (task == null) t["add_task"] else t["task_edit"],
                style = MaterialTheme.typography.titleLarge,
                color = colors.text
            )
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(t["task_title"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = t["course_detail"],
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            FilterPill(
                text = selectedCourse?.name ?: t["course_detail"],
                onClick = { showCoursePicker = true },
                trailing = org.openyaske.app.ui.design.AppIcons.dropdown
            )

            Text(
                text = t["task_due"],
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {}
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(Space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateField(
                    label = t["task_due"],
                    value = dueDate,
                    onChange = { dueDate = it },
                    modifier = Modifier.weight(1f)
                )
                TimeField(
                    label = t["task_due_time"],
                    value = dueTime,
                    onChange = { dueTime = it },
                    modifier = Modifier.weight(1f)
                )
            }

            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.danger)
            }

            Text(
                text = t["task_priority"],
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                listOf(
                    null to t["priority_none"],
                    TodoPriority.LOW to t["priority_low"],
                    TodoPriority.MEDIUM to t["priority_medium"],
                    TodoPriority.HIGH to t["priority_high"]
                ).forEach { (value, label) ->
                    FilterPill(
                        text = label,
                        onClick = { priority = value },
                        selected = priority == value
                    )
                }
            }

            Text(
                text = t["task_repeat"],
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                listOf(
                    RepeatPattern.NONE to t["repeat_none"],
                    RepeatPattern.DAILY to t["repeat_daily"],
                    RepeatPattern.WEEKLY to t["repeat_weekly"],
                    RepeatPattern.MONTHLY to t["repeat_monthly"]
                ).forEach { (value, label) ->
                    FilterPill(
                        text = label,
                        onClick = { repeat = value },
                        selected = repeat == value
                    )
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(t["task_notes"]) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text(t["common_delete"], color = colors.danger)
                    }
                }
                PrimaryButton(
                    text = t["common_save"],
                    onClick = {
                        if (title.isBlank()) {
                            error = t["task_title"]
                            return@PrimaryButton
                        }
                        try {
                            val base = task ?: CourseTask(id = "", courseId = courseId, title = title)
                            api.saveTask(
                                base.copy(
                                    title = title.trim(),
                                    courseId = courseId,
                                    dueDate = dueDate,
                                    dueTime = dueTime,
                                    repeat = repeat.takeIf { it != RepeatPattern.NONE },
                                    priority = priority,
                                    notes = notes.ifBlank { null }
                                )
                            )
                            onDismiss()
                        } catch (e: CoreException) {
                            error = e.message
                        }
                    }
                )
                TextButton(onClick = onDismiss) { Text(t["close"]) }
            }
        }
    }

    if (showCoursePicker) {
        CoursePickerSheet(
            courses = courses,
            onSelect = { picked ->
                courseId = picked?.id
                showCoursePicker = false
            },
            onDismiss = { showCoursePicker = false }
        )
    }

    if (confirmDelete && task != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(t["task_delete"]) },
            text = { Text(task.title, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        api.deleteTask(task.id)
                        confirmDelete = false
                        onDismiss()
                    }
                ) { Text(t["common_delete"]) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(t["close"]) }
            }
        )
    }
}
