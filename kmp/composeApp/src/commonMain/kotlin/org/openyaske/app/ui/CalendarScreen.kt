package org.openyaske.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.toShape
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import org.openyaske.app.ui.design.FilterPill
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.app.ui.calendar.DayBar
import org.openyaske.app.ui.calendar.MonthGrid
import org.openyaske.app.ui.calendar.WeekdayRow
import org.openyaske.app.ui.design.screenBottomPadding
import org.openyaske.app.ui.design.Layout
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.CircleIconButton
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.PrimaryButton
import org.openyaske.app.ui.design.Radius
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.parseCourseColor
import org.openyaske.core.api.CoreApi
import org.openyaske.core.validation.IsoDate
import org.openyaske.core.logic.AgendaItem
import org.openyaske.core.model.SchoolEvent
import org.openyaske.core.model.SchoolEventKind
import org.openyaske.core.model.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    api: CoreApi,
    onCourse: (String) -> Unit,
    onSettings: () -> Unit,
    bottomInset: Boolean = true
) {
    val data by api.state.collectAsState()
    val settings by api.settings.collectAsState()
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val today = remember { todayIso() }
    var year by remember { mutableStateOf(today.substring(0, 4).toInt()) }
    var month by remember { mutableStateOf(today.substring(5, 7).toInt()) }
    var selected by remember { mutableStateOf(today) }
    var showDay by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var detailTask by remember { mutableStateOf<org.openyaske.core.model.CourseTask?>(null) }
    var editingTask by remember { mutableStateOf<org.openyaske.core.model.CourseTask?>(null) }
    var editingEvent by remember { mutableStateOf<SchoolEvent?>(null) }

    val coursesById = remember(data) { data.courses.associateBy { it.id } }
    val colorIndex = remember(data) {
        data.courses.sortedBy { it.name }.withIndex().associate { it.value.id to it.index }
    }
    val holidayColor = colors.danger
    val examColor = colors.warning
    val academicColor = colors.info
    val otherEventColor = colors.accent

    val bars = remember(data, settings, holidayColor, examColor, academicColor, otherEventColor) {
        val map = mutableMapOf<String, MutableList<DayBar>>()
        if (settings.calendarShowClasses) {
            data.sessions.filter { it.deletedAtMillis == null }.forEach { session ->
                val course = coursesById[session.courseId] ?: return@forEach
                map.getOrPut(session.date) { mutableListOf() }
                    .add(DayBar(course.name, parseCourseColor(course.color, colorIndex[course.id] ?: 0)))
            }
        }
        if (settings.calendarShowEvents) {
            data.schoolEvents.filter { it.deletedAtMillis == null }.forEach { event ->
                map.getOrPut(event.date) { mutableListOf() }
                    .add(
                        DayBar(
                            event.title,
                            when (event.kind) {
                                SchoolEventKind.HOLIDAY -> holidayColor
                                SchoolEventKind.EXAM -> examColor
                                SchoolEventKind.ACADEMIC -> academicColor
                                else -> otherEventColor
                            }
                        )
                    )
            }
        }
        if (settings.calendarShowTasks) {
            data.courseTasks
                .filter { it.deletedAtMillis == null && it.status != TaskStatus.DONE }
                .mapNotNull { task -> task.dueDate?.let { it to task } }
                .forEach { (date, task) ->
                    map.getOrPut(date) { mutableListOf() }
                        .add(DayBar(task.title, colors.warning))
                }
        }
        map
    }

    val agenda = remember(data, selected, settings) {
        api.agenda(selected, selected).filter { entry ->
            when (entry.item) {
                is AgendaItem.Class -> settings.calendarShowClasses
                is AgendaItem.Task -> settings.calendarShowTasks
                is AgendaItem.Event -> settings.calendarShowEvents
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 800.dp
        val screenHeight = maxHeight
        val compact = screenHeight < 560.dp
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                org.openyaske.app.ui.design.TabHeader(
                    leading = {
                        FilterPill(
                            text = monthLabel(year, month, settings.locale),
                            onClick = { showDatePicker = true },
                            selected = true,
                            trailing = AppIcons.dropdown,
                            height = 38.dp
                        )
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                year = today.substring(0, 4).toInt()
                                month = today.substring(5, 7).toInt()
                                selected = today
                            }
                        ) {
                            Text(t["calendar_today"])
                        }
                        org.openyaske.app.ui.design.AppBarIconButton(
                            icon = AppIcons.settings,
                            contentDescription = t["settings"],
                            onClick = onSettings
                        )
                    }
                )

                WeekdayRow(compact = compact)

                MonthGrid(
                    year = year,
                    month = month,
                    today = today,
                    selected = selected,
                    bars = bars,
                    compact = compact,
                    availableHeight = screenHeight -
                        (if (compact) org.openyaske.app.ui.design.Sizes.headerCompact else org.openyaske.app.ui.design.Sizes.headerHeight) -
                        26.dp - screenBottomPadding(bottomInset) - (if (!wide) 44.dp else 0.dp),
                    modifier = Modifier.padding(horizontal = Space.md),
                    onSelect = { date ->
                        selected = date
                        if (!wide) showDay = true
                    }
                )

                if (!wide && agenda.isNotEmpty()) {
                    SelectedDayStrip(agenda, onCourse, onTask = { detailTask = it }, onEvent = { editingEvent = it })
                }
            }

            if (wide) {
                DayPanel(
                    date = selected,
                    agenda = agenda,
                    colorIndex = colorIndex,
                    onCourse = onCourse,
                    onTask = { detailTask = it },
                    onEvent = { editingEvent = it },
                    onAdd = { editingEvent = newEvent(selected) },
                    modifier = Modifier.width(320.dp).fillMaxHeight()
                )
            }
        }
    }

    if (showDay) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        org.openyaske.app.ui.design.SheetDismissHaptic(sheetState)
        org.openyaske.app.ui.design.ModalBackHandler { showDay = false }
        ModalBottomSheet(onDismissRequest = { showDay = false }, sheetState = sheetState) {
            Column(modifier = Modifier.padding(horizontal = Space.xl).padding(bottom = Space.xxl)) {
                Text(selected, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.text)
                Spacer(Modifier.height(Space.sm))
                AgendaList(
                    agenda = agenda,
                    colorIndex = colorIndex,
                    onCourse = onCourse,
                    onTask = { detailTask = it },
                    onEvent = { editingEvent = it },
                    emptyText = t["calendar_empty"]
                )
                Box(modifier = Modifier.padding(top = Space.md)) {
                    PrimaryButton(text = t["calendar_add_on_day"], onClick = { editingEvent = newEvent(selected) })
                }
            }
        }
    }

    detailTask?.let { task ->
        TaskDetailSheet(
            api = api,
            task = task,
            course = data.courses.firstOrNull { it.id == task.courseId },
            onEdit = {
                editingTask = task
                detailTask = null
            },
            onDismiss = { detailTask = null }
        )
    }

    editingTask?.let { task ->
        TaskEditorSheet(
            api = api,
            task = task,
            courses = data.courses,
            onDismiss = { editingTask = null }
        )
    }

    editingEvent?.let { event ->
        SchoolEventEditorSheet(
            api = api,
            event = event.takeIf { it.id.isNotBlank() },
            initialDate = event.date,
            onDone = { editingEvent = null },
            onDismiss = { editingEvent = null }
        )
    }

    if (showDatePicker) {
        val pickerState = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = IsoDate.toEpochDay(selected) * 86_400_000L
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val iso = IsoDate.fromEpochDay(millis / 86_400_000L)
                            selected = iso
                            year = iso.substring(0, 4).toInt()
                            month = iso.substring(5, 7).toInt()
                        }
                        showDatePicker = false
                    }
                ) { Text(t["calendar_go"]) }
            }
        ) {
            androidx.compose.material3.DatePicker(state = pickerState)
        }
    }


}

