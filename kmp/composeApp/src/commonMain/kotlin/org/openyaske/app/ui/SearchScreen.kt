package org.openyaske.app.ui

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.app.ui.design.screenBottomPadding
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.AppTopBar
import org.openyaske.app.ui.design.FilterPill
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.Disclosure
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.PrimaryButton
import org.openyaske.app.ui.design.Radius
import org.openyaske.app.ui.design.Space
import org.openyaske.core.api.CoreApi
import org.openyaske.core.model.Course
import org.openyaske.core.model.SyllabusEntry

@Composable
fun SearchScreen(
    api: CoreApi,
    onCourse: (String) -> Unit,
    bottomInset: Boolean = true,
    onBack: (() -> Unit)? = null
) {
    val count by api.syllabusCount.collectAsState()
    val data by api.state.collectAsState()
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    var query by remember { mutableStateOf("") }
    var year by remember { mutableStateOf<Int?>(null) }
    var semester by remember { mutableStateOf<String?>(null) }
    var weekday by remember { mutableStateOf<org.openyaske.core.model.Weekday?>(null) }
    var period by remember { mutableStateOf<Int?>(null) }
    var program by remember { mutableStateOf<String?>(null) }
    var yearMenu by remember { mutableStateOf(false) }
    var semesterMenu by remember { mutableStateOf(false) }
    var weekdayMenu by remember { mutableStateOf(false) }
    var periodMenu by remember { mutableStateOf(false) }
    var programMenu by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<org.openyaske.core.api.SyllabusSearchHit>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var selectedEntry by remember { mutableStateOf<SyllabusEntry?>(null) }

    val added = remember(data) { data.courses.mapNotNull { it.syllabusId }.toSet() }
    val latestTerm = remember(data) { data.terms.lastOrNull() }
    val defaultTermId = remember(data) { api.resolveTermId(todayIso()) }
    // 入力のたびに全件を走査しないよう、200ms 待ってから検索する。
    LaunchedEffect(query, year, semester, weekday, period, program, count) {
        if (query.isBlank()) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        kotlinx.coroutines.delay(200)
        results = api.searchSyllabus(
            query = query,
            year = year,
            weekday = weekday,
            period = period,
            programType = program,
            limit = 40
        ).filter { hit -> semester == null || semesterMatches(hit.entry, semester!!) }
        searching = false
    }

    Column(
        modifier = Modifier.fillMaxSize().imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 760.dp).fillMaxWidth()) {
            if (onBack != null) {
                AppTopBar(title = t["more_search"], onBack = onBack)
            }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.lg)
                .padding(top = Space.md, bottom = Space.sm)
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
                    Text(t["search_hint"], style = MaterialTheme.typography.bodyMedium, color = colors.textTertiary)
                }
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.text)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                FilterPill(
                    text = year?.toString() ?: t["search_all_years"],
                    onClick = { yearMenu = true },
                    trailing = AppIcons.dropdown
                )
                DropdownMenu(expanded = yearMenu, onDismissRequest = { yearMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(t["search_all_years"]) },
                        onClick = { year = null; yearMenu = false }
                    )
                    (2020..2026).reversed().forEach { y ->
                        DropdownMenuItem(
                            text = { Text(y.toString()) },
                            onClick = { year = y; yearMenu = false }
                        )
                    }
                }
            }
            Box {
                FilterPill(
                    text = semester ?: t["search_all_terms"],
                    onClick = { semesterMenu = true },
                    trailing = AppIcons.dropdown
                )
                DropdownMenu(expanded = semesterMenu, onDismissRequest = { semesterMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(t["search_all_terms"]) },
                        onClick = { semester = null; semesterMenu = false }
                    )
                    listOf("前学期", "後学期", "通年").forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s) },
                            onClick = { semester = s; semesterMenu = false }
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Space.lg, vertical = Space.xs),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                FilterPill(
                    text = weekday?.let { t["day_${it.isoNumber}"] } ?: t["search_weekday"],
                    onClick = { weekdayMenu = true },
                    trailing = AppIcons.dropdown,
                    selected = weekday != null
                )
                DropdownMenu(expanded = weekdayMenu, onDismissRequest = { weekdayMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(t["search_all"]) },
                        onClick = { weekday = null; weekdayMenu = false }
                    )
                    org.openyaske.core.model.Weekday.entries.take(6).forEach { day ->
                        DropdownMenuItem(
                            text = { Text(t["day_${day.isoNumber}"]) },
                            onClick = { weekday = day; weekdayMenu = false }
                        )
                    }
                }
            }
            Box {
                FilterPill(
                    text = period?.let { "$it" + t["search_period_suffix"] } ?: t["search_period"],
                    onClick = { periodMenu = true },
                    trailing = AppIcons.dropdown,
                    selected = period != null
                )
                DropdownMenu(expanded = periodMenu, onDismissRequest = { periodMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(t["search_all"]) },
                        onClick = { period = null; periodMenu = false }
                    )
                    (1..7).forEach { p ->
                        DropdownMenuItem(
                            text = { Text("$p" + t["search_period_suffix"]) },
                            onClick = { period = p; periodMenu = false }
                        )
                    }
                }
            }
            Box {
                FilterPill(
                    text = program ?: t["search_program"],
                    onClick = { programMenu = true },
                    trailing = AppIcons.dropdown,
                    selected = program != null
                )
                DropdownMenu(expanded = programMenu, onDismissRequest = { programMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(t["search_all"]) },
                        onClick = { program = null; programMenu = false }
                    )
                    listOf("昼間", "夜間", "修士", "博士").forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p) },
                            onClick = { program = p; programMenu = false }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Space.sm))

        when {
            query.isBlank() -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.lg,
                    end = Space.lg,
                    bottom = screenBottomPadding(bottomInset)
                )
            ) {
                item {
                    Text(
                        text = t["search_enrolled"],
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = Space.md, bottom = 4.dp)
                    )
                }
                items(data.courses.size) { index ->
                    val course = data.courses[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCourse(course.id) }
                            .padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(AppShapes.pill)
                                .background(
                                    org.openyaske.app.ui.design.parseCourseColor(course.color, index)
                                )
                                .height(28.dp)
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(
                                course.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOfNotNull(
                                    course.weekday?.let { t["day_${it.isoNumber}"] },
                                    course.period?.let { "${it}限" },
                                    course.room
                                ).joinToString("  "),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }
                        Disclosure()
                    }
                    if (index != data.courses.lastIndex) HairLine()
                }
            }

            searching -> Row(
                modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                androidx.compose.material3.LoadingIndicator(modifier = Modifier.size(16.dp))
                Text(
                    t["searching"],
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            }

            results.isEmpty() -> Text(
                t["search_empty"],
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = Space.lg, vertical = Space.lg)
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Space.lg,
                    end = Space.lg,
                    bottom = screenBottomPadding(bottomInset)
                )
            ) {
                items(results.size) { index ->
                    val hit = results[index]
                    val entry = hit.entry
                    val isAdded = entry.id in added
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEntry = entry }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                            Text(
                                text = entry.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.text,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    AppIcons.grades,
                                    null,
                                    tint = colors.textTertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = listOfNotNull(
                                        entry.weekday?.let { t["day_${it.isoNumber}"] },
                                        entry.period?.let { "${it}限" },
                                        entry.instructor.ifBlank { null }
                                    ).joinToString("  "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(start = 5.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (isAdded) {
                            Text(t["search_added"], style = MaterialTheme.typography.bodySmall, color = colors.textTertiary)
                        } else {
                            PrimaryButton(
                                text = t["search_add"],
                                onClick = {
                                    addFromSyllabus(api, entry, defaultTermId ?: latestTerm?.id)
                                },
                                height = 36.dp
                            )
                        }
                    }
                    if (index != results.lastIndex) HairLine()
                }
            }
        }
        }
    }

    selectedEntry?.let { entry ->
        SyllabusSheet(
            entry = entry,
            added = entry.id in added,
            onAdd = {
                addFromSyllabus(api, entry, defaultTermId ?: latestTerm?.id)
                selectedEntry = null
            },
            onDismiss = { selectedEntry = null }
        )
    }
}

