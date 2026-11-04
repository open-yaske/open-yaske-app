package org.openyaske.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import org.openyaske.app.ui.design.tabular
import org.openyaske.app.ui.design.AppCard
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.Disclosure
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.ModalBackHandler
import org.openyaske.app.ui.design.SheetDismissHaptic
import org.openyaske.app.ui.design.Radius
import org.openyaske.app.ui.design.SectionBar
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.parseCourseColor
import org.openyaske.app.ui.design.tintOf
import org.openyaske.core.api.CoreApi
import org.openyaske.core.model.AttendanceStatus
import org.openyaske.core.model.SessionStatus
import org.openyaske.core.model.TaskStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CourseDetailSheet(api: CoreApi, courseId: String, onDismiss: () -> Unit) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val data by api.state.collectAsState()
    val detail = remember(courseId, data) { api.courseDetail(courseId) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SheetDismissHaptic(sheetState)
    val today = remember { todayIso() }
    val gradeScheme = api.settings.collectAsState().value.gradeScheme
    val courseColor = parseCourseColor(detail.course.color, 0)
    val summary = detail.attendance
    val todaySession = detail.sessions.firstOrNull { it.date == today && it.status != SessionStatus.CANCELLED }
    var showEditor by remember { mutableStateOf(false) }
    var detailTask by remember { mutableStateOf<org.openyaske.core.model.CourseTask?>(null) }
    var editTask by remember { mutableStateOf<org.openyaske.core.model.CourseTask?>(null) }
    var attendanceSession by remember { mutableStateOf<org.openyaske.core.model.CourseSession?>(null) }
    var attendanceDate by remember { mutableStateOf<String?>(null) }
    var showMakeup by remember { mutableStateOf(false) }
    val attendanceState = remember(courseId, data) { api.attendance(courseId) }
    val recordsBySession = remember(attendanceState.records) {
        attendanceState.records.associateBy { it.sessionId }
    }

    // 編集は同じ位置でシートを差し替える（詳細 → 編集 → 閉じる）。
    if (showEditor) {
        CourseEditorSheet(
            api = api,
            course = detail.course,
            onDone = {
                showEditor = false
                onDismiss()
            },
            onDismiss = { showEditor = false }
        )
        return
    }

    ModalBackHandler(onDismiss = onDismiss)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.xl)
                .padding(bottom = Space.xxl),
            verticalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(10.dp).clip(CircleShape).background(courseColor)
                )
                Text(
                    text = detail.course.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.text,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                androidx.compose.material3.IconButton(onClick = { showEditor = true }) {
                    Icon(
                        org.openyaske.app.ui.design.AppIcons.edit,
                        contentDescription = t["course_edit"],
                        tint = colors.textSecondary
                    )
                }
            }
            Text(
                text = listOfNotNull(
                    detail.course.weekday?.let { t["day_${it.isoNumber}"] },
                    detail.course.period?.let { "${it}限" },
                    "${detail.course.credits} ${t["timetable_credit_unit"]}",
                    detail.course.instructor
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            SectionBar(t["course_attendance"], modifier = Modifier.padding(top = Space.sm)) {
                Text(
                    text = "${summary.total}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.sectionBarText
                )
            }
            AppCard {
                Column(modifier = Modifier.padding(Space.md)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${summary.rate.roundToInt()}%",
                            style = MaterialTheme.typography.headlineLarge.tabular(),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary
                        )
                        Text(
                            text = t["attendance_rate"],
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(start = 6.dp, bottom = 6.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Space.sm)
                            .height(6.dp)
                            .clip(AppShapes.pill)
                            .background(colors.sunken)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((summary.rate / 100.0).toFloat().coerceIn(0f, 1f))
                                .height(6.dp)
                                .clip(AppShapes.pill)
                                .background(if (summary.rate < 80) colors.danger else colors.success)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = Space.xs),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${t["attendance_absent"]} ${summary.absent} / ${t["attendance_limit"]} ${summary.absenceLimit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (summary.atRisk) colors.danger else colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        if (summary.atRisk) {
                            Text(
                                text = t["attendance_at_risk"],
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.danger
                            )
                        }
                    }
                    AttendanceBar(t["attendance_present"], summary.present, summary.total, colors.success)
                    AttendanceBar(t["attendance_absent"], summary.absent, summary.total, colors.danger)
                    AttendanceBar(t["attendance_late"], summary.late, summary.total, colors.warning)
                    AttendanceBar(t["attendance_excused"], summary.excused, summary.total, colors.info)
                    AttendanceBar(t["attendance_cancelled"], summary.cancelled, summary.total, colors.textTertiary)
                }
            }

            SectionBar(t["course_attendance_today"], modifier = Modifier.padding(top = Space.sm))
            if (todaySession == null) {
                Text(
                    text = t["attendance_today_hint"],
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 授業回がある日はその回に、無い日は今日の日付に記録する。
                fun record(status: AttendanceStatus) {
                    if (todaySession != null) {
                        api.setAttendance(detail.course.id, status, sessionId = todaySession.id)
                    } else {
                        api.setAttendance(detail.course.id, status, date = today)
                    }
                }
                AttendanceAction(Icons.Filled.Check, t["attendance_present"], colors.success) {
                    record(AttendanceStatus.PRESENT)
                }
                AttendanceAction(Icons.Filled.Close, t["attendance_absent"], colors.danger) {
                    record(AttendanceStatus.ABSENT)
                }
                AttendanceAction(Icons.Filled.Schedule, t["attendance_late"], colors.warning) {
                    record(AttendanceStatus.LATE)
                }
                AttendanceAction(Icons.Filled.PriorityHigh, t["attendance_excused"], colors.info) {
                    record(AttendanceStatus.EXCUSED)
                }
            }

            SectionBar(t["course_sessions"], modifier = Modifier.padding(top = Space.sm))
            AppCard {
                Column {
                    if (detail.sessions.isEmpty()) {
                        Text(
                            t["course_no_sessions"],
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(Space.md)
                        )
                    }
                    detail.sessions.forEachIndexed { index, session ->
                        if (index > 0) HairLine(modifier = Modifier.padding(start = Space.md))
                        val record = recordsBySession[session.id]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    attendanceSession = session
                                    attendanceDate = null
                                }
                                .padding(horizontal = Space.md, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(session.date, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            Text(
                                text = timeLabel(session),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = record?.let { statusLabel(it.status, t) }
                                    ?: when (session.status) {
                                        SessionStatus.CANCELLED -> t["attendance_cancelled"]
                                        SessionStatus.MOVED -> t["session_moved"]
                                        SessionStatus.SCHEDULED -> t["session_kind_${session.kind.name.lowercase()}"]
                                    },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (record != null) colors.primary else colors.textTertiary
                            )
                            Disclosure()
                        }
                    }
                }
            }

            TextButton(onClick = { showMakeup = true }) {
                Text(t["makeup_add"])
            }
            val dateOnly = attendanceState.records.filter { it.sessionId == null }
            if (dateOnly.isNotEmpty()) {
                SectionBar(t["attendance_by_date"], modifier = Modifier.padding(top = Space.xs))
                AppCard {
                    Column {
                        dateOnly.forEachIndexed { index, record ->
                            if (index > 0) HairLine(modifier = Modifier.padding(start = Space.md))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        attendanceSession = null
                                        attendanceDate = record.date
                                    }
                                    .padding(horizontal = Space.md, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(record.date, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    text = statusLabel(record.status, t),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primary
                                )
                                Disclosure()
                            }
                        }
                    }
                }
            }

            SectionBar(t["course_tasks"], modifier = Modifier.padding(top = Space.sm))
            AppCard {
                Column {
                    if (detail.tasks.isEmpty()) {
                        Text(
                            t["course_no_tasks"],
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(Space.md)
                        )
                    }
                    detail.tasks.forEachIndexed { index, task ->
                        if (index > 0) HairLine(modifier = Modifier.padding(start = Space.md))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { detailTask = task }
                                .padding(horizontal = Space.md, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (task.status == TaskStatus.DONE) colors.textTertiary else colors.text,
                                    fontWeight = FontWeight.Medium
                                )
                                task.dueDate?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
                                }
                            }
                            if (task.status != TaskStatus.DONE) {
                                Disclosure()
                            }
                        }
                    }
                }
            }

            SectionBar(t["course_grade"], modifier = Modifier.padding(top = Space.sm))
            GradePicker(
                current = detail.grade?.grade,
                scheme = gradeScheme,
                onSelect = { grade ->
                    api.saveGrade(
                        org.openyaske.core.model.CourseGrade(
                            courseId = detail.course.id,
                            state = org.openyaske.core.model.GradeState.GRADED,
                            grade = grade
                        )
                    )
                }
            )
        }
    }

    (attendanceSession ?: attendanceDate)?.let { target ->
        AttendanceRecordSheet(
            api = api,
            courseId = detail.course.id,
            session = attendanceSession,
            date = attendanceDate,
            current = attendanceSession?.let { recordsBySession[it.id]?.status }
                ?: attendanceDate?.let { day -> attendanceState.records.firstOrNull { it.sessionId == null && it.date == day }?.status },
            onDone = {
                attendanceSession = null
                attendanceDate = null
            },
            onDismiss = {
                attendanceSession = null
                attendanceDate = null
            }
        )
    }

    if (showMakeup) {
        MakeupSessionSheet(
            api = api,
            courseId = detail.course.id,
            onDone = { showMakeup = false },
            onDismiss = { showMakeup = false }
        )
    }

    detailTask?.let { task ->
        TaskDetailSheet(
            api = api,
            task = task,
            course = detail.course,
            onEdit = {
                editTask = task
                detailTask = null
            },
            onDismiss = { detailTask = null }
        )
    }

    editTask?.let { task ->
        TaskEditorSheet(
            api = api,
            task = task,
            courses = api.state.value.courses,
            onDismiss = { editTask = null }
        )
    }
}

@Composable
private fun AttendanceBar(label: String, count: Int, total: Int, color: Color) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = colors.text, modifier = Modifier.width(60.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(5.dp)
                .clip(AppShapes.pill)
                .background(colors.sunken)
        ) {
            val ratio = if (total == 0) 0f else count.toFloat() / total
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio.coerceIn(0f, 1f))
                    .height(5.dp)
                    .background(color)
            )
        }
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            modifier = Modifier.width(28.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun AttendanceAction(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(AppShapes.control)
                .background(tintOf(color, !colors.dark))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** 出席状態の表示名。 */
private fun statusLabel(status: AttendanceStatus, t: UiStrings): String = when (status) {
    AttendanceStatus.PRESENT -> t["attendance_present"]
    AttendanceStatus.ABSENT -> t["attendance_absent"]
    AttendanceStatus.LATE -> t["attendance_late"]
    AttendanceStatus.EXCUSED -> t["attendance_excused"]
    AttendanceStatus.CANCELLED -> t["attendance_cancelled"]
}
