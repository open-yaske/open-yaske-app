package org.openyaske.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import org.openyaske.app.ui.design.AppIcons
import org.openyaske.app.ui.design.AppShapes
import org.openyaske.app.ui.design.LocalAppColors
import org.openyaske.app.ui.design.Space
import org.openyaske.app.ui.design.tabular
import org.openyaske.core.api.WeatherSnapshot

/**
 * 天気カード。現在の気温と天気、3 時間ごとの気温と降水確率。
 * 取得できなかったときは呼び出し側がカードごと出さない。
 */
@Composable
internal fun WeatherCard(weather: WeatherSnapshot, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val label = weatherLabel(weather.code, t)
    val points = remember(weather) {
        val hours = weather.hours
        if (hours.isEmpty()) emptyList()
        else listOf(0, 3, 6, 9).mapNotNull { hours.getOrNull(it) }
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
            Icon(
                if (weather.isDay) AppIcons.day else AppIcons.night,
                contentDescription = null,
                tint = if (weather.isDay) colors.warning else colors.accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = t["weather_title"],
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = Space.xs)
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${weather.temperature}°",
                style = MaterialTheme.typography.headlineMedium.tabular(),
                color = colors.text
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(start = Space.sm, bottom = 4.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.xs)
        ) {
            points.forEach { hour ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${hour.hour}",
                        style = MaterialTheme.typography.labelSmall.tabular(),
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${hour.temperature}°",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.text
                    )
                    Text(
                        text = "${hour.precipitation}%",
                        style = MaterialTheme.typography.labelSmall.tabular(),
                        color = colors.info
                    )
                }
            }
        }
    }
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
