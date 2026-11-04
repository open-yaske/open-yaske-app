package org.openyaske.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.openyaske.app.ui.design.FilterPill
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.ModalBackHandler
import org.openyaske.app.ui.design.SheetDismissHaptic
import org.openyaske.app.ui.design.Space
import org.openyaske.core.api.CoreApi
import org.openyaske.core.model.AttendanceStatus
import org.openyaske.core.model.CourseSession

/**
 * 1 回分の出席を記録・変更する。
 *
 * 授業回があればその回に、無ければ日付に紐づける（時間割外の科目・過去の調整用）。
 * 記録を消すと集計からも外れる。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceRecordSheet(
    api: CoreApi,
    courseId: String,
    session: CourseSession?,
    date: String?,
    current: AttendanceStatus?,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var error by remember { mutableStateOf<String?>(null) }
    val targetDate = session?.date ?: date ?: todayIso()

    fun record(status: AttendanceStatus) {
        try {
            if (session != null) {
                api.setAttendance(courseId, status, sessionId = session.id)
            } else {
                api.setAttendance(courseId, status, date = targetDate)
            }
            onDone()
        } catch (e: Exception) {
            error = e.message
        }
    }

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
            Text(
                text = listOfNotNull(targetDate, session?.let { timeLabel(it) }).joinToString("  "),
                style = MaterialTheme.typography.titleMedium,
                color = colors.text
            )
            Text(
                text = current?.let { label(it, t) } ?: t["attendance_not_recorded"],
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                listOf(
                    AttendanceStatus.PRESENT to t["attendance_present"],
                    AttendanceStatus.ABSENT to t["attendance_absent"],
                    AttendanceStatus.LATE to t["attendance_late"],
                    AttendanceStatus.EXCUSED to t["attendance_excused"]
                ).forEach { (status, text) ->
                    FilterPill(
                        text = text,
                        onClick = { record(status) },
                        selected = current == status
                    )
                }
            }
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.danger)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                if (current != null) {
                    TextButton(
                        onClick = {
                            try {
                                if (session != null) {
                                    api.removeAttendance(courseId, sessionId = session.id)
                                } else {
                                    api.removeAttendance(courseId, date = targetDate)
                                }
                                onDone()
                            } catch (e: Exception) {
                                error = e.message
                            }
                        }
                    ) { Text(t["attendance_clear"], color = colors.danger) }
                }
                TextButton(onClick = onDismiss) { Text(t["close"]) }
            }
        }
    }
}

@Composable
private fun label(status: AttendanceStatus, t: UiStrings): String = when (status) {
    AttendanceStatus.PRESENT -> t["attendance_present"]
    AttendanceStatus.ABSENT -> t["attendance_absent"]
    AttendanceStatus.LATE -> t["attendance_late"]
    AttendanceStatus.EXCUSED -> t["attendance_excused"]
    AttendanceStatus.CANCELLED -> t["attendance_cancelled"]
}

/** 補講を 1 回追加する。日付と時限を選び、追加後に出席を記録できる。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MakeupSessionSheet(
    api: CoreApi,
    courseId: String,
    onDone: () -> Unit,
    onDismiss: () -> Unit
) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var date by remember { mutableStateOf(todayIso()) }
    var period by remember { mutableStateOf(1) }
    var error by remember { mutableStateOf<String?>(null) }

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
            Text(t["makeup_add"], style = MaterialTheme.typography.titleLarge, color = colors.text)
            Text(t["event_date"], style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            org.openyaske.app.ui.design.DateField(
                label = t["event_date"],
                value = date,
                onChange = { date = it ?: date },
                allowClear = false
            )
            Text(t["search_period"], style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.xs)) {
                (1..7).forEach { p ->
                    FilterPill(
                        text = "$p" + t["search_period_suffix"],
                        onClick = { period = p },
                        selected = period == p
                    )
                }
            }
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.danger)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                org.openyaske.app.ui.design.PrimaryButton(
                    text = t["common_add"],
                    onClick = {
                        try {
                            api.addMakeupSession(courseId, date, startPeriod = period)
                            onDone()
                        } catch (e: Exception) {
                            error = e.message
                        }
                    }
                )
                TextButton(onClick = onDismiss) { Text(t["close"]) }
            }
        }
    }
}
