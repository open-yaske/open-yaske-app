package org.openyaske.app.ui.timetable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.app.ui.design.tabular
import org.openyaske.app.ui.LocalUiStrings
import org.openyaske.app.ui.design.Layout
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.Disclosure
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Radius
import org.openyaske.app.ui.design.parseCourseColor
import org.openyaske.app.ui.design.readableOn
import org.openyaske.app.ui.design.tintOf
import org.openyaske.app.ui.design.tapFeedback
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.TimetableScreenState
import org.openyaske.core.logic.PeriodTimes
import org.openyaske.core.model.Course
import org.openyaske.core.model.TimetableSlot
import org.openyaske.core.model.Weekday

@Composable
fun WeekdayHeader(
    days: List<Weekday>,
    today: Weekday?,
    showLatePeriods: Boolean,
    onToggleLatePeriods: (Boolean) -> Unit
) {
    val t = LocalUiStrings.current
    val colors = LocalAppColors.current
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = Layout.cellGap)) {
        Column(
            modifier = Modifier.width(48.dp).height(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("6-7限", style = MaterialTheme.typography.labelSmall, color = colors.textSecondary)
            MiniSwitch(checked = showLatePeriods, onChange = onToggleLatePeriods)
        }
        days.forEach { day ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Layout.cellGap)
                    .height(40.dp)
                    .clip(AppShapes.control)
                    .background(if (day == today) colors.primaryContainer else colors.surface)
                    .border(
                        1.dp,
                        if (day == today) colors.primary else colors.border,
                        AppShapes.control
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = t["day_${day.isoNumber}"],
                    style = MaterialTheme.typography.titleMedium,
                    color = when {
                        day == today -> colors.onPrimaryContainer
                        day == Weekday.SUN -> colors.danger
                        else -> colors.primary
                    }
                )
            }
        }
    }
}

/** 本家の「6-7限」トグル。小さなスイッチで時限の表示を切り替える。 */
@Composable
fun MiniSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .width(28.dp)
            .height(16.dp)
            .clip(AppShapes.pill)
            .background(if (checked) colors.primary else colors.borderStrong)
            .clickable { onChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(colors.surface)
        )
    }
}

@Composable
fun PeriodRow(
    api: CoreApi,
    period: Int,
    days: List<Weekday>,
    view: org.openyaske.core.api.TimetableScreenState,
    colorIndex: Map<String, Int>,
    today: Weekday?,
    currentPeriod: Int?,
    mode: org.openyaske.core.api.CellSurfaceMode = org.openyaske.core.api.CellSurfaceMode.OPAQUE,
    hasBackground: Boolean = false,
    onCourse: (String) -> Unit,
    onEmptyCell: (Weekday, Int) -> Unit
) {
    val colors = LocalAppColors.current
    val start = PeriodTimes.uecStartMinutes[period]
    val end = PeriodTimes.uecEndMinutes[period]
    Row(
        modifier = Modifier.fillMaxWidth().height(104.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.width(48.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            start?.let {
                Text(PeriodTimes.formatMinutes(it), style = MaterialTheme.typography.labelSmall.tabular(), color = colors.textTertiary)
            }
            Text(
                text = period.toString(),
                style = MaterialTheme.typography.bodyLarge.tabular(),
                fontWeight = FontWeight.SemiBold,
                color = if (period == currentPeriod) colors.accent else colors.primary
            )
            // 高さが変わらないよう、ドットの有無に関わらず同じ枠を確保する。
            Box(modifier = Modifier.height(6.dp), contentAlignment = Alignment.Center) {
                if (period == currentPeriod) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                    )
                }
            }
            end?.let {
                Text(PeriodTimes.formatMinutes(it), style = MaterialTheme.typography.labelSmall, color = colors.textTertiary)
            }
        }
        days.forEach { day ->
            val cell = view.cells[TimetableSlot(day, period)]
            Box(modifier = Modifier.weight(1f).padding(Layout.cellGap)) {
                if (cell == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(AppShapes.control)
                            .border(1.dp, colors.border, AppShapes.control)
                            .clickable {
                                tapFeedback()
                                onEmptyCell(day, period)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.Icon(
                            imageVector = org.openyaske.app.ui.design.AppIcons.add,
                            contentDescription = null,
                            tint = colors.textTertiary.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    val color = parseCourseColor(cell.course.color, colorIndex[cell.course.id] ?: 0)
                    // すりガラスは「半透明 + 1dp の細線」で表す（Compose には背景ぼかしが無い）。
                    val surfaceAlpha = when (mode) {
                        org.openyaske.core.api.CellSurfaceMode.OPAQUE -> 1f
                        org.openyaske.core.api.CellSurfaceMode.FROSTED -> 0.55f
                        org.openyaske.core.api.CellSurfaceMode.TRANSLUCENT -> 0.30f
                        org.openyaske.core.api.CellSurfaceMode.TRANSPARENT -> 0f
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(AppShapes.control)
                            .then(
                                if (surfaceAlpha > 0f) Modifier.background(colors.surface.copy(alpha = surfaceAlpha))
                                else Modifier
                            )
                            .then(
                                if (mode == org.openyaske.core.api.CellSurfaceMode.TRANSPARENT) Modifier
                                else Modifier.background(tintOf(color, !colors.dark))
                            )
                            .then(
                                if (mode == org.openyaske.core.api.CellSurfaceMode.OPAQUE) Modifier
                                else Modifier.border(1.dp, colors.border, AppShapes.control)
                            )
                            .clickable { onCourse(cell.course.id) }
                    ) {
                        Text(
                            text = cell.course.name,
                            style = MaterialTheme.typography.bodySmall.copy(
                                lineHeight = 15.sp,
                                lineBreak = androidx.compose.ui.text.style.LineBreak.Paragraph
                            ),
                            fontWeight = FontWeight.SemiBold,
                            color = colors.text,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp, vertical = 5.dp)
                        )
                        cell.course.room?.let { room ->
                            // セル下端の全幅バー。角は親の clip で丸くなる。
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp)
                                    .background(color),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = room,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    color = readableOn(color),
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnscheduledCard(course: Course, index: Int, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    val color = parseCourseColor(course.color, index)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.card)
            .background(colors.surface)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(4.dp).height(48.dp).background(color))
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(course.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = colors.text)
            Text(
                text = listOfNotNull(course.instructor, course.room).joinToString(" / ").ifBlank {
                    LocalUiStrings.current["timetable_unscheduled"]
                },
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }
        Disclosure(modifier = Modifier.padding(end = 12.dp))
    }
}
