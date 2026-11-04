package org.openyaske.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openyaske.core.net.httpGetText

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

/**
 * Open-Meteo（無認証）から現在の天気と 1 日の時間別予報を取る。
 * Svelte 版と同じ取得元。失敗したら null（画面はカードごと出さない）。
 */
object WeatherClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

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
