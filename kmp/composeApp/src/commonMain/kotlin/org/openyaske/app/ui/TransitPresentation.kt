package org.openyaske.app.ui

import org.openyaske.core.api.TransitBoard
import org.openyaske.core.api.TransitDeparture
import org.openyaske.core.api.TransitFilter
import org.openyaske.core.api.TransitSelection

/** 1 駅分のボード。複数駅をまとめて出すときに駅名を持つ。 */
internal data class TransitSource(
    val stationId: String,
    val stationName: String?,
    val board: TransitBoard
)

/** 画面に出す 1 便と、発車までの残り秒。 */
internal data class TransitRow(
    val departure: TransitDeparture,
    val remaining: Int,
    val stationName: String?,
    val showStation: Boolean,
    /** 方面で絞っている駅か。行の先頭を種別にする。 */
    val typeFirst: Boolean
)

/** 乗換カードに描く内容。 */
internal data class TransitContent(
    val rows: List<TransitRow>,
    val filtered: Boolean,
    val english: Boolean
)

/**
 * 乗換ボードを、その時刻と絞り込みに合わせて表示用へ射影する。
 * Compose に依存しないので、並べ替えと絞り込みの規則をここだけで読める。
 */
internal object TransitPresentation {
    fun content(
        sources: List<TransitSource>,
        nowSecond: Int,
        filters: Map<String, TransitFilter>,
        english: Boolean
    ): TransitContent {
        val multi = sources.size > 1
        // 複数駅のときは 1 駅が上位を占めないよう、駅ごとに 2 本ずつ拾ってから混ぜる。
        val selected = sources
            .flatMap { source ->
                val filter = filters[source.stationId] ?: TransitFilter()
                TransitSelection.upcoming(
                    departures = source.board.departures,
                    nowSecond = nowSecond,
                    destinations = filter.directions.toSet(),
                    type = filter.type,
                    limit = if (multi) 2 else 3
                ).map { Triple(source.stationName, it, filter.directions.isNotEmpty()) }
            }
            .sortedBy { it.second.departureSecs }
            .take(if (multi) 6 else 3)

        return TransitContent(
            rows = selected.map { (stationName, departure, typeFirst) ->
                TransitRow(
                    departure = departure,
                    remaining = departure.departureSecs - nowSecond,
                    stationName = stationName,
                    showStation = multi,
                    typeFirst = typeFirst
                )
            },
            filtered = sources.any { source ->
                val filter = filters[source.stationId]
                filter != null && (filter.directions.isNotEmpty() || filter.type != null)
            },
            english = english
        )
    }
}
