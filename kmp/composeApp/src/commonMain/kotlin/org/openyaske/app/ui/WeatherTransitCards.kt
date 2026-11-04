package org.openyaske.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import org.openyaske.core.api.TransitClient
import org.openyaske.core.api.TransitDeparture
import org.openyaske.core.api.WeatherClient
import org.openyaske.core.api.WeatherSnapshot

/** 30 分キャッシュ（天気）。 */
private data class CachedWeather(val at: Long, val value: WeatherSnapshot)

/** 60 秒キャッシュ（乗換）。 */
private data class CachedBoard(val at: Long, val value: TransitBoard)

private var weatherCache: CachedWeather? = null
private var boardCache: CachedBoard? = null
private var boardStation: String? = null

/**
 * 天気と乗換案内。取得できなければカードごと出さない（空の箱を置かない）。
 * 横並びは 900dp 以上のときだけ。
 */
@Composable
fun WeatherTransitCards(api: CoreApi, settings: AppSettings, wide: Boolean) {
    var weather by remember { mutableStateOf<WeatherSnapshot?>(null) }
    var board by remember { mutableStateOf<TransitBoard?>(null) }
    val stationId = settings.transitStationId

    LaunchedEffect(settings.weatherLatitude, settings.weatherLongitude) {
        val cached = weatherCache
        if (cached != null && nowMillis() - cached.at < 30 * 60 * 1000L) {
            weather = cached.value
            return@LaunchedEffect
        }
        val fetched = WeatherClient.fetch(settings.weatherLatitude, settings.weatherLongitude)
        if (fetched != null) {
            weatherCache = CachedWeather(nowMillis(), fetched)
            weather = fetched
        }
    }

    LaunchedEffect(stationId) {
        if (stationId == null) {
            board = null
            return@LaunchedEffect
        }
        val cached = boardCache
        if (cached != null && boardStation == stationId && nowMillis() - cached.at < 60_000L) {
            board = cached.value
            return@LaunchedEffect
        }
        val fetched = TransitClient.departures(stationId)
        if (fetched != null) {
            boardCache = CachedBoard(nowMillis(), fetched)
            boardStation = stationId
            board = fetched
        }
    }

    // 表示直後にもう一度だけ取り直す（起動直後の失敗を引きずらない）。
    LaunchedEffect(stationId, weather == null) {
        if (weather == null && weatherCache == null) {
            delay(5_000)
            weather = WeatherClient.fetch(settings.weatherLatitude, settings.weatherLongitude)
                ?.also { weatherCache = CachedWeather(nowMillis(), it) }
        }
    }

    if (weather == null && board == null) return

    val stationName = settings.transitStations
        .firstOrNull { it.id == stationId }?.name
        ?: settings.transitStations.firstOrNull()?.name

    if (wide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            weather?.let { WeatherCard(it, Modifier.weight(1f)) }
            board?.let { TransitCard(api, settings, it, stationName, Modifier.weight(1f)) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            weather?.let { WeatherCard(it, Modifier.fillMaxWidth()) }
            board?.let { TransitCard(api, settings, it, stationName, Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun WeatherCard(weather: WeatherSnapshot, modifier: Modifier = Modifier) {
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

@Composable
private fun TransitCard(
    api: CoreApi,
    settings: AppSettings,
    board: TransitBoard,
    stationName: String?,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val t = LocalUiStrings.current
    val lines = remember(board) { board.departures.map { it.route }.distinct().sorted() }
    val directions = remember(board) {
        board.departures.mapNotNull { it.headsign.takeIf { value -> value.isNotBlank() } }
            .distinct()
            .sorted()
    }
    val filtered = board.departures.filter { departure ->
        (settings.transitLine == null || departure.route == settings.transitLine) &&
            (settings.transitDirection == null || departure.headsign == settings.transitDirection)
    }
    val next = remember(filtered) { filtered.sortedBy { it.departureSecs }.take(3) }

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
        if (next.isEmpty()) {
            Text(
                text = t["transit_empty"],
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        } else {
            next.forEach { departure ->
                DepartureRow(departure)
            }
        }
        if (lines.size > 1 || directions.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.xs)
            ) {
                lines.take(2).forEach { line ->
                    FilterPill(
                        text = line,
                        onClick = {
                            api.updateSettings {
                                it.copy(transitLine = if (it.transitLine == line) null else line)
                            }
                        },
                        selected = settings.transitLine == line
                    )
                }
                directions.take(2).forEach { direction ->
                    FilterPill(
                        text = direction,
                        onClick = {
                            api.updateSettings {
                                it.copy(
                                    transitDirection = if (it.transitDirection == direction) null else direction
                                )
                            }
                        },
                        selected = settings.transitDirection == direction
                    )
                }
            }
        }
    }
}

@Composable
private fun DepartureRow(departure: TransitDeparture) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formatDepartureSecs(departure.departureSecs),
            style = MaterialTheme.typography.bodyMedium.tabular(),
            fontWeight = FontWeight.SemiBold,
            color = colors.primary
        )
        Text(
            text = listOfNotNull(departure.route, departure.headsign.takeIf { it.isNotBlank() })
                .joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = colors.text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = Space.sm)
        )
    }
}

private fun formatDepartureSecs(secs: Int): String {
    val normalized = if (secs < 0) secs + 86_400 else secs
    val hour = (normalized / 3600) % 24
    val minute = (normalized % 3600) / 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

@Composable
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
