package org.openyaske.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openyaske.core.net.httpGetText

/** 乗換案内の 1 便。 */
data class TransitDeparture(
    val route: String,
    val headsign: String,
    val type: String?,
    val color: String?,
    val departureSecs: Int
)

data class TransitBoard(
    val date: String,
    val departures: List<TransitDeparture>
)

data class TransitPlace(val id: String, val name: String)

/**
 * 乗換案内。Svelte 版と同じ `api.transit.ls8h.com`。
 * 失敗したら null / 空で、カードは出さない。
 */
object TransitClient {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun departures(stationId: String): TransitBoard? {
        val text = httpGetText("https://api.transit.ls8h.com/api/v1/stations/$stationId/departures")
            ?: return null
        val response = runCatching { json.decodeFromString<DeparturesResponse>(text) }.getOrNull()
            ?: return null
        return TransitBoard(
            date = response.date,
            departures = response.departures.map {
                TransitDeparture(
                    route = it.routeName,
                    headsign = it.headsign ?: "",
                    type = it.trainType,
                    color = it.color,
                    departureSecs = it.departureSecs
                )
            }
        )
    }

    suspend fun suggest(query: String): List<TransitPlace> {
        if (query.isBlank()) return emptyList()
        val text = httpGetText(
            "https://api.transit.ls8h.com/api/v1/places/suggest?q=${query.replace(" ", "%20")}"
        ) ?: return emptyList()
        val response = runCatching { json.decodeFromString<PlacesResponse>(text) }.getOrNull()
            ?: return emptyList()
        return response.places
            .filter {
                (it.kind == "station" || it.kind == "stop") &&
                    !it.id.startsWith("transit:") &&
                    !it.id.startsWith("osm:") &&
                    !it.id.startsWith("geocoder:")
            }
            .map { TransitPlace(it.id, it.name) }
    }

    @Serializable
    private data class DeparturesResponse(
        val date: String = "",
        val departures: List<Departure> = emptyList()
    )

    @Serializable
    private data class Departure(
        @SerialName("routeName") val routeName: String = "",
        val headsign: String? = null,
        @SerialName("trainType") val trainType: String? = null,
        val color: String? = null,
        @SerialName("departureSecs") val departureSecs: Int = 0
    )

    @Serializable
    private data class PlacesResponse(val places: List<Place> = emptyList())

    @Serializable
    private data class Place(
        val id: String = "",
        val name: String = "",
        val kind: String = ""
    )
}
