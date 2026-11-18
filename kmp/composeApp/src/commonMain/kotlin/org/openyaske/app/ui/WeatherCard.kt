package org.openyaske.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.tabular
import org.openyaske.core.api.WeatherHour
import org.openyaske.core.api.WeatherSnapshot

/**
 * 天気カード。現在の気温と状態、これから 3 時間ごとの気温と降水確率。
 * 状態はアイコンの形で示し、色だけで意味を持たせない。
 */
@Composable
internal fun WeatherCard(
    weather: WeatherSnapshot,
    placeName: String?,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val points = remember(weather) { nextHours(weather.hours) }

    Column(
        modifier = modifier
            .clip(AppShapes.card)
            .background(colors.surface)
            .padding(Space.lg),
        verticalArrangement = Arrangement.spacedBy(Space.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = t["weather_title"],
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
            if (!placeName.isNullOrBlank()) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = placeName,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = weatherIcon(weather.code, weather.isDay),
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.padding(start = Space.md)) {
                Text(
                    text = "${weather.temperature}°",
                    style = MaterialTheme.typography.headlineMedium.tabular(),
                    color = colors.text
                )
                Text(
                    text = weatherLabel(weather.code, t),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Space.xs),
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            points.forEachIndexed { index, hour ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (index == 0) t["weather_now"] else "+${index * 3}h",
                        style = MaterialTheme.typography.labelSmall.tabular(),
                        color = colors.textSecondary
                    )
                    // 各コマの天気。時刻から昼/夜だけ決め、形で状態を出す。
                    Icon(
                        imageVector = weatherIcon(hour.code, hour.hour in 6..17),
                        contentDescription = weatherLabel(hour.code, t),
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${hour.temperature}°",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.text
                    )
                    if (hour.precipitation > 0) {
                        Text(
                            text = "${hour.precipitation}%",
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = colors.info
                        )
                    } else {
                        // 0% は書かない。行の高さだけ確保して桁を揃える。
                        Spacer(Modifier.height(15.dp))
                    }
                }
            }
        }
    }
}

/** 今の時刻から 3 時間おきに 4 点。夜でも翌日へ続けて拾う。 */
private fun nextHours(hours: List<WeatherHour>): List<WeatherHour> {
    if (hours.isEmpty()) return emptyList()
    val nowHour = nowMinutes() / 60
    val start = hours.indexOfFirst { it.hour >= nowHour }.let { if (it < 0) 0 else it }
    return (0 until 4).mapNotNull { index -> hours.getOrNull(start + index * 3) }
}

private fun weatherIcon(code: Int, isDay: Boolean): ImageVector = when (code) {
    0, 1 -> if (isDay) AppIcons.day else AppIcons.night
    2, 3, 45, 48 -> AppIcons.cloudy
    in 51..57 -> AppIcons.drizzle
    in 61..67, in 80..82 -> AppIcons.rain
    in 71..77, in 85..86 -> AppIcons.snow
    in 95..99 -> AppIcons.thunder
    else -> AppIcons.cloudy
}

private fun weatherLabel(code: Int, t: UiStrings): String = when (code) {
    0, 1 -> t["weather_clear"]
    2, 3 -> t["weather_cloudy"]
    45, 48 -> t["weather_fog"]
    in 51..57 -> t["weather_drizzle"]
    in 61..67 -> t["weather_rain"]
    in 71..77 -> t["weather_snow"]
    in 80..82 -> t["weather_showers"]
    in 85..86 -> t["weather_snow_showers"]
    in 95..99 -> t["weather_thunder"]
    else -> t["weather_unknown"]
}
