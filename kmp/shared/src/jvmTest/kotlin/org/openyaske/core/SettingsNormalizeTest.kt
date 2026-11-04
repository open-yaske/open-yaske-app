package org.openyaske.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.openyaske.core.api.AppSettings
import org.openyaske.core.api.TransitFilter
import org.openyaske.core.api.TransitStation

class SettingsNormalizeTest {
    @Test
    fun foldsLegacySingleStationAndDirection() {
        val settings = AppSettings(
            transitStations = listOf(
                TransitStation("a", "調布"),
                TransitStation("b", "新宿")
            ),
            transitStationId = "b",
            transitDirection = "新宿"
        ).normalized()

        assertEquals(listOf("b"), settings.transitStationIds)
        assertEquals(
            TransitFilter(directions = listOf("新宿")),
            settings.transitFilters["b"]
        )
        assertEquals(emptyList(), settings.transitDirections)
        assertNull(settings.transitStationId)
        assertNull(settings.transitDirection)
    }

    @Test
    fun keepsMultipleSelectionsAndDropsUnknownStation() {
        val settings = AppSettings(
            transitStations = listOf(
                TransitStation("a", "調布"),
                TransitStation("b", "新宿")
            ),
            transitStationIds = listOf("a", "b", "gone"),
            transitDirections = listOf("新宿", "橋本", " ")
        ).normalized()

        assertEquals(listOf("a", "b"), settings.transitStationIds)
        assertEquals(listOf("新宿", "橋本"), settings.transitFilters["a"]?.directions)
        assertEquals(listOf("新宿", "橋本"), settings.transitFilters["b"]?.directions)
    }

    @Test
    fun keepsPerStationFiltersAndDropsUnknown() {
        val settings = AppSettings(
            transitStations = listOf(
                TransitStation("a", "調布"),
                TransitStation("b", "新宿")
            ),
            transitStationIds = listOf("a", "b"),
            transitFilters = mapOf(
                "a" to TransitFilter(listOf("新宿", " "), "特急"),
                "b" to TransitFilter(emptyList(), null),
                "gone" to TransitFilter(listOf("橋本"), null)
            )
        ).normalized()

        assertEquals(listOf("新宿"), settings.transitFilters["a"]?.directions)
        assertEquals("特急", settings.transitFilters["a"]?.type)
        assertNull(settings.transitFilters["b"])
        assertNull(settings.transitFilters["gone"])
    }

    @Test
    fun weatherDefaultsAreKept() {
        val settings = AppSettings().normalized()
        assertTrue(settings.weatherVisible)
        assertEquals("調布キャンパス", settings.weatherPlaceName)
        assertEquals(35.6256, settings.weatherLatitude)
        assertEquals(139.4855, settings.weatherLongitude)
    }
}
