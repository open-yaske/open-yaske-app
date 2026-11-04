package org.openyaske.core.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.openyaske.core.net.httpGetText

/**
 * 方面名・種別名の日英ペア。
 * API は「(特急 Limited Express) 新宿 Shinjuku」のように 1 つの文字列へ詰めて返すため、
 * 表示する言語を選べるように分けて持つ。
 */
data class TransitText(val ja: String, val en: String? = null) {
    /** 画面に出す文字列。英語が無ければ日本語へ落とす。 */
    fun label(english: Boolean): String =
        if (english) en?.takeIf { it.isNotBlank() } ?: ja else ja
}

/** 種別と方面。 */
data class TransitHeadsign(val serviceType: TransitText?, val destination: TransitText)

/** 乗換案内の 1 便。 */
data class TransitDeparture(
    val route: String,
    val color: String?,
    val departureSecs: Int,
    val headsign: TransitHeadsign,
    /** 生の headsign。解析できなかったときの表示に使う。 */
    val rawHeadsign: String
) {
    /** 絞り込みのキー。言語を変えても一致するよう日本語側で持つ。 */
    val destinationKey: String get() = headsign.destination.ja
    val typeKey: String? get() = headsign.serviceType?.ja
}

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
        return parse(text)
    }

    /** 本文をそのまま解釈する（テスト用に分離）。 */
    internal fun parse(text: String): TransitBoard? {
        val response = runCatching { json.decodeFromString<DeparturesResponse>(text) }.getOrNull()
            ?: return null
        return TransitBoard(
            date = response.date,
            departures = response.departures.map {
                TransitDeparture(
                    route = it.routeName,
                    color = it.color,
                    departureSecs = it.departureSecs,
                    headsign = TransitHeadsignParser.parse(it.headsign ?: "", it.trainType),
                    rawHeadsign = it.headsign ?: ""
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

/** 発車の選び方。並べ替えと絞り込みの規則を 1 か所に置く。 */
object TransitSelection {
    /**
     * まだ発車していない便を、発車が近い順に [limit] 件まで返す。
     * [destination] と [type] は日本語側のキー（null は絞り込み無し）。
     */
    fun upcoming(
        board: TransitBoard,
        nowSecond: Int,
        destination: String? = null,
        type: String? = null,
        limit: Int = 3
    ): List<TransitDeparture> = board.departures
        .filter { departure ->
            (destination == null || departure.destinationKey == destination) &&
                (type == null || departure.typeKey == type)
        }
        .filter { it.departureSecs - nowSecond >= 0 }
        .sortedBy { it.departureSecs }
        .take(limit)
}

/**
 * `(特急 Limited Express) 新宿 Shinjuku` を種別と方面へ分解する。
 * 括弧が無いときは [trainType] を種別に使い、残り全部を方面にする。
 */
object TransitHeadsignParser {
    private val TYPE = Regex("""^\(([^)]*)\)\s*(.*)$""")

    fun parse(raw: String, trainType: String? = null): TransitHeadsign {
        val text = raw.trim()
        val match = TYPE.find(text)
        val typeText = match?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
            ?: trainType?.trim()?.takeIf { it.isNotEmpty() }
        val rest = (match?.groupValues?.get(2) ?: text).trim()
        return TransitHeadsign(
            serviceType = typeText?.let { splitBilingual(it) },
            destination = splitBilingual(rest.ifBlank { text })
        )
    }

    /** 「新宿 Shinjuku」を日本語と英語へ分ける。ASCII の開始位置で切る。 */
    fun splitBilingual(text: String): TransitText {
        val value = text.trim()
        val index = value.indexOfFirst { char ->
            char in 'A'..'Z' || char in 'a'..'z'
        }
        if (index <= 0) return TransitText(value, null)
        val ja = value.take(index).trim()
        val en = value.substring(index).trim()
        if (ja.isEmpty()) return TransitText(en, null)
        return TransitText(ja, en.ifBlank { null })
    }
}
