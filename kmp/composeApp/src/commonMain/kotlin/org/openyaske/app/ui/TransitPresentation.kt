package org.openyaske.app.ui

import org.openyaske.core.api.TransitBoard
import org.openyaske.core.api.TransitDeparture
import org.openyaske.core.api.TransitSelection

/** 絞り込みチップ。key は言語に依らない日本語側。 */
internal data class TransitChip(val text: String, val key: String)

/** 画面に出す 1 便と、発車までの残り秒。 */
internal data class TransitRow(val departure: TransitDeparture, val remaining: Int)

/** 乗換カードに描く内容。 */
internal data class TransitContent(
    val rows: List<TransitRow>,
    val destinations: List<TransitChip>,
    val types: List<TransitChip>,
    val filtered: Boolean,
    val english: Boolean
)

/**
 * 乗換ボードを、その時刻と絞り込みに合わせて表示用へ射影する。
 * Compose に依存しないので、並べ替えと絞り込みの規則をここだけで読める。
 */
internal object TransitPresentation {
    fun content(
        board: TransitBoard,
        nowSecond: Int,
        destination: String?,
        type: String?,
        english: Boolean
    ): TransitContent {
        val destinations = board.departures
            .map { it.headsign.destination }
            .distinctBy { it.ja }
            .sortedBy { it.ja }
            .map { TransitChip(it.label(english), it.ja) }
        val types = board.departures
            .mapNotNull { it.headsign.serviceType }
            .distinctBy { it.ja }
            .sortedBy { it.ja }
            .map { TransitChip(it.label(english), it.ja) }

        val rows = TransitSelection
            .upcoming(board, nowSecond, destination, type)
            .map { TransitRow(it, it.departureSecs - nowSecond) }

        return TransitContent(
            rows = rows,
            destinations = destinations,
            types = types,
            filtered = destination != null || type != null,
            english = english
        )
    }
}
