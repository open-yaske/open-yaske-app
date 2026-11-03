package org.openyaske.core.logic

/** 時限の開始時刻。設定で上書きできるが、並び替えと表示の既定値として持つ。 */
object PeriodTimes {
    /** UEC の既定時刻（分）。 */
    val uecStartMinutes: Map<Int, Int> = mapOf(
        1 to 9 * 60,
        2 to 10 * 60 + 40,
        3 to 13 * 60,
        4 to 14 * 60 + 40,
        5 to 16 * 60 + 15,
        6 to 17 * 60 + 50,
        7 to 19 * 60 + 30
    )

    /** UEC の既定終了時刻（分）。 */
    val uecEndMinutes: Map<Int, Int> = mapOf(
        1 to 10 * 60 + 30,
        2 to 12 * 60 + 10,
        3 to 14 * 60 + 30,
        4 to 16 * 60 + 10,
        5 to 17 * 60 + 45,
        6 to 19 * 60 + 20,
        7 to 21 * 60
    )

    /** 分を "HH:MM" にする。 */
    fun formatMinutes(minutes: Int): String {
        val h = (minutes / 60).toString().padStart(2, '0')
        val m = (minutes % 60).toString().padStart(2, '0')
        return "$h:$m"
    }

    /** "HH:MM" を 0 時からの分に変換する。 */
    fun parseTimeToMinutes(value: String?): Int? {
        if (value == null) return null
        val parts = value.split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }
}
