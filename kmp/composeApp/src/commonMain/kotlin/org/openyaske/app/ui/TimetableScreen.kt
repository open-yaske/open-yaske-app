package org.openyaske.app.ui

import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.core.model.CourseKind
import org.openyaske.app.ui.design.DateField
import org.openyaske.app.ui.design.screenBottomPadding
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.CircleIconButton
import org.openyaske.app.ui.design.CoursePalette
import org.openyaske.app.ui.design.Disclosure
import org.openyaske.app.ui.design.FilterPill
import org.openyaske.app.ui.design.AppBarIconButton
import org.openyaske.app.ui.design.Layout
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Radius
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.parseCourseColor
import org.openyaske.app.ui.design.readableOn
import org.openyaske.app.ui.design.tabular
import org.openyaske.app.ui.timetable.PeriodRow
import org.openyaske.app.ui.timetable.UnscheduledCard
import org.openyaske.app.ui.timetable.WeekdayHeader
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.CoreException
import org.openyaske.core.logic.PeriodTimes
import org.openyaske.core.model.Course
import org.openyaske.core.model.TimetableSlot
import org.openyaske.core.model.Weekday
import org.openyaske.core.validation.IsoDate

@Composable
fun TimetableScreen(
    api: CoreApi,
    onCourse: (String) -> Unit,
    onSettings: () -> Unit,
    onGpa: () -> Unit,
    onManageTerms: () -> Unit,
    bottomInset: Boolean = true
) {
    val data by api.state.collectAsState()
    val settings by api.settings.collectAsState()
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    // 既定は「今日を含む学期 → 今日以降で最も近い学期 → 最新」。開始日が一番遅い学期を無条件に選ばない。
    var selectedTermId by remember(data.terms) {
        mutableStateOf(
            run {
                val today = todayIso()
                data.terms.firstOrNull { today >= it.startDate && today <= it.endDate }?.id
                    ?: data.terms.filter { it.startDate > today }.minByOrNull { it.startDate }?.id
                    ?: data.terms.lastOrNull()?.id
            }
        )
    }
    var showAdd by remember { mutableStateOf(false) }
    var newSlot by remember { mutableStateOf<Pair<Weekday, Int>?>(null) }
    var termMenu by remember { mutableStateOf(false) }
    var background by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    var backgroundVersion by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var pickBackground by remember { mutableStateOf(false) }
    var clearBackground by remember { mutableStateOf(false) }
    LaunchedEffect(backgroundVersion) {
        val bytes = org.openyaske.app.ui.timetable.TimetableBackground.load()
        background = bytes?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
    }
    // 画面上の確認用。?cell=opaque / frosted / translucent / transparent
    val forcedCell = remember {
        initialWebQuery()?.substringAfter("cell=", "")?.substringBefore('&')?.ifBlank { null }
    }
    LaunchedEffect(forcedCell) {
        val mode = when (forcedCell) {
            "opaque" -> org.openyaske.core.api.CellSurfaceMode.OPAQUE
            "frosted" -> org.openyaske.core.api.CellSurfaceMode.FROSTED
            "translucent" -> org.openyaske.core.api.CellSurfaceMode.TRANSLUCENT
            "transparent" -> org.openyaske.core.api.CellSurfaceMode.TRANSPARENT
            else -> null
        }
        if (mode != null) api.updateSettings { it.copy(timetableCellMode = mode) }
    }
    val cellMode = settings.timetableCellMode
        ?: if (background != null) org.openyaske.core.api.CellSurfaceMode.FROSTED
        else org.openyaske.core.api.CellSurfaceMode.OPAQUE
    var addUnscheduled by remember { mutableStateOf(false) }
    val view = remember(data, settings, selectedTermId) { api.timetable(selectedTermId) }
    val courses = remember(view) {
        view.cells.values.map { it.course }.distinctBy { it.id }.sortedBy { it.name }
    }
    val colorIndex = remember(courses) { courses.withIndex().associate { it.value.id to it.index } }
    val usedDays = remember(view) { view.cells.keys.map { it.weekday }.distinct().sortedBy { it.isoNumber } }
    // 設定で隠した曜日は、授業があっても出さない。
    val days = remember(view, usedDays, settings.visibleWeekdays) {
        org.openyaske.core.logic.TimetableDays.resolve(usedDays, settings.visibleWeekdays)
            .ifEmpty { listOf(Weekday.MON, Weekday.TUE, Weekday.WED, Weekday.THU, Weekday.FRI) }
    }
    val periods = remember(view) { view.periods.ifEmpty { (1..5).toList() } }
    val gpa = remember(data) { api.gpa().base.gpa }
    val today = remember { todayIso() }
    val todayWeekday = remember(today) {
        Weekday.entries.firstOrNull { it.isoNumber == IsoDate.dayOfWeekIso(today) }
    }
    val currentPeriod = remember(periods) {
        val now = nowMinutes()
        periods.firstOrNull { period ->
            val start = PeriodTimes.uecStartMinutes[period] ?: return@firstOrNull false
            val end = PeriodTimes.uecEndMinutes[period] ?: return@firstOrNull false
            now in start until end
        }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        org.openyaske.app.ui.design.TabHeader(
            leading = {
                Box {
                    FilterPill(
                        text = data.terms.firstOrNull { it.id == selectedTermId }
                            ?.let { termLabel(it, settings.locale) }
                            ?: t["timetable_term"],
                        onClick = { termMenu = true },
                        selected = true,
                        trailing = Icons.Filled.ArrowDropDown,
                        height = 38.dp
                    )
                    DropdownMenu(expanded = termMenu, onDismissRequest = { termMenu = false }) {
                        data.terms.forEach { term ->
                            DropdownMenuItem(
                                text = { Text(termLabel(term, settings.locale)) },
                                onClick = {
                                    selectedTermId = term.id
                                    termMenu = false
                                }
                            )
                        }
                        androidx.compose.material3.HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(t["term_manage"]) },
                            onClick = {
                                termMenu = false
                                onManageTerms()
                            }
                        )
                    }
                }
            },
            actions = {
                AppBarIconButton(
                    icon = AppIcons.settings,
                    contentDescription = t["settings"],
                    onClick = onSettings
                )
                Box {
                    AppBarIconButton(
                        icon = AppIcons.more,
                        contentDescription = t["timetable_display"],
                        onClick = { menuOpen = true }
                    )
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(t["timetable_background_pick"]) },
                            onClick = {
                                menuOpen = false
                                pickBackground = true
                            }
                        )
                        if (background != null) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(t["timetable_background_clear"]) },
                                onClick = {
                                    menuOpen = false
                                    clearBackground = true
                                }
                            )
                        }
                        androidx.compose.material3.HorizontalDivider()
                        org.openyaske.core.api.CellSurfaceMode.entries.forEach { mode ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = {
                                    Text(
                                        text = cellModeLabel(mode, t),
                                        fontWeight = if (mode == cellMode) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    api.updateSettings { it.copy(timetableCellMode = mode) }
                                }
                            )
                        }
                    }
                }
            }
        )

        Box(modifier = Modifier.widthIn(max = 1000.dp).fillMaxHeight()) {
            background?.let { image ->
                androidx.compose.foundation.Image(
                    bitmap = image,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                // 読みやすさのためのスクリム。モードが透明なほど濃くする。
                val scrim = when (cellMode) {
                    org.openyaske.core.api.CellSurfaceMode.TRANSPARENT -> 0.55f
                    org.openyaske.core.api.CellSurfaceMode.TRANSLUCENT -> 0.45f
                    else -> 0.35f
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(colors.background.copy(alpha = scrim))
                )
            }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = Layout.gutter,
                end = Layout.gutter,
                bottom = screenBottomPadding(bottomInset)
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                WeekdayHeader(
                    days = days,
                    today = todayWeekday,
                    showLatePeriods = settings.maxPeriod >= 6,
                    onToggleLatePeriods = { on ->
                        api.updateSettings { it.copy(maxPeriod = if (on) 7 else 5) }
                    }
                )
            }
            items(periods.size) { index ->
                val period = periods[index]
                PeriodRow(
                    api = api,
                    period = period,
                    days = days,
                    view = view,
                    colorIndex = colorIndex,
                    today = todayWeekday,
                    currentPeriod = currentPeriod,
                    mode = cellMode,
                    hasBackground = background != null,
                    onCourse = onCourse,
                    onEmptyCell = { day, p ->
                        newSlot = day to p
                        showAdd = true
                    }
                )
            }
            if (view.unscheduled.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(Space.md))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        view.unscheduled.forEach { course ->
                            UnscheduledCard(course, colorIndex[course.id] ?: 0) { onCourse(course.id) }
                        }
                    }
                }
            }
            item {
                Spacer(Modifier.height(Space.sm))
                AddCourseRow(label = t["timetable_add_unscheduled"]) {
                    newSlot = null
                    addUnscheduled = true
                    showAdd = true
                }
            }
            item {
                Spacer(Modifier.height(Space.md))
                CreditsBar(totalCredits = view.totalCredits, gpa = gpa, onClick = onGpa)
            }
            if (data.courses.isEmpty()) {
                item {
                    Text(
                        text = t["timetable_empty"],
                        color = colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = Space.md, start = 2.dp)
                    )
                }
            }
        }
        }
    }

    if (pickBackground) {
        LaunchedEffect(Unit) {
            val saved = org.openyaske.app.ui.timetable.TimetableBackground.pickAndSave()
            if (saved) backgroundVersion += 1
            pickBackground = false
        }
    }
    if (clearBackground) {
        LaunchedEffect(Unit) {
            org.openyaske.app.ui.timetable.TimetableBackground.clear()
            api.updateSettings { it.copy(timetableCellMode = null) }
            backgroundVersion += 1
            clearBackground = false
        }
    }

    if (showAdd) {
        CourseEditorSheet(
            api = api,
            course = null,
            initialWeekday = newSlot?.first,
            initialPeriod = newSlot?.second,
            initialTermId = selectedTermId,
            unscheduled = addUnscheduled,
            onDone = {
                showAdd = false
                addUnscheduled = false
            },
            onDismiss = {
                showAdd = false
                addUnscheduled = false
            }
        )
    }
}

