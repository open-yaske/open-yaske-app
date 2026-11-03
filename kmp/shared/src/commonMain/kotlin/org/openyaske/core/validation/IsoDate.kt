package org.openyaske.core.validation

/**
 * タイムゾーンに依存しない ISO 日付 (YYYY-MM-DD) の検証と日数計算。
 *
 * 1970-01-01 を 0 とする epoch day と、ISO の曜日 (月=1 … 日=7) を扱う。
 */
object IsoDate {
    private val PATTERN = Regex("""^(\d{4})-(\d{2})-(\d{2})$""")

    fun isValid(value: String): Boolean {
        val match = PATTERN.matchEntire(value) ?: return false
        val (year, month, day) = match.destructured
        return isValid(year.toInt(), month.toInt(), day.toInt())
    }

    fun isValid(year: Int, month: Int, day: Int): Boolean {
        if (year !in 1..9999 || month !in 1..12 || day < 1) return false
        return day <= daysInMonth(year, month)
    }

    fun daysInMonth(year: Int, month: Int): Int = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 0
    }

    fun isLeapYear(year: Int): Boolean =
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0

    /** 1970-01-01 からの日数。 */
    fun toEpochDay(value: String): Long {
        val match = requireNotNull(PATTERN.matchEntire(value)) { "invalid ISO date: $value" }
        val (year, month, day) = match.destructured
        return daysFromCivil(year.toInt(), month.toInt(), day.toInt())
    }

    fun fromEpochDay(epochDay: Long): String {
        val (year, month, day) = civilFromDays(epochDay)
        return buildString {
            append(year.toString().padStart(4, '0'))
            append('-')
            append(month.toString().padStart(2, '0'))
            append('-')
            append(day.toString().padStart(2, '0'))
        }
    }

    fun plusDays(value: String, days: Int): String = fromEpochDay(toEpochDay(value) + days)

    /** ISO の曜日。月曜=1、日曜=7。 */
    fun dayOfWeekIso(value: String): Int {
        val epochDay = toEpochDay(value)
        // 1970-01-01 は木曜日 (4)
        return (((epochDay + 3) % 7 + 7) % 7 + 1).toInt()
    }

    fun isBefore(a: String, b: String): Boolean = toEpochDay(a) < toEpochDay(b)

    fun isAfter(a: String, b: String): Boolean = toEpochDay(a) > toEpochDay(b)

    /** Howard Hinnant の days_from_civil。 */
    private fun daysFromCivil(year: Int, month: Int, day: Int): Long {
        var y = year.toLong()
        y -= if (month <= 2) 1 else 0
        val era = (if (y >= 0) y else y - 399) / 400
        val yoe = y - era * 400
        val mp = (month + 9) % 12
        val doy = (153 * mp + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }

    /** Howard Hinnant の civil_from_days。 */
    private fun civilFromDays(epochDay: Long): Triple<Int, Int, Int> {
        val z = epochDay + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val doe = z - era * 146097
        val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
        val y = yoe + era * 400
        val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
        val mp = (5 * doy + 2) / 153
        val d = doy - (153 * mp + 2) / 5 + 1
        val m = mp + if (mp < 10) 3 else -9
        return Triple((y + if (m <= 2) 1 else 0).toInt(), m.toInt(), d.toInt())
    }
}
