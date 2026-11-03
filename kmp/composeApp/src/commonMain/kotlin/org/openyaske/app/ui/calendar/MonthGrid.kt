package org.openyaske.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openyaske.app.ui.design.tabular
import org.openyaske.app.ui.LocalUiStrings
import org.openyaske.app.ui.design.HairLine
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.readableOn
import org.openyaske.core.validation.IsoDate

/** 日セルに重ねる 1 本の帯。 */
data class DayBar(val label: String, val color: Color)

/** 日曜始まりの月グリッド。前後の月は null。 */
fun monthGrid(year: Int, month: Int): List<String?> {
    val days = org.openyaske.app.ui.monthDays(year, month)
    val first = days.first()
    val offset = IsoDate.dayOfWeekIso(first) % 7 // 日曜 = 0
    val cells = MutableList<String?>(offset) { null }
    cells.addAll(days)
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

@Composable
fun WeekdayRow(compact: Boolean = false) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 2.dp else 6.dp)
    ) {
        listOf(7, 1, 2, 3, 4, 5, 6).forEach { iso ->
            Text(
                text = t["day_$iso"],
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (iso == 7) colors.danger else colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MonthGrid(
    year: Int,
    month: Int,
    today: String,
    selected: String,
    bars: Map<String, List<DayBar>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    availableHeight: Dp? = null
) {
    val colors = LocalAppColors.current
    val grid = monthGrid(year, month)
    val rows = (grid.size / 7).coerceAtLeast(1)
    val rowHeight: Dp = when {
        availableHeight != null -> (availableHeight / rows).coerceIn(72.dp, 132.dp)
        compact -> 64.dp
        else -> 96.dp
    }
    // 帯は文字が読める本数まで。4 本以上は +n にまとめる。
    val maxBars = if (rowHeight >= 96.dp) 3 else 2
    val prefix = "$year-${month.toString().padStart(2, '0')}"

    Column(modifier = modifier.fillMaxWidth()) {
        grid.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    if (date == null) {
                        Box(Modifier.weight(1f).height(rowHeight))
                    } else {
                        DayCell(
                            date = date,
                            bars = bars[date].orEmpty(),
                            isToday = date == today,
                            isSelected = date == selected,
                            inMonth = date.startsWith(prefix),
                            height = rowHeight,
                            maxBars = maxBars,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelect(date) }
                        )
                    }
                }
            }
            HairLine(color = colors.border)
        }
    }
}

@Composable
private fun DayCell(
    date: String,
    bars: List<DayBar>,
    isToday: Boolean,
    isSelected: Boolean,
    inMonth: Boolean,
    height: Dp,
    maxBars: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val day = date.substring(8).toIntOrNull() ?: 0
    Column(
        modifier = modifier
            .height(height)
            .padding(2.dp)
            .clip(AppShapes.control)
            .background(if (isSelected) colors.primaryContainer else colors.background)
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(19.dp)
                .clip(CircleShape)
                .then(
                    when {
                        isSelected -> Modifier.background(colors.primary)
                        isToday -> Modifier.border(1.5.dp, colors.primary, CircleShape)
                        else -> Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.labelSmall.tabular(),
                fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    isSelected -> colors.onPrimary
                    !inMonth -> colors.textTertiary
                    else -> colors.text
                }
            )
        }
        // 予定は文字入りの帯で出す。何の予定かはセルだけで分かる。
        bars.take(maxBars).forEach { bar ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(AppShapes.pill)
                    .background(bar.color),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 13.sp),
                    color = readableOn(bar.color),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        if (bars.size > maxBars) {
            Text(
                text = "+${bars.size - maxBars}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }

    }
}
