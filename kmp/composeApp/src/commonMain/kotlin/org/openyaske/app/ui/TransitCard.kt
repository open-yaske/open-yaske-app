package org.openyaske.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.FilterPill
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.tabular
import org.openyaske.core.api.AppSettings
import org.openyaske.core.api.CoreApi
import org.openyaske.core.api.TransitBoard
import org.openyaske.core.api.TransitDeparture

/**
 * 乗換カード。次の 3 便を、発車までの残り秒つきで出す。
 * 方面と種別は絞り込みチップで切り替え、設定に保存する。
 */
@Composable
internal fun TransitCard(
    api: CoreApi,
    settings: AppSettings,
    board: TransitBoard,
    stationName: String?,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val english = !(settings.locale.startsWith("ja") || settings.locale.startsWith("zh"))

    // 発車までの残りは 1 秒ごとに数え直す。API の date（運行日）を基準にする。
    var nowSecond by remember(board.date) {
        mutableIntStateOf(elapsedSecondOfServiceDay(board.date))
    }
    LaunchedEffect(board.date) {
        while (true) {
            nowSecond = elapsedSecondOfServiceDay(board.date)
            delay(1_000)
        }
    }

    val content = remember(board, nowSecond, settings.transitDirection, settings.transitType) {
        TransitPresentation.content(
            board = board,
            nowSecond = nowSecond,
            destination = settings.transitDirection,
            type = settings.transitType,
            english = english
        )
    }

    Column(
        modifier = modifier
            .clip(AppShapes.card)
            .background(colors.surface)
            .border(1.dp, colors.border, AppShapes.card)
            .padding(Space.md),
        verticalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(AppIcons.timetable, null, tint = colors.accent, modifier = Modifier.size(18.dp))
            Text(
                text = stationName ?: t["transit_title"],
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = Space.xs)
            )
        }
        if (content.rows.isEmpty()) {
            Text(
                text = if (content.filtered) t["transit_no_match"] else t["transit_empty"],
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        } else {
            content.rows.forEach { row ->
                DepartureRow(
                    departure = row.departure,
                    remaining = row.remaining,
                    english = content.english
                )
            }
        }
        if (content.destinations.size > 1) {
            FilterRow(
                label = t["transit_destination"],
                chips = content.destinations,
                selected = settings.transitDirection,
                onSelect = { key ->
                    api.updateSettings {
                        it.copy(transitDirection = if (it.transitDirection == key) null else key)
                    }
                }
            )
        }
        if (content.types.size > 1) {
            FilterRow(
                label = t["transit_type"],
                chips = content.types,
                selected = settings.transitType,
                onSelect = { key ->
                    api.updateSettings {
                        it.copy(transitType = if (it.transitType == key) null else key)
                    }
                }
            )
        }
    }
}

@Composable
private fun FilterRow(
    label: String,
    chips: List<TransitChip>,
    selected: String?,
    onSelect: (String) -> Unit
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.xs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
        chips.forEach { chip ->
            FilterPill(
                text = chip.text,
                onClick = { onSelect(chip.key) },
                selected = selected == chip.key
            )
        }
    }
}

@Composable
private fun DepartureRow(departure: TransitDeparture, remaining: Int, english: Boolean) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val lineColor = remember(departure.color) {
        parseLineColor(departure.color) ?: colors.primary
    }
    val type = departure.headsign.serviceType?.label(english)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 路線色。色が取れない路線はプライマリ色にする。
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(30.dp)
                .clip(AppShapes.pill)
                .background(lineColor)
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Space.sm)
        ) {
            Text(
                text = departure.headsign.destination.label(english),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (type != null) {
                Text(
                    text = type,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = remainingLabel(remaining, t),
                style = MaterialTheme.typography.bodyMedium.tabular(),
                fontWeight = FontWeight.SemiBold,
                color = if (remaining < 180) colors.danger else colors.text
            )
            Text(
                text = formatDepartureSecs(departure.departureSecs),
                style = MaterialTheme.typography.labelSmall.tabular(),
                color = colors.textSecondary
            )
        }
    }
}

/** "D5007F" / "#D5007F" を Color にする。取れなければ null。 */
private fun parseLineColor(hex: String?): Color? {
    val value = hex?.trim()?.removePrefix("#") ?: return null
    if (value.length != 6) return null
    val rgb = value.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or rgb)
}
