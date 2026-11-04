package org.openyaske.app.ui

import org.openyaske.core.api.TransitBoard
import org.openyaske.core.api.TransitClient

/**
 * 乗換ボードの取得と短いキャッシュ。
 * ホームのカードと設定画面が同じ結果を見るように、1 か所にまとめる。
 */
internal object TransitBoardLoader {
    private data class Cached(val at: Long, val board: TransitBoard)

    private val cache = mutableMapOf<String, Cached>()

    /** 30 秒以内の同じ駅ならキャッシュを返す。取れなければ null。 */
    suspend fun load(stationId: String, maxAgeMillis: Long = 30_000L): TransitBoard? {
        val cached = cache[stationId]
        if (cached != null && nowMillis() - cached.at < maxAgeMillis) {
            return cached.board
        }
        val fetched = TransitClient.departures(stationId) ?: return null
        cache[stationId] = Cached(nowMillis(), fetched)
        return fetched
    }

    /** 取得済みのボード。再取得せずに選択肢だけ見たいときに使う。 */
    fun cached(stationId: String): TransitBoard? = cache[stationId]?.board
}
