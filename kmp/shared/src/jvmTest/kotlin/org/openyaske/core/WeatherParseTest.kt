package org.openyaske.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.openyaske.core.api.WeatherClient

class WeatherParseTest {
    @Test
    fun parsesOpenMeteoResponse() {
        val text = """
            {"latitude":35.65,"current":{"time":"2026-11-04T09:45","temperature_2m":13.2,
            "apparent_temperature":11.1,"relative_humidity_2m":62,"wind_speed_10m":7.6,
            "weather_code":2,"is_day":1},
            "hourly":{"time":["2026-11-04T00:00","2026-11-04T01:00"],
            "temperature_2m":[12.1,12.5],"weather_code":[1,2],"precipitation_probability":[24,20]}}
        """.trimIndent()
        val snapshot = assertNotNull(WeatherClient.parse(text))
        assertEquals(13, snapshot.temperature)
        assertEquals(2, snapshot.code)
        assertEquals(2, snapshot.hours.size)
        assertEquals(24, snapshot.hours.first().precipitation)
    }
}
