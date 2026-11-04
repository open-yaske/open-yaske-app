package org.openyaske.app.ui

import org.openyaske.core.validation.IsoDate

expect fun todayIso(): String

/** 0 時からの分。現在の時限を出すのに使う。 */
expect fun nowMinutes(): Int

/** キャッシュの経過時間を見るための現在ミリ秒。 */
expect fun nowMillis(): Long

/** 学期の表示名。label が空のときは年と学期から作る。 */
fun termLabel(term: org.openyaske.core.model.Term, locale: String): String {
    if (term.label.isNotBlank()) return term.label
    val season = when (term.season?.name) {
        "SPRING" -> if (locale.startsWith("ja")) "前学期" else "Spring"
        "FALL" -> if (locale.startsWith("ja")) "後学期" else "Fall"
        else -> ""
    }
    return if (season.isBlank()) "${term.year}" else if (locale.startsWith("ja")) {
        "${term.year}年${season}"
    } else {
        "${term.year} $season"
    }
}

/** 期限の表示。今日・明日は言葉、それ以外は M/D。 */
fun dueLabel(date: String, today: String, locale: String): String {
    if (date == today) return if (locale.startsWith("ja")) "今日" else "Today"
    if (date == org.openyaske.core.validation.IsoDate.plusDays(today, 1)) {
        return if (locale.startsWith("ja")) "明日" else "Tomorrow"
    }
    val month = date.substring(5, 7).toIntOrNull() ?: 0
    val day = date.substring(8).toIntOrNull() ?: 0
    return "$month/$day"
}

fun monthLabel(year: Int, month: Int, locale: String): String =
    if (locale.startsWith("ja")) "${year}年${month}月" else "$year/$month"

fun monthDays(year: Int, month: Int): List<String> {
    val days = IsoDate.daysInMonth(year, month)
    return (1..days).map { day ->
        buildString {
            append(year.toString().padStart(4, '0'))
            append('-')
            append(month.toString().padStart(2, '0'))
            append('-')
            append(day.toString().padStart(2, '0'))
        }
    }
}
