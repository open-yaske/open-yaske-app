package org.openyaske.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import org.openyaske.app.ui.design.Space
import org.openyaske.core.api.AppSettings
import org.openyaske.core.api.WeatherClient
import org.openyaske.core.api.WeatherSnapshot

/** 30 分キャッシュ（天気）。地点が変わったら使わない。 */
private data class CachedWeather(
    val at: Long,
    val latitude: Double,
    val longitude: Double,
    val value: WeatherSnapshot
)

private var weatherCache: CachedWeather? = null

/**
 * ホームに置く天気と乗換案内の枠。
 * 取得できなければカードごと出さない（空の箱を置かない）。横並びは 900dp 以上のときだけ。
 */
@Composable
fun WeatherTransitCards(settings: AppSettings, wide: Boolean) {
    val weather = rememberWeather(settings)
    val transit = rememberTransit(settings)

    if (weather == null && transit.isEmpty()) return

    if (wide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            weather?.let { WeatherCard(it, Modifier.weight(1f)) }
            if (transit.isNotEmpty()) {
                TransitCard(settings, transit, Modifier.weight(1f))
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            weather?.let { WeatherCard(it, Modifier.fillMaxWidth()) }
            if (transit.isNotEmpty()) {
                TransitCard(settings, transit, Modifier.fillMaxWidth())
            }
        }
    }
}

/** 天気は 30 分キャッシュ。地点が変わったら引き直す。 */
@Composable
private fun rememberWeather(settings: AppSettings): WeatherSnapshot? {
    var weather by remember { mutableStateOf<WeatherSnapshot?>(null) }

    LaunchedEffect(
        settings.weatherVisible,
        settings.weatherLatitude,
        settings.weatherLongitude
    ) {
        if (!settings.weatherVisible) {
            weather = null
            return@LaunchedEffect
        }
        val cached = weatherCache
        val samePlace = cached != null &&
            cached.latitude == settings.weatherLatitude &&
            cached.longitude == settings.weatherLongitude
        if (samePlace && nowMillis() - cached.at < 30 * 60 * 1000L) {
            weather = cached.value
            return@LaunchedEffect
        }
        val fetched = WeatherClient.fetch(settings.weatherLatitude, settings.weatherLongitude)
        if (fetched != null) {
            weatherCache = CachedWeather(
                at = nowMillis(),
                latitude = settings.weatherLatitude,
                longitude = settings.weatherLongitude,
                value = fetched
            )
            weather = fetched
        }
    }

    // 表示直後にもう一度だけ取り直す（起動直後の失敗を引きずらない）。
    LaunchedEffect(settings.weatherVisible, weather == null) {
        if (settings.weatherVisible && weather == null) {
            delay(5_000)
            weather = WeatherClient.fetch(settings.weatherLatitude, settings.weatherLongitude)
                ?.also {
                    weatherCache = CachedWeather(
                        at = nowMillis(),
                        latitude = settings.weatherLatitude,
                        longitude = settings.weatherLongitude,
                        value = it
                    )
                }
        }
    }

    return weather
}

/** 選んだ駅のボードをまとめる。30 秒キャッシュ。駅を変えたらすぐ引き直す。 */
@Composable
private fun rememberTransit(settings: AppSettings): List<TransitSource> {
    var sources by remember { mutableStateOf<List<TransitSource>>(emptyList()) }
    val selected = remember(settings.transitStations, settings.transitStationIds) {
        val ids = settings.transitStationIds.toSet()
        settings.transitStations.filter { it.id in ids }
    }

    LaunchedEffect(selected, settings.transitVisible) {
        // 駅を変えた直後や非表示にした直後に、前の駅の発車を残さない。
        sources = emptyList()
        if (!settings.transitVisible || selected.isEmpty()) return@LaunchedEffect
        while (true) {
            sources = selected.mapNotNull { station ->
                val board = TransitBoardLoader.load(station.id)
                    ?: TransitBoardLoader.cached(station.id)
                board?.let { TransitSource(station.id, station.name, it) }
            }
            delay(5_000)
        }
    }

    return sources
}
