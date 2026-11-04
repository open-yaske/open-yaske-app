package org.openyaske.app.ui

import org.openyaske.core.api.TransitBoard
import org.openyaske.core.api.TransitDeparture
import org.openyaske.core.api.TransitSelection

/** 画面に出す 1 便と、発車までの残り秒。 */
internal data class TransitRow(val departure: TransitDeparture, val remaining: Int)

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
        board: TransitBoard,
        nowSecond: Int,
        destination: String?,
        type: String?,
        english: Boolean
    ): TransitContent {
        val rows = TransitSelection
            .upcoming(board, nowSecond, destination, type)
            .map { TransitRow(it, it.departureSecs - nowSecond) }

        return TransitContent(
            rows = rows,
            filtered = destination != null || type != null,
            english = english
        )
    }
}
