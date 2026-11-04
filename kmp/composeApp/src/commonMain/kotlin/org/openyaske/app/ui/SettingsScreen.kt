package org.openyaske.app.ui

import kotlinx.coroutines.launch
import org.openyaske.app.ui.design.toAccentHex
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.ChoiceButtons
import org.openyaske.app.ui.design.MultiChoiceChips
import org.openyaske.app.ui.design.FilterPill
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.Disclosure
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.AppTopBar
import org.openyaske.app.ui.design.SectionBar
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.tabular
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.ThemeMode
import org.openyaske.core.model.Weekday

@Composable
fun SettingsScreen(
    api: CoreApi,
    onClose: () -> Unit,
    startAtItem: Int? = null
) {
    val settings by api.settings.collectAsState()
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    var showExport by remember { mutableStateOf(false) }
    var exported by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var confirmImport by remember { mutableStateOf(false) }
    var clearStep by remember { mutableStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    // カレンダーの歯車から開いたときは「カレンダー」の節まで送る。
    LaunchedEffect(startAtItem) {
        startAtItem?.let { listState.scrollToItem(it) }
    }

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        AppTopBar(title = t["settings"], onBack = onClose, modifier = Modifier.widthIn(max = 760.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.widthIn(max = 760.dp).fillMaxHeight(),
            contentPadding = PaddingValues(start = Space.lg, end = Space.lg, bottom = Space.xxl),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            item { SectionBar(t["settings_appearance"]) }
            item {
                AppCard {
                    Column(modifier = Modifier.padding(Space.md)) {
                        FieldLabel(t["settings_theme"])
                        ChoiceButtons(
                            options = listOf(
                                ThemeMode.SYSTEM to t["settings_theme_system"],
                                ThemeMode.LIGHT to t["settings_theme_light"],
                                ThemeMode.DARK to t["settings_theme_dark"],
                                ThemeMode.PURE_BLACK to t["settings_theme_black"]
                            ),
                            selected = settings.theme,
                            onSelect = { mode -> api.updateSettings { it.copy(theme = mode) } }
                        )
                        Spacer(Modifier.height(Space.md))
                        FieldLabel(t["settings_language"])
                        ChoiceButtons(
                            options = listOf(
                                "ja" to "日本語",
                                "en" to "English",
                                "zh" to "中文",
                                "ko" to "한국어"
                            ),
                            selected = settings.locale.take(2),
                            onSelect = { code -> api.updateSettings { it.copy(locale = code) } },
                            labelFont = { code -> uiFontFamily(code) }
                        )
                        Spacer(Modifier.height(Space.lg))
                        AccentPicker(
                            hue = settings.accentHue,
                            monochrome = settings.accentStyle == org.openyaske.core.api.AccentStyle.MONOCHROME,
                            onChange = { hue, mono ->
                                api.updateSettings {
                                    it.copy(
                                        accentHue = hue,
                                        accentStyle = if (mono) org.openyaske.core.api.AccentStyle.MONOCHROME
                                        else org.openyaske.core.api.AccentStyle.COLOR
                                    )
                                }
                            }
                        )
                    }
                }
            }

            item { SectionBar(t["settings_grades"]) }
            item {
                AppCard {
                    Column(modifier = Modifier.padding(Space.md)) {
                        FieldLabel(t["settings_grade_scheme"])
                        ChoiceButtons(
                            options = listOf(
                                org.openyaske.core.logic.GradeScheme.SABCD to "S A B C D",
                                org.openyaske.core.logic.GradeScheme.JAPANESE to "秀 優 良 可 不可",
                                org.openyaske.core.logic.GradeScheme.NUMERIC to "4.0 — 0.0"
                            ),
                            selected = settings.gradeScheme,
                            onSelect = { scheme ->
                                api.updateSettings { it.copy(gradeScheme = scheme) }
                            }
                        )
                    }
                }
            }

            item { SectionBar(t["settings_timetable"]) }
            item {
                AppCard {
                    Column(modifier = Modifier.padding(Space.md)) {
                        FieldLabel(t["settings_timetable_days"])
                        MultiChoiceChips(
                            options = Weekday.entries.map { it to t["day_${it.isoNumber}"] },
                            selected = settings.visibleWeekdays.toSet(),
                            onToggle = { day ->
                                val next = if (day in settings.visibleWeekdays) {
                                    settings.visibleWeekdays - day
                                } else {
                                    settings.visibleWeekdays + day
                                }
                                api.updateSettings { it.copy(visibleWeekdays = next) }
                            }
                        )
                        Spacer(Modifier.height(Space.md))
                        FieldLabel(t["settings_timetable_periods"])
                        ChoiceButtons(
                            options = (4..7).map { it to it.toString() },
                            selected = settings.maxPeriod,
                            onSelect = { period -> api.updateSettings { it.copy(maxPeriod = period) } }
                        )
                        Spacer(Modifier.height(Space.md))
                        FieldLabel(t["timetable_display"])
                        ChoiceButtons(
                            options = org.openyaske.core.api.CellSurfaceMode.entries.map { mode ->
                                mode to when (mode) {
                                    org.openyaske.core.api.CellSurfaceMode.OPAQUE -> t["cell_mode_opaque"]
                                    org.openyaske.core.api.CellSurfaceMode.FROSTED -> t["cell_mode_frosted"]
                                    org.openyaske.core.api.CellSurfaceMode.TRANSLUCENT -> t["cell_mode_translucent"]
                                    org.openyaske.core.api.CellSurfaceMode.TRANSPARENT -> t["cell_mode_transparent"]
                                }
                            },
                            selected = settings.timetableCellMode
                                ?: org.openyaske.core.api.CellSurfaceMode.OPAQUE,
                            onSelect = { mode ->
                                api.updateSettings { it.copy(timetableCellMode = mode) }
                            }
                        )
                        Spacer(Modifier.height(Space.md))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        org.openyaske.app.ui.timetable.TimetableBackground.pickAndSave()
                                    }
                                }
                            ) { Text(t["timetable_background_pick"]) }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        org.openyaske.app.ui.timetable.TimetableBackground.clear()
                                        api.updateSettings { it.copy(timetableCellMode = null) }
                                    }
                                }
                            ) { Text(t["timetable_background_clear"]) }
                        }
                    }
                }
            }

            item { SectionBar(t["settings_calendar"]) }
            item {
                AppCard {
                    Column {
                        ToggleRow(t["settings_calendar_classes"], settings.calendarShowClasses) {
                            api.updateSettings { s -> s.copy(calendarShowClasses = it) }
                        }
                        HairLine(modifier = Modifier.padding(start = Space.md))
                        ToggleRow(t["settings_calendar_tasks"], settings.calendarShowTasks) {
                            api.updateSettings { s -> s.copy(calendarShowTasks = it) }
                        }
                        HairLine(modifier = Modifier.padding(start = Space.md))
                        ToggleRow(t["settings_calendar_events"], settings.calendarShowEvents) {
                            api.updateSettings { s -> s.copy(calendarShowEvents = it) }
                        }
                    }
                }
            }

            item { SectionBar(t["transit_title"]) }
            item { TransitSettingsSection(api, settings) }

            item { SectionBar(t["weather_title"]) }
            item { WeatherSettingsSection(api, settings) }

            item { SectionBar(t["settings_notifications"]) }
            item {
                AppCard {
                    Column {
                        ToggleRow(t["settings_notifications"], settings.notifications.enabled) {
                            api.updateSettings { s -> s.copy(notifications = s.notifications.copy(enabled = it)) }
                        }
                        HairLine(modifier = Modifier.padding(start = Space.md))
                        ToggleRow(t["settings_quiet_class"], settings.notifications.quietDuringClass) {
                            api.updateSettings { s ->
                                s.copy(notifications = s.notifications.copy(quietDuringClass = it))
                            }
                        }
                        HairLine(modifier = Modifier.padding(start = Space.md))
                        ToggleRow(t["settings_task_reminder"], settings.notifications.taskReminderEnabled) {
                            api.updateSettings { s ->
                                s.copy(notifications = s.notifications.copy(taskReminderEnabled = it))
                            }
                        }
                        Spacer(Modifier.height(Space.md))
                        FieldLabel(t["settings_notifications_lead"])
                        ChoiceButtons(
                            options = listOf(5, 10, 15, 30).map { it to "$it" + t["home_minutes_suffix"] },
                            selected = settings.notifications.leadMinutes,
                            onSelect = { minutes ->
                                api.updateSettings { s ->
                                    s.copy(notifications = s.notifications.copy(leadMinutes = minutes))
                                }
                            }
                        )
                    }
                }
            }

            item { SectionBar(t["settings_attendance"]) }
            item {
                AppCard {
                    Column(modifier = Modifier.padding(Space.md)) {
                        FieldLabel(t["settings_attendance_warn"])
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${kotlin.math.round(settings.attendancePolicy.absenceWarnRatio * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium.tabular(),
                                color = colors.primary,
                                modifier = Modifier.width(56.dp)
                            )
                            androidx.compose.material3.Slider(
                                value = settings.attendancePolicy.absenceWarnRatio.toFloat(),
                                onValueChange = { value ->
                                    api.updateSettings { current ->
                                        current.copy(
                                            attendancePolicy = current.attendancePolicy.copy(
                                                absenceWarnRatio = value.toDouble()
                                            )
                                        )
                                    }
                                },
                                valueRange = 0.1f..0.5f,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item { SectionBar(t["settings_data"]) }
            item {
                AppCard {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    exported = api.exportJson()
                                    showExport = true
                                }
                                .padding(horizontal = Space.md, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                t["settings_export"],
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.text,
                                modifier = Modifier.weight(1f)
                            )
                            Disclosure()
                        }
                        HairLine(modifier = Modifier.padding(start = Space.md))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    importText = ""
                                    showImport = true
                                }
                                .padding(horizontal = Space.md, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                t["settings_import"],
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.text,
                                modifier = Modifier.weight(1f)
                            )
                            Disclosure()
                        }
                        HairLine(modifier = Modifier.padding(start = Space.md))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { clearStep = 1 }
                                .padding(horizontal = Space.md, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                t["settings_clear"],
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.danger,
                                modifier = Modifier.weight(1f)
                            )
                            Disclosure()
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(top = Space.lg)) {
                    Text("OpenYASKE", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = colors.textSecondary)
                    Text(
                        t["settings_version"],
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary
                    )
                }
            }
        }
    }

    if (message != null) {
        AlertDialog(
            onDismissRequest = { message = null },
            title = { Text(t["settings"]) },
            text = { Text(message!!) },
            confirmButton = {
                TextButton(onClick = { message = null }) { Text(t["close"]) }
            }
        )
    }

    if (showImport) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text(t["settings_import"]) },
            text = {
                Column {
                    Text(
                        t["settings_import_hint"],
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = Space.sm)
                    )
                    BasicTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        textStyle = MaterialTheme.typography.labelSmall.copy(color = colors.text),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showImport = false
                        confirmImport = true
                    }
                ) { Text(t["settings_import"]) }
            },
            dismissButton = {
                TextButton(onClick = { showImport = false }) { Text(t["close"]) }
            }
        )
    }

    if (confirmImport) {
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text(t["settings_import"]) },
            text = { Text(t["settings_import_confirm"]) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmImport = false
                        val result = runCatching { api.importJson(importText) }
                        message = result.fold(
                            onSuccess = { t["settings_import_done"] },
                            onFailure = { it.message ?: t["settings_import_done"] }
                        )
                    }
                ) { Text(t["common_save"]) }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = false }) { Text(t["close"]) }
            }
        )
    }

    if (clearStep == 1) {
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            title = { Text(t["settings_clear"]) },
            text = { Text(t["settings_clear_confirm"]) },
            confirmButton = {
                TextButton(onClick = { clearStep = 2 }) { Text(t["common_delete"]) }
            },
            dismissButton = {
                TextButton(onClick = { clearStep = 0 }) { Text(t["close"]) }
            }
        )
    }

    if (clearStep == 2) {
        AlertDialog(
            onDismissRequest = { clearStep = 0 },
            title = { Text(t["settings_clear"]) },
            text = { Text(t["settings_clear_confirm2"]) },
            confirmButton = {
                TextButton(
                    onClick = {
                        api.clearAllData()
                        clearStep = 0
                        message = t["settings_clear_done"]
                    }
                ) { Text(t["common_delete"]) }
            },
            dismissButton = {
                TextButton(onClick = { clearStep = 0 }) { Text(t["close"]) }
            }
        )
    }

    if (showExport) {
        AlertDialog(
            onDismissRequest = { showExport = false },
            title = { Text(t["settings_export"]) },
            text = {
                Column {
                    Text(
                        t["settings_export_hint"],
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(bottom = Space.sm)
                    )
                    BasicTextField(
                        value = exported,
                        onValueChange = {},
                        readOnly = true,
                        textStyle = MaterialTheme.typography.labelSmall.copy(color = colors.text),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExport = false }) { Text(t["close"]) }
            }
        )
    }
}

