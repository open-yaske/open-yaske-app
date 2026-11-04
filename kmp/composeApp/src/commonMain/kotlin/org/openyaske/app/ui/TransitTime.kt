package org.openyaske.app.ui

import org.openyaske.core.validation.IsoDate

/** 残り秒を「12:34」の分:秒で出す。等幅数字と組み合わせて桁を揃える。 */
internal fun remainingLabel(seconds: Int): String {
    val value = seconds.coerceAtLeast(0)
    val minutes = value / 60
    val rest = value % 60
    return "$minutes:${rest.toString().padStart(2, '0')}"
}

/** 運行日 0 時からの経過秒。API の date は YYYYMMDD。 */
internal fun elapsedSecondOfServiceDay(date: String): Int {
    if (date.length != 8) return nowSecondOfDay()
    val iso = "${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}"
    val days = runCatching {
        IsoDate.toEpochDay(todayIso()) - IsoDate.toEpochDay(iso)
    }.getOrDefault(0L)
    return (days * 86_400L + nowSecondOfDay()).toInt()
}

/** 発車時刻を HH:mm で出す。 */
internal fun formatDepartureSecs(secs: Int): String {
    val normalized = if (secs < 0) secs + 86_400 else secs
    val hour = (normalized / 3600) % 24
    val minute = (normalized % 3600) / 60
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}
