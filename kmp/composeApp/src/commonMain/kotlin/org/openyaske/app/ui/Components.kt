package org.openyaske.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.core.api.CoreApi
import org.openyaske.core.logic.AgendaEntry
import org.openyaske.core.logic.AgendaItem
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseTask
import org.openyaske.core.model.TaskStatus

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(top = 16.dp, bottom = 6.dp),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
fun AgendaRow(entry: AgendaEntry, onCourse: (String) -> Unit, modifier: Modifier = Modifier) {
    val item = entry.item
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                when (item) {
                    is AgendaItem.Class -> onCourse(item.course.id)
                    is AgendaItem.Task -> item.course?.let { onCourse(it.id) }
                    is AgendaItem.Event -> Unit
                }
            }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = entry.date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            when (item) {
                is AgendaItem.Class -> {
                    Text(
                        text = "${timeLabel(item.session)}  ${item.course.name}",
                        fontWeight = FontWeight.SemiBold
                    )
                    item.session.room?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                is AgendaItem.Event -> {
                    Text(text = item.event.title, fontWeight = FontWeight.SemiBold)
                    Text(LocalUiStrings.current["event_label"], style = MaterialTheme.typography.bodySmall)
                }

                is AgendaItem.Task -> {
                    Text(text = item.task.title, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = item.course?.let { it.name } ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TaskRow(
    api: CoreApi,
    task: CourseTask,
    course: Course?,
    onCourse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalUiStrings.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { course?.let { onCourse(it.id) } }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = task.status == TaskStatus.DONE,
            onCheckedChange = { checked ->
                api.setTaskStatus(task.id, if (checked) TaskStatus.DONE else TaskStatus.OPEN)
            }
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = task.title,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = listOfNotNull(
                    course?.name,
                    task.dueDate?.let { "${t["task_due"]} $it" }
                ).joinToString(" / "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun timeLabel(session: org.openyaske.core.model.CourseSession): String {
    val start = session.startTime
    if (start != null) return start
    val period = session.startPeriod ?: return ""
    return "${period}限"
}

@Composable
fun courseColor(token: String): Color {
    val hex = token.removePrefix("custom:").removePrefix("#")
    return runCatching {
        if (hex.length == 8) Color(hex.toLong(16))
        else Color(0xFF000000 or hex.toLong(16))
    }.getOrDefault(MaterialTheme.colorScheme.primary)
}
