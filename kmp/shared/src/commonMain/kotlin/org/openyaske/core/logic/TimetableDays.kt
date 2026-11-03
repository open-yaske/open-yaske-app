package org.openyaske.core.logic

import org.openyaske.core.model.Weekday

/**
 * 時間割に出す曜日を決める。
 *
 * 授業がある曜日でも、設定で隠していれば出さない。
 * 授業が 1 つも無いときは設定の曜日をそのまま並べる。
 */
object TimetableDays {
    fun resolve(used: List<Weekday>, visible: List<Weekday>): List<Weekday> {
        val shown = visible.toSet()
        val filtered = used.filter { it in shown }.distinct().sortedBy { it.isoNumber }
        if (filtered.isNotEmpty()) return filtered
        return visible.distinct().sortedBy { it.isoNumber }
    }
}
