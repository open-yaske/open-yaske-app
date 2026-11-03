package org.openyaske.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.ModalBackHandler
import org.openyaske.app.ui.design.PrimaryButton
import org.openyaske.app.ui.design.SheetDismissHaptic
import org.openyaske.app.ui.design.Space
import org.openyaske.core.api.CoreApi
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseTask
import org.openyaske.core.model.TaskStatus

/**
 * TODO の詳細。TODO 画面の外（ホーム・カレンダー・科目詳細）から開く。
 * 完了の切り替えはここで行い、内容の変更は編集シートへ送る。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    api: CoreApi,
    task: CourseTask,
    course: Course?,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val done = task.status == TaskStatus.DONE

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.text,
                        textDecoration = if (done) TextDecoration.LineThrough else null
                    )
                    Text(
                        text = course?.name ?: t["task_no_course"],
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (done) colors.primary else colors.surface)
                        .clickable {
                            org.openyaske.app.ui.design.selectFeedback()
                            api.setTaskStatus(task.id, if (done) TaskStatus.OPEN else TaskStatus.DONE)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Icon(AppIcons.check, null, tint = colors.onPrimary, modifier = Modifier.size(16.dp))
                    }
                }
            }

            HairLine(modifier = Modifier.padding(vertical = Space.xs))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = t["task_complete"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.text,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = done,
                    onCheckedChange = { checked ->
                        org.openyaske.app.ui.design.selectFeedback()
                        api.setTaskStatus(task.id, if (checked) TaskStatus.DONE else TaskStatus.OPEN)
                    }
                )
            }

            DetailLine(t["task_due"], listOfNotNull(task.dueDate, task.dueTime).joinToString(" "))
            DetailLine(t["task_priority"], task.priority?.let { label(it, t) } ?: t["priority_none"])
            DetailLine(t["task_repeat"], repeatLabel(task, t))
            task.notes?.takeIf { it.isNotBlank() }?.let { DetailLine(t["task_notes"], it) }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryButton(text = t["task_edit"], onClick = onEdit)
                TextButton(onClick = onDismiss) { Text(t["close"]) }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
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

@Composable
private fun label(priority: org.openyaske.core.model.TodoPriority, t: UiStrings): String = when (priority) {
    org.openyaske.core.model.TodoPriority.LOW -> t["priority_low"]
    org.openyaske.core.model.TodoPriority.MEDIUM -> t["priority_medium"]
    org.openyaske.core.model.TodoPriority.HIGH -> t["priority_high"]
}

@Composable
private fun repeatLabel(task: CourseTask, t: UiStrings): String = when (task.repeat) {
    org.openyaske.core.model.RepeatPattern.DAILY -> t["repeat_daily"]
    org.openyaske.core.model.RepeatPattern.WEEKLY -> t["repeat_weekly"]
    org.openyaske.core.model.RepeatPattern.BIWEEKLY -> t["repeat_weekly"]
    org.openyaske.core.model.RepeatPattern.MONTHLY -> t["repeat_monthly"]
    else -> t["repeat_none"]
}