@Composable
private fun DayPanel(
    date: String,
    agenda: List<org.openyaske.core.logic.AgendaEntry>,
    colorIndex: Map<String, Int>,
    onCourse: (String) -> Unit,
    onTask: (org.openyaske.core.model.CourseTask) -> Unit,
    onEvent: (SchoolEvent) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    Column(
        modifier = modifier
            .padding(start = Space.sm, end = Space.lg, top = Space.lg, bottom = Space.lg)
    ) {
        Text(date, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = colors.text)
        Spacer(Modifier.height(Space.sm))
        AppCard(modifier = Modifier.weight(1f, fill = false).heightIn(max = 480.dp)) {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(Space.md)) {
                item {
                    AgendaList(agenda, colorIndex, onCourse, onTask, onEvent, t["calendar_empty"])
                }
            }
        }
        Spacer(Modifier.height(Space.sm))
        PrimaryButton(text = t["calendar_add"], onClick = onAdd)
    }
}

@Composable
private fun AgendaList(
    agenda: List<org.openyaske.core.logic.AgendaEntry>,
    colorIndex: Map<String, Int>,
    onCourse: (String) -> Unit,
    onTask: (org.openyaske.core.model.CourseTask) -> Unit,
    onEvent: (SchoolEvent) -> Unit,
    emptyText: String
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    if (agenda.isEmpty()) {
        Text(emptyText, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        return
    }
    Column {
        agenda.forEach { entry ->
            when (val item = entry.item) {
                is AgendaItem.Class -> SheetRow(
                    title = item.course.name,
                    meta = listOfNotNull(timeLabel(item.session), item.session.room).joinToString(" · "),
                    color = parseCourseColor(item.course.color, colorIndex[item.course.id] ?: 0)
                ) { onCourse(item.course.id) }

                is AgendaItem.Task -> SheetRow(
                    title = item.task.title,
                    meta = item.course?.name ?: "",
                    color = colors.warning
                ) { onTask(item.task) }

                is AgendaItem.Event -> SheetRow(
                    title = item.event.title,
                    meta = t["event_label"],
                    color = colors.info
                ) { onEvent(item.event) }
            }
        }
    }
}

@Composable
private fun SelectedDayStrip(
    agenda: List<org.openyaske.core.logic.AgendaEntry>,
    onCourse: (String) -> Unit,
    onTask: (org.openyaske.core.model.CourseTask) -> Unit,
    onEvent: (SchoolEvent) -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(horizontal = Space.lg, vertical = Space.sm),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        agenda.take(3).forEach { entry ->
            val (label, color) = when (val item = entry.item) {
                is AgendaItem.Class -> item.course.name to colors.accent
                is AgendaItem.Task -> item.task.title to colors.warning
                is AgendaItem.Event -> item.event.title to colors.info
            }
            Row(
                modifier = Modifier
                    .clip(AppShapes.control)
                    .background(colors.surface)
                    .clickable {
                        when (val item = entry.item) {
                            is AgendaItem.Class -> onCourse(item.course.id)
                            is AgendaItem.Task -> onTask(item.task)
                            is AgendaItem.Event -> onEvent(item.event)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(color))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.text,
                    modifier = Modifier.padding(start = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SheetRow(title: String, meta: String, color: Color, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.width(3.dp).height(30.dp).clip(AppShapes.pill).background(color))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.text)
            if (meta.isNotBlank()) {
                Text(meta, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.text, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

/** 新規の予定。id が空なので、シートは作成として動く。 */
private fun newEvent(date: String) = SchoolEvent(
    id = "",
    title = "",
    kind = SchoolEventKind.ACADEMIC,
    date = date
)