/** 合計単位数と暫定 GPA。行全体から GPA 画面へ入れる。 */
@Composable
private fun CreditsBar(totalCredits: Int, gpa: Double, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(colors.primaryContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = Space.lg, vertical = Space.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.grades, null, tint = colors.primary, modifier = Modifier.size(20.dp))
        Column(modifier = Modifier.weight(1f).padding(start = Space.sm)) {
            Text(
                text = t["timetable_total_credits"],
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimaryContainer
            )
            Text(
                text = "$totalCredits ${t["timetable_credit_unit"]}",
                style = MaterialTheme.typography.titleMedium.tabular(),
                color = colors.primary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = t["gpa_total"],
                style = MaterialTheme.typography.bodySmall,
                color = colors.onPrimaryContainer
            )
            Text(
                text = formatGpa(gpa),
                style = MaterialTheme.typography.titleMedium.tabular(),
                color = colors.primary
            )
        }
        Disclosure(
            modifier = Modifier.padding(start = Space.xs),
            tint = colors.primary
        )
    }
}

/** GPA は小数 2 桁で見せる。 */
private fun formatGpa(gpa: Double): String {
    val cents = kotlin.math.round(gpa * 100).toInt()
    val fraction = (cents % 100).toString().padStart(2, '0')
    return "${cents / 100}.$fraction"
}

@Composable
private fun AddCourseRow(label: String, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(AppIcons.add, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}


private fun Color.toArgbHex(): String {
    val a = (alpha * 255).toInt()
    val r = (red * 255).toInt()
    val g = (green * 255).toInt()
    val b = (blue * 255).toInt()
    return ((a shl 24) or (r shl 16) or (g shl 8) or b).toUInt().toString(16).padStart(8, '0')
}

/** セル表示モードの名前。 */
private fun cellModeLabel(
    mode: org.openyaske.core.api.CellSurfaceMode,
    t: UiStrings
): String = when (mode) {
    org.openyaske.core.api.CellSurfaceMode.OPAQUE -> t["cell_mode_opaque"]
    org.openyaske.core.api.CellSurfaceMode.FROSTED -> t["cell_mode_frosted"]
    org.openyaske.core.api.CellSurfaceMode.TRANSLUCENT -> t["cell_mode_translucent"]
    org.openyaske.core.api.CellSurfaceMode.TRANSPARENT -> t["cell_mode_transparent"]
}
