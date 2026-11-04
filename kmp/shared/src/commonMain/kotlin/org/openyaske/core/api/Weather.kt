package org.openyaske.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openyaske.core.net.httpGetText
import org.openyaske.core.net.urlEncodeQuery

/** 現在の天気。数値はすべて取得できたものだけを入れる。 */
data class WeatherSnapshot(
    val temperature: Int,
    val apparent: Int,
    val humidity: Int,
    val wind: Int,
    val code: Int,
    val isDay: Boolean,
    val hours: List<WeatherHour>
)

data class WeatherHour(val hour: Int, val temperature: Int, val precipitation: Int, val code: Int)

/** 天気を取る地点。 */
data class WeatherPlace(
    val name: String,
    val region: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double,
    val population: Long = 0L
) {
    /** 一覧に出す名前。地域が違えば添える。 */
    fun displayName(english: Boolean): String {
        val area = if (english) country else region ?: country
        return if (area.isNullOrBlank() || area == name) name else "$name · $area"
    }
}

/**
 * Open-Meteo（無認証）から現在の天気と 1 日の時間別予報を取る。
 * Svelte 版と同じ取得元。失敗したら null（画面はカードごと出さない）。
 */
object WeatherClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * 地点名から緯度経度を引く。Open-Meteo の geocoding（無認証）。
     * 見つからなければ空。
     */
    suspend fun searchPlaces(query: String): List<WeatherPlace> {
        val text = query.trim()
        if (text.isBlank()) return emptyList()
        val primary = requestPlaces(text)
        // 漢字の地名は「横浜」だけでは横浜市が出ないことがある。「市」を足して引き直す。
        val needsCity = text.any { it.code > 0x2E80 } &&
            !text.endsWith("市") && !text.endsWith("町") && !text.endsWith("村")
        val withCity = if (needsCity) requestPlaces(text + "市") else emptyList()
        return (withCity + primary)
            .distinctBy { "${it.name}/${it.region}/${it.latitude}/${it.longitude}" }
            .sortedByDescending { it.population }
            .take(8)
    }

    private suspend fun requestPlaces(query: String): List<WeatherPlace> {
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=${urlEncodeQuery(query)}" +
            "&count=8&language=ja&format=json"
        val text = httpGetText(url) ?: return emptyList()
        val response = runCatching { json.decodeFromString<GeocodingResponse>(text) }.getOrNull()
            ?: return emptyList()
        return response.results.orEmpty().map {
            WeatherPlace(
                name = it.name,
                region = it.admin1,
                country = it.country,
                latitude = it.latitude,
                longitude = it.longitude,
                population = it.population
            )
        }
    }

    suspend fun fetch(latitude: Double, longitude: Double): WeatherSnapshot? {
        val url = buildString {
            append("https://api.open-meteo.com/v1/forecast")
            append("?latitude=$latitude&longitude=$longitude")
            append("&current=temperature_2m,apparent_temperature,relative_humidity_2m,wind_speed_10m,weather_code,is_day")
            append("&hourly=temperature_2m,weather_code,precipitation_probability")
            append("&timezone=auto&forecast_days=1")
        }
        val text = httpGetText(url) ?: return null
        return parse(text)
    }

    /** 本文をそのまま解釈する（テスト用に分離）。 */
    internal fun parse(text: String): WeatherSnapshot? {
        val response = runCatching { json.decodeFromString<OpenMeteo>(text) }.getOrNull() ?: return null
        val current = response.current ?: return null
        val hours = response.hourly?.time?.indices?.map { index ->
            WeatherHour(
                hour = response.hourly.time[index].substringAfter('T').take(2).toIntOrNull() ?: index,
                temperature = response.hourly.temperature.getOrNull(index)?.toInt() ?: 0,
                precipitation = response.hourly.precipitation.getOrNull(index) ?: 0,
                code = response.hourly.code.getOrNull(index) ?: 0
            )
        } ?: emptyList()
        return WeatherSnapshot(
            temperature = current.temperature.toInt(),
            apparent = current.apparent.toInt(),
            humidity = current.humidity,
            wind = current.wind.toInt(),
            code = current.code,
            isDay = current.isDay == 1,
            hours = hours
        )
    }

    @Serializable
    private data class GeocodingResponse(val results: List<GeocodingPlace>? = null)

    @Serializable
    private data class GeocodingPlace(
        val name: String = "",
        val latitude: Double = 0.0,
        val longitude: Double = 0.0,
        val country: String? = null,
        val admin1: String? = null,
        val population: Long = 0L
    )

    @Serializable
    private data class OpenMeteo(
        val current: Current? = null,
        val hourly: Hourly? = null
    )

    @Serializable
    private data class Current(
        @SerialName("temperature_2m") val temperature: Double = 0.0,
        @SerialName("apparent_temperature") val apparent: Double = 0.0,
        @SerialName("relative_humidity_2m") val humidity: Int = 0,
        @SerialName("wind_speed_10m") val wind: Double = 0.0,
        @SerialName("weather_code") val code: Int = 0,
        @SerialName("is_day") val isDay: Int = 1
    )

    @Serializable
    private data class Hourly(
        val time: List<String> = emptyList(),
        @SerialName("temperature_2m") val temperature: List<Double> = emptyList(),
        @SerialName("weather_code") val code: List<Int> = emptyList(),
        @SerialName("precipitation_probability") val precipitation: List<Int> = emptyList()
    )
}
