package org.openyaske.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import org.openyaske.core.api.TransitClient
import org.openyaske.core.api.TransitHeadsignParser
import org.openyaske.core.api.TransitSelection

class TransitParseTest {
    @Test
    fun splitsTypeAndDestination() {
        val headsign = TransitHeadsignParser.parse("(特急 Limited Express) 新宿 Shinjuku")
        assertEquals("特急", headsign.serviceType?.ja)
        assertEquals("Limited Express", headsign.serviceType?.en)
        assertEquals("新宿", headsign.destination.ja)
        assertEquals("Shinjuku", headsign.destination.en)
        assertEquals("新宿", headsign.destination.label(english = false))
        assertEquals("Shinjuku", headsign.destination.label(english = true))
    }

    @Test
    fun handlesMultiWordType() {
        val headsign = TransitHeadsignParser.parse("(区間急行 Semi Express) 橋本 Hashimoto")
        assertEquals("区間急行", headsign.serviceType?.ja)
        assertEquals("Semi Express", headsign.serviceType?.en)
        assertEquals("橋本", headsign.destination.ja)
    }

    @Test
    fun handlesNoType() {
        val headsign = TransitHeadsignParser.parse("新宿 Shinjuku")
        assertNull(headsign.serviceType)
        assertEquals("新宿", headsign.destination.ja)
        assertEquals("Shinjuku", headsign.destination.en)
    }

    @Test
    fun parsesDeparturesWithColorAndDate() {
        val text = """
            {"stationId":"tokyo-keio-rail:Chofu","date":"20261104",
             "departures":[
              {"routeName":"KO","mode":"rail","color":"D5007F",
               "headsign":"(各駅停車 Local) 新宿 Shinjuku",
               "departureSecs":44520}
             ]}
        """.trimIndent()
        val board = assertNotNull(TransitClient.parse(text))
        assertEquals("20261104", board.date)
        val departure = board.departures.single()
        assertEquals("D5007F", departure.color)
        assertEquals("新宿", departure.destinationKey)
        assertEquals("各駅停車", departure.typeKey)
        assertEquals(44520, departure.departureSecs)
    }

    private fun boardOf(vararg rows: Pair<String, Int>): org.openyaske.core.api.TransitBoard {
        fun departure(headsign: String, secs: Int): org.openyaske.core.api.TransitDeparture =
            org.openyaske.core.api.TransitDeparture(
                route = "KO",
                color = "D5007F",
                departureSecs = secs,
                headsign = TransitHeadsignParser.parse(headsign),
                rawHeadsign = headsign
            )
        return org.openyaske.core.api.TransitBoard(
            date = "20261104",
            departures = rows.map { departure(it.first, it.second) }
        )
    }

    @Test
    fun dropsDepartedTrainsAndSortsByTime() {
        val board = boardOf(
            "(各駅停車 Local) 新宿 Shinjuku" to 44520,
            "(特急 Limited Express) 橋本 Hashimoto" to 44640,
            "(快速 Rapid) 本八幡 Motoyawata" to 44000
        )
        val upcoming = TransitSelection.upcoming(board, nowSecond = 44500)
        assertEquals(listOf(44520, 44640), upcoming.map { it.departureSecs })
    }

    @Test
    fun filtersByDestinationAndType() {
        val board = boardOf(
            "(各駅停車 Local) 新宿 Shinjuku" to 44520,
            "(特急 Limited Express) 橋本 Hashimoto" to 44640,
            "(特急 Limited Express) 新宿 Shinjuku" to 44700
        )
        assertEquals(
            listOf(44700),
            TransitSelection.upcoming(board, 0, destination = "新宿", type = "特急")
                .map { it.departureSecs }
        )
        assertEquals(
            listOf(44520, 44700),
            TransitSelection.upcoming(board, 0, destination = "新宿").map { it.departureSecs }
        )
    }
}