/**
 * アクセント。色相だけを選ぶ（カラー）か、色相を使わない（モノクロ）かの 2 択。
 * 白・黒・灰を選ぶ余地を作らないため、色そのものは保存しない。
 */
@Composable
private fun AccentPicker(hue: Int, monochrome: Boolean, onChange: (Int, Boolean) -> Unit) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current

    Column {
        FieldLabel(t["settings_accent"])
        ChoiceButtons(
            options = listOf(
                false to t["accent_color"],
                true to t["accent_mono"]
            ),
            selected = monochrome,
            onSelect = { mono -> onChange(hue, mono) }
        )
        if (!monochrome) {
            Spacer(Modifier.height(Space.md))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(Space.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                org.openyaske.app.ui.design.AccentHuePresets.forEach { preset ->
                    val swatch = org.openyaske.app.ui.design.accentColorFor(preset, colors.dark)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(swatch)
                            .then(
                                if (preset == hue) Modifier.border(
                                    2.dp,
                                    colors.text,
                                    androidx.compose.foundation.shape.CircleShape
                                ) else Modifier
                            )
                            .clickable { onChange(preset, false) }
                    )
                }
            }
            Spacer(Modifier.height(Space.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(colors.primary)
                )
                Text(
                    text = "$hue°",
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = Space.md)
                )
                androidx.compose.material3.Slider(
                    value = hue.toFloat(),
                    onValueChange = { value -> onChange(value.toInt().coerceIn(0, 359), false) },
                    valueRange = 0f..359f,
                    modifier = Modifier.weight(1f).padding(start = Space.sm),
                    colors = androidx.compose.material3.SliderDefaults.colors(
                        activeTrackColor = colors.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = FontWeight.SemiBold,
        color = LocalAppColors.current.textSecondary,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = Space.md, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.text, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
