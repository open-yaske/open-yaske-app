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
import org.openyaske.core.api.TransitBoard
import org.openyaske.core.api.WeatherClient
import org.openyaske.core.api.WeatherSnapshot

/** 30 分キャッシュ（天気）。 */
private data class CachedWeather(val at: Long, val value: WeatherSnapshot)

private var weatherCache: CachedWeather? = null

/**
 * ホームに置く天気と乗換案内の枠。
 * 取得できなければカードごと出さない（空の箱を置かない）。横並びは 900dp 以上のときだけ。
 */
@Composable
fun WeatherTransitCards(settings: AppSettings, wide: Boolean) {
    val weather = rememberWeather(settings)
    val board = rememberTransitBoard(settings.transitStationId, settings.transitVisible)

    if (weather == null && board == null) return

    val stationName = settings.transitStations
        .firstOrNull { it.id == settings.transitStationId }?.name
        ?: settings.transitStations.firstOrNull()?.name

    if (wide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.sm)
        ) {
            weather?.let { WeatherCard(it, Modifier.weight(1f)) }
            board?.let { TransitCard(settings, it, stationName, Modifier.weight(1f)) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
            weather?.let { WeatherCard(it, Modifier.fillMaxWidth()) }
            board?.let { TransitCard(settings, it, stationName, Modifier.fillMaxWidth()) }
        }
    }
}

/** 天気は 30 分キャッシュ。地点が変わったら引き直す。 */
@Composable
private fun rememberWeather(settings: AppSettings): WeatherSnapshot? {
    var weather by remember { mutableStateOf<WeatherSnapshot?>(null) }

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

    // 表示直後にもう一度だけ取り直す（起動直後の失敗を引きずらない）。
    LaunchedEffect(weather == null) {
        if (weather == null && weatherCache == null) {
            delay(5_000)
            weather = WeatherClient.fetch(settings.weatherLatitude, settings.weatherLongitude)
                ?.also { weatherCache = CachedWeather(nowMillis(), it) }
        }
    }

    return weather
}

/** 乗換は 30 秒キャッシュ。駅を変えたらすぐ引き直す。 */
@Composable
private fun rememberTransitBoard(stationId: String?, enabled: Boolean): TransitBoard? {
    var board by remember { mutableStateOf<TransitBoard?>(null) }

    LaunchedEffect(stationId, enabled) {
        // 駅を変えた直後や非表示にした直後に、前の駅の発車を残さない。
        board = null
        if (stationId == null || !enabled) return@LaunchedEffect
        while (true) {
            TransitBoardLoader.load(stationId)?.let { board = it }
            delay(5_000)
        }
    }

    return board
}
