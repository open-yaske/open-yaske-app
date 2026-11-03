package org.openyaske.app.ui

import androidx.compose.foundation.layout.imePadding
import org.openyaske.core.model.Weekday
import org.openyaske.core.model.SyllabusEntry
import org.openyaske.core.api.SyllabusSearchHit
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.CoursePalette
import org.openyaske.app.ui.design.FilterPill
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.ModalBackHandler
import org.openyaske.app.ui.design.SheetDismissHaptic
import org.openyaske.app.ui.design.PrimaryButton
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.parseCourseColor
import org.openyaske.app.ui.design.toAccentHex
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.CoreException
import org.openyaske.core.model.Course
import org.openyaske.core.model.CourseKind
import org.openyaske.core.model.Term

/**
 * 科目の編集。値はすべて `api.saveCourse` で書き戻し、削除は既存の論理削除を使う。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseEditorSheet(
    api: CoreApi,
    course: Course?,
    initialWeekday: Weekday? = null,
    initialPeriod: Int? = null,
    initialTermId: String? = null,
    unscheduled: Boolean = false,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val isNew = course == null
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val data by api.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SheetDismissHaptic(sheetState)

    var name by remember { mutableStateOf(course?.name ?: "") }
    var room by remember { mutableStateOf(course?.room ?: "") }
    var instructor by remember { mutableStateOf(course?.instructor ?: "") }
    var code by remember { mutableStateOf(course?.code ?: "") }
    var credits by remember { mutableStateOf((course?.credits ?: 2).toString()) }
    var colorToken by remember { mutableStateOf(course?.color ?: CoursePalette[0].toAccentHex()) }
    var termId by remember { mutableStateOf(course?.termId ?: initialTermId) }
    var kind by remember {
        mutableStateOf(if (unscheduled) CourseKind.EXTRA else course?.kind ?: CourseKind.REGULAR)
    }
    var weekday by remember { mutableStateOf(initialWeekday ?: course?.weekday) }
    var period by remember { mutableStateOf(initialPeriod ?: course?.period) }
    var memo by remember { mutableStateOf(course?.memo ?: "") }
    var url by remember { mutableStateOf(course?.url ?: "") }
    var showSyllabus by remember { mutableStateOf(false) }
    var termMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val terms = data.terms
    val selectedTerm = terms.firstOrNull { it.id == termId }
    val kinds = listOf(
        CourseKind.REGULAR to t["kind_regular"],
        CourseKind.EXTRA to t["kind_extra"],
        CourseKind.INTENSIVE to t["kind_intensive"],
        CourseKind.PARTNER to t["kind_partner"]
    )

    ModalBackHandler(onDismiss)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.xl)
                .padding(bottom = Space.xxl)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Text(
                text = if (isNew) t["add_course"] else t["course_edit"],
                style = MaterialTheme.typography.titleLarge,
                color = colors.text
            )
            if (isNew && !showSyllabus) {
                TextButton(onClick = { showSyllabus = true }, contentPadding = PaddingValues(0.dp)) {
                    Text(t["course_find_syllabus"])
                }
            }
            if (isNew && showSyllabus) {
                SyllabusInlineSearch(
                    api = api,
                    initialWeekday = initialWeekday,
                    initialPeriod = initialPeriod,
                    onPick = { entry ->
                        name = entry.title
                        code = entry.code
                        instructor = entry.instructor
                        room = entry.room ?: room
                        weekday = entry.weekday ?: weekday
                        period = entry.period ?: period
                        url = syllabusUrl(entry.year, entry.code) ?: url
                        showSyllabus = false
                    },
                    onClose = { showSyllabus = false }
                )
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(t["course_name"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text(t["course_room"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = instructor,
                onValueChange = { instructor = it },
                label = { Text(t["course_instructor"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text(t["course_code"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = credits,
                onValueChange = { credits = it.filter { ch -> ch.isDigit() }.take(2) },
                label = { Text(t["timetable_credits"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(t["course_color"], style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoursePalette.forEach { swatch ->
                    val token = "custom:${swatch.toAccentHex()}"
                    val selected = colorToken.equals(token, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(swatch)
                            .then(
                                if (selected) Modifier.border(2.dp, colors.text, CircleShape)
                                else Modifier
                            )
                            .clickable { colorToken = token }
                    )
                }
            }

            Text(t["timetable_term"], style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Box {
                FilterPill(
                    text = selectedTerm?.let { termLabel(it, t["lang"]) } ?: t["timetable_term"],
                    onClick = { termMenu = true },
                    trailing = org.openyaske.app.ui.design.AppIcons.dropdown
                )
                DropdownMenu(expanded = termMenu, onDismissRequest = { termMenu = false }) {
                    terms.forEach { term ->
                        DropdownMenuItem(
                            text = { Text(termLabel(term, t["lang"])) },
                            onClick = {
                                termId = term.id
                                termMenu = false
                            }
                        )
                    }
                }
            }

            if (weekday != null || period != null) {
                Row(
                    modifier = Modifier
                        .clip(AppShapes.pill)
                        .background(colors.primaryContainer)
                        .padding(horizontal = Space.md, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = listOfNotNull(
                            weekday?.let { t["day_${it.isoNumber}"] },
                            period?.let { "$it" + t["search_period_suffix"] }
                        ).joinToString(" "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onPrimaryContainer
                    )
                }
            }
            Text(t["course_kind"], style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                kinds.forEach { (value, label) ->
                    FilterPill(
                        text = label,
                        onClick = { kind = value },
                        selected = kind == value
                    )
                }
            }

            OutlinedTextField(
                value = memo,
                onValueChange = { memo = it },
                label = { Text(t["course_memo"]) },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(t["course_link"]) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (course?.syllabusId != null) {
                TextButton(
                    onClick = {
                        runCatching {
                            uriHandler.openUri("https://kyoumu.office.uec.ac.jp/syllabus/")
                        }
                    }
                ) { Text(t["course_syllabus_open"]) }
            }
            if (url.isNotBlank()) {
                TextButton(
                    onClick = { runCatching { uriHandler.openUri(url) } }
                ) { Text(t["course_link_open"]) }
            }

            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.danger)
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = Space.sm),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isNew) {
                    TextButton(
                        onClick = { confirmDelete = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(t["course_delete"], color = colors.danger)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }
                PrimaryButton(
                    text = t["common_save"],
                    onClick = {
                        if (name.isBlank()) {
                            error = t["course_name"]
                            return@PrimaryButton
                        }
                        try {
                            val extra = kind == CourseKind.EXTRA || kind == CourseKind.INTENSIVE
                            val base = course ?: Course(id = "", name = name.trim())
                            api.saveCourse(
                                base.copy(
                                    name = name.trim(),
                                    room = room.ifBlank { null },
                                    instructor = instructor.ifBlank { null },
                                    code = code.ifBlank { null },
                                    credits = credits.toIntOrNull() ?: base.credits,
                                    color = colorToken,
                                    termId = termId,
                                    kind = kind,
                                    weekday = if (extra) null else weekday,
                                    period = if (extra) null else period,
                                    memo = memo.ifBlank { null },
                                    url = url.ifBlank { null }
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
            title = { Text(t["course_delete"]) },
            text = { Text(t["course_delete_confirm"], style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(
                    onClick = {
                        course?.let { api.deleteCourse(it.id) }
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

/** シートの中で完結するシラバス検索。選ぶと科目の項目を埋める。 */
@Composable
private fun SyllabusInlineSearch(
    api: CoreApi,
    initialWeekday: Weekday?,
    initialPeriod: Int?,
    onPick: (SyllabusEntry) -> Unit,
    onClose: () -> Unit
) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    var query by remember { mutableStateOf("") }
    var hits by remember { mutableStateOf<List<SyllabusSearchHit>>(emptyList()) }
    // コマから開いたときは、その曜日・時限で絞った状態から始める。
    var filterSlot by remember { mutableStateOf(initialWeekday != null && initialPeriod != null) }
    val filtered = remember(hits, filterSlot, initialWeekday, initialPeriod) {
        if (!filterSlot) hits
        else hits.filter { hit ->
            (initialWeekday == null || hit.entry.weekday == initialWeekday) &&
                (initialPeriod == null || hit.entry.period == initialPeriod)
        }
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            hits = emptyList()
            return@LaunchedEffect
        }
        delay(200)
        hits = runCatching { api.searchSyllabus(query, limit = 20) }.getOrDefault(emptyList())
    }

    Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(t["search_hint"]) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (initialWeekday != null && initialPeriod != null) {
            FilterPill(
                text = listOfNotNull(
                    t["day_${initialWeekday.isoNumber}"],
                    "$initialPeriod" + t["search_period_suffix"]
                ).joinToString(" "),
                onClick = { filterSlot = !filterSlot },
                selected = filterSlot,
                trailing = if (filterSlot) org.openyaske.app.ui.design.AppIcons.collapse else null
            )
        }
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp)) {
            items(filtered.size) { index ->
                val entry = filtered[index].entry
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(entry) }
                        .padding(vertical = Space.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            text = listOfNotNull(
                                entry.code.ifBlank { null },
                                entry.weekday?.let { t["day_${it.isoNumber}"] },
                                entry.period?.let { "$it" + t["search_period_suffix"] },
                                entry.instructor.ifBlank { null }
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
        TextButton(onClick = onClose) { Text(t["close"]) }
    }
}

/** Svelte 版と同じ形式のシラバス URL。 */
private fun syllabusUrl(year: Int, code: String): String? {
    if (code.isBlank()) return null
    return "https://kyoumu.office.uec.ac.jp/syllabus/$year/31/31_$code.html"
}
