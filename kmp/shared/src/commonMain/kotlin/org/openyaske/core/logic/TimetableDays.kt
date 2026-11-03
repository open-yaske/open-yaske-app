package org.openyaske.core.logic

import org.openyaske.core.model.Weekday

/**
 * 時間割に出す曜日を決める。
 *
 * 設定で表示にした曜日をそのまま並べる。授業がある曜日だけに絞ると、
 * 1 コマ登録しただけで「1 週間が 1 日」に見えてしまうため。
 * `used` は呼び出し側との互換のために残している。
 */
object TimetableDays {
    @Suppress("UNUSED_PARAMETER")
    fun resolve(used: List<Weekday>, visible: List<Weekday>): List<Weekday> =
        visible.distinct().sortedBy { it.isoNumber }
}
