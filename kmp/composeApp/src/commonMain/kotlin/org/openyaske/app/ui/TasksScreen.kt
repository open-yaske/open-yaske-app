package org.openyaske.app.ui

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.toShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.app.ui.design.Layout
import org.openyaske.app.ui.design.screenBottomPadding
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.CircleIconButton
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Radius
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.parseCourseColor
import org.openyaske.app.ui.design.tabular
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.CoreException
import org.openyaske.core.model.CourseTask
import org.openyaske.core.model.TaskStatus

private enum class TaskFilter { OPEN, DONE, ALL }

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TasksScreen(
    api: CoreApi,
    onCourse: (String) -> Unit,
    onSettings: () -> Unit,
    bottomInset: Boolean = true
) {
    val data by api.state.collectAsState()
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    var filter by remember { mutableStateOf(TaskFilter.OPEN) }
    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CourseTask?>(null) }
    var deletingTask by remember { mutableStateOf<CourseTask?>(null) }
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val coursesById = remember(data) { data.courses.associateBy { it.id } }
    val colorIndex = remember(data) {
        data.courses.sortedBy { it.name }.withIndex().associate { it.value.id to it.index }
    }
    val allTasks = remember(data) { data.courseTasks.filter { it.deletedAtMillis == null } }
    val matching = remember(allTasks, query) {
        if (query.isBlank()) allTasks
        else allTasks.filter { it.title.contains(query, ignoreCase = true) }
    }
    // 件数は全タブに出す（検索中は絞った件数）。
    val counts = remember(matching) {
        mapOf(
            TaskFilter.OPEN to matching.count { it.status == TaskStatus.OPEN },
            TaskFilter.DONE to matching.count { it.status != TaskStatus.OPEN },
            TaskFilter.ALL to matching.size
        )
    }
    val tasks = remember(matching, filter) {
        matching
            .filter {
                when (filter) {
                    TaskFilter.OPEN -> it.status == TaskStatus.OPEN
                    TaskFilter.DONE -> it.status != TaskStatus.OPEN
                    TaskFilter.ALL -> true
                }
            }
            .sortedWith(compareBy({ it.dueDate ?: "9999" }, { it.dueTime ?: "" }))
    }
    val today = remember { todayIso() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxHeight()) {
            TaskTabs(
                filter = filter,
                counts = counts,
                onSearch = { searchOpen = !searchOpen },
                onSelect = { filter = it }
            )

            if (searchOpen) {
                TaskSearchField(
                    query = query,
                    onQueryChange = { query = it },
                    modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.sm)
                )
            }

            LazyColumn(
                modifier = Modifier.widthIn(max = 760.dp).fillMaxHeight(),
                contentPadding = PaddingValues(
                    start = Space.lg,
                    end = Space.lg,
                    top = Space.xs,
                    bottom = screenBottomPadding(bottomInset)
                )
            ) {
                if (tasks.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = t["tasks_empty"],
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary
                            )
                        }
                    }
                } else if (filter == TaskFilter.OPEN && query.isBlank()) {
                    val groups = listOf(
                        t["tasks_overdue"] to tasks.filter { task -> task.dueDate?.let { it < today } == true },
                        t["tasks_today"] to tasks.filter { task -> task.dueDate == today },
                        t["tasks_upcoming"] to tasks.filter { task -> task.dueDate?.let { it > today } == true },
                        t["tasks_no_due"] to tasks.filter { task -> task.dueDate == null }
                    )
                    groups.forEach { (label, group) ->
                        if (group.isNotEmpty()) {
                            item(key = "group-$label") {
                                Column {
                                    org.openyaske.app.ui.design.SectionBar(label)
                                    AppCard {
                                        group.forEachIndexed { index, task ->
                                            if (index > 0) {
                                                HairLine(modifier = Modifier.padding(start = 44.dp))
                                            }
                                            TaskRow(
                                                api = api,
                                                task = task,
                                                course = coursesById[task.courseId],
                                                colorIndex = colorIndex,
                                                onCourse = onCourse,
                                                onEdit = { editing = it; editorOpen = true },
                                                onDelete = { deletingTask = it }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item(key = "all-tasks") {
                        AppCard {
                            tasks.forEachIndexed { index, task ->
                                if (index > 0) HairLine(modifier = Modifier.padding(start = 44.dp))
                                TaskRow(
                                    api = api,
                                    task = task,
                                    course = coursesById[task.courseId],
                                    colorIndex = colorIndex,
                                    onCourse = onCourse,
                                    onEdit = { editing = it; editorOpen = true },
                                    onDelete = { deletingTask = it }
                                )
                            }
                        }
                    }
                }
                if (filter == TaskFilter.OPEN) {
                    item(key = "add_task") {
                        AddTaskRow { editorOpen = true; editing = null }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = Space.lg, bottom = if (bottomInset) Layout.bottomInset else 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(androidx.compose.material3.MaterialShapes.Cookie9Sided.toShape())
                    .background(colors.primary)
                    .clickable {
                        org.openyaske.app.ui.design.tapFeedback()
                        editorOpen = true
                        editing = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(AppIcons.add, t["add_task"], tint = colors.onPrimary, modifier = Modifier.size(24.dp))
            }
        }
    }

    if (editorOpen) {
        TaskEditorSheet(
            api = api,
            task = editing,
            courses = data.courses,
            onDismiss = { editorOpen = false; editing = null }
        )
    }

    deletingTask?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingTask = null },
            title = { Text(t["task_delete"]) },
            text = { Text(target.title, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        api.deleteTask(target.id)
                        deletingTask = null
                    }
                ) { Text(t["common_delete"]) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTask = null }) { Text(t["close"]) }
            }
        )
    }
}

/** タブの下に置く検索欄。白面 + 1dp 線 + 44dp で、開いたら自動でフォーカスする。 */
@Composable
private fun TaskSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(AppShapes.control)
            .background(colors.surface)
            .border(1.dp, colors.border, AppShapes.control)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.search, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        Box(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
            if (query.isEmpty()) {
                Text(
                    text = t["search_hint"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.text),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
            )
        }
    }
}

