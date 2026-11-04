package org.openyaske.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.tabular
import org.openyaske.core.api.AppSettings
import org.openyaske.core.api.TransitBoard
import org.openyaske.core.api.TransitDeparture

/**
 * 乗換カード。複数駅の次の便をまとめ、発車までの残り秒つきで出す。
 * 方面と種別の絞り込みは設定画面で決める（カードには置かない）。
 */
@Composable
internal fun TransitCard(
    settings: AppSettings,
    sources: List<TransitSource>,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val english = !(settings.locale.startsWith("ja") || settings.locale.startsWith("zh"))
    val date = sources.firstOrNull()?.board?.date ?: ""

    // 発車までの残りは 1 秒ごとに数え直す。API の date（運行日）を基準にする。
    var nowSecond by remember(date) {
        mutableIntStateOf(elapsedSecondOfServiceDay(date))
    }
    LaunchedEffect(date) {
        while (true) {
            nowSecond = elapsedSecondOfServiceDay(date)
            delay(1_000)
        }
    }

    val content = remember(sources, nowSecond, settings.transitFilters) {
        TransitPresentation.content(
            sources = sources,
            nowSecond = nowSecond,
            filters = settings.transitFilters,
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
                text = stationTitle(sources, t),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
                    english = content.english,
                    stationName = row.stationName.takeIf { row.showStation },
                    // 方面で絞っている駅は、行の先頭を種別にする。
                    typeFirst = row.typeFirst
                )
            }
        }
    }
}

@Composable
private fun DepartureRow(
    departure: TransitDeparture,
    remaining: Int,
    english: Boolean,
    stationName: String?,
    typeFirst: Boolean
) {
    val colors = LocalAppColors.current
    val lineColor = remember(departure.color) {
        parseLineColor(departure.color) ?: colors.primary
    }
    val type = departure.headsign.serviceType?.label(english)
    val destination = departure.headsign.destination.label(english)
    // 種別を先に出すときも 2 行のまま。行の高さと右端の桁を揃える。
    val primary = if (typeFirst && type != null) type else destination
    val other = if (typeFirst && type != null) {
        destination.takeIf { it != type }
    } else {
        type
    }
    val secondary = when {
        // 方面で絞っているときは行先が全部同じなので、駅名だけを添える。
        typeFirst && type != null -> stationName
        else -> listOfNotNull(stationName, other).joinToString(" · ").ifBlank { null }
    }
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
                text = primary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (secondary != null) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = remainingLabel(remaining),
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

/** 複数駅を選んでいるときは駅名をまとめて出す。 */
private fun stationTitle(sources: List<TransitSource>, t: UiStrings): String {
    val names = sources.mapNotNull { it.stationName }.distinct()
    return when {
        names.isEmpty() -> t["transit_title"]
        names.size <= 2 -> names.joinToString("・")
        else -> names.take(2).joinToString("・") + " " + t["transit_more_stations"]
    }
}