/** 学期の表記ゆれ（前学期/春/spring など）を同じ意味に寄せる。 */
private fun normalizeSemester(value: String): String {
    val v = value.trim()
    return when {
        v.contains("前") || v.contains("春") || v.contains("spring", ignoreCase = true) -> "spring"
        v.contains("後") || v.contains("秋") || v.contains("fall", ignoreCase = true) -> "fall"
        v.contains("通") || v.contains("年") -> "full"
        else -> v
    }
}

private fun semesterMatches(entry: SyllabusEntry, filter: String): Boolean {
    val target = normalizeSemester(filter)
    return listOfNotNull(entry.semester, entry.semester1, entry.semester2)
        .any { normalizeSemester(it) == target }
}

private fun addFromSyllabus(api: CoreApi, entry: SyllabusEntry, termId: String?) {
    api.saveCourse(
        course = Course(
            id = "",
            name = entry.title,
            code = entry.code,
            instructor = entry.instructor.ifBlank { null },
            room = entry.room,
            weekday = entry.weekday,
            period = entry.period,
            credits = 2,
            termId = termId,
            syllabusId = entry.id,
            color = "custom:#${
                org.openyaske.app.ui.design.CoursePalette[entry.title.length % 14]
                    .let { c ->
                        val a = (c.alpha * 255).toInt()
                        val r = (c.red * 255).toInt()
                        val g = (c.green * 255).toInt()
                        val b = (c.blue * 255).toInt()
                        ((a shl 24) or (r shl 16) or (g shl 8) or b).toUInt().toString(16).padStart(8, '0')
                    }
            }"
        ),
        today = todayIso()
    )
}