/** 一覧の末尾に置く追加行。 */
@Composable
private fun AddTaskRow(onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Space.sm)
            .height(56.dp)
            .clip(AppShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, AppShapes.card)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.add, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        Text(
            text = LocalUiStrings.current["add_task"],
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = Space.sm)
        )
    }
}

@Composable
private fun TaskTabs(
    filter: TaskFilter,
    counts: Map<TaskFilter, Int>,
    onSearch: () -> Unit,
    onSelect: (TaskFilter) -> Unit
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val items = listOf(
        TaskFilter.OPEN to t["tasks_open"],
        TaskFilter.DONE to t["tasks_done"],
        TaskFilter.ALL to t["tasks_all"]
    )
    org.openyaske.app.ui.design.TabHeader(
        tabs = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                items.forEach { (value, label) ->
                    Column(
                        modifier = Modifier.clickable { onSelect(value) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (filter == value) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (filter == value) colors.primary else colors.text
                            )
                            val count = counts[value] ?: 0
                            if (count > 0) {
                                Text(
                                    text = "$count",
                                    style = MaterialTheme.typography.labelSmall.tabular(),
                                    color = if (filter == value) colors.primary else colors.textSecondary,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .width(if (filter == value) 26.dp else 0.dp)
                                .height(3.dp)
                                .clip(AppShapes.pill)
                                .background(if (filter == value) colors.primary else colors.surface)
                        )
                    }
                }
            }
        },
        actions = {
            CircleIconButton(
                icon = AppIcons.search,
                contentDescription = t["tasks_search"],
                onClick = onSearch,
                size = 40.dp
            )
        }
    )
    HairLine(modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun TaskRow(
    api: CoreApi,
    task: CourseTask,
    course: org.openyaske.core.model.Course?,
    colorIndex: Map<String, Int>,
    onCourse: (String) -> Unit,
    onEdit: (CourseTask) -> Unit,
    onDelete: (CourseTask) -> Unit
) {
    val colors = LocalAppColors.current
    val done = task.status == TaskStatus.DONE
    val dot = parseCourseColor(course?.color, colorIndex[course?.id] ?: 0)
    var menu by remember { mutableStateOf(false) }
    androidx.compose.material3.ListItem(
        modifier = Modifier.clickable { onEdit(task) },
        colors = androidx.compose.material3.ListItemDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            headlineColor = if (done) colors.textTertiary else colors.text,
            supportingColor = colors.textSecondary
        ),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (done) colors.primary else colors.surface)
                    .border(1.5.dp, if (done) colors.primary else colors.borderStrong, CircleShape)
                    .clickable {
                        if (!done) org.openyaske.app.ui.design.selectFeedback()
                        api.setTaskStatus(task.id, if (done) TaskStatus.OPEN else TaskStatus.DONE)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(Icons.Filled.Check, null, tint = colors.onPrimary, modifier = Modifier.size(14.dp))
                }
            }
        },
        headlineContent = {
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                textDecoration = if (done) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            val due = task.dueDate
            val meta = listOfNotNull(
                course?.name,
                due?.let { dueLabel(it, todayIso(), LocalUiStrings.current["lang"]) }
            ).joinToString(" · ")
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.labelSmall)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!done) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
                }
                Box {
                    androidx.compose.material3.IconButton(onClick = { menu = true }) {
                        Icon(
                            AppIcons.more,
                            contentDescription = LocalUiStrings.current["task_edit"],
                            tint = colors.textTertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text(LocalUiStrings.current["task_edit"]) },
                            onClick = {
                                menu = false
                                onEdit(task)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(LocalUiStrings.current["common_delete"]) },
                            onClick = {
                                menu = false
                                onDelete(task)
                            }
                        )
                    }
                }
            }
        }
    )
}
