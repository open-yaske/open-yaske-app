package org.openyaske.core.logic

import org.openyaske.core.model.Semester
import org.openyaske.core.model.Term

/**
 * 学年暦（前学期・後学期の授業実施期間）。
 *
 * 出典: 東京電気通信大学「学事日程（情報理工学域）」
 *       https://www.uec.ac.jp/campus/academic/calendar/pdf/{年度}/gakuiki.pdf
 * 取得日: 2026-11-04（各年度の PDF を 1 回だけ取得して転記）
 *
 * - 使うのは試験期間を含まない「授業実施期間」。
 * - 実行時にネットワークは見ない。表に無い年度は日付を作らず、
 *   学期画面で開始日・終了日を入力してもらう扱いにする。
 * - 翌年度以降を足すときは、URL の {年度} を差し替えて PDF を取得し、
 *   [table] に 1 行足すだけでよい。
 */
object AcademicCalendar {
    data class SemesterDates(val start: String, val end: String)

    data class Entry(
        val spring: SemesterDates,
        val fall: SemesterDates,
        val sourceUrl: String,
        val retrievedOn: String = "2026-11-04"
    )

    private fun entry(spring: SemesterDates, fall: SemesterDates, year: Int) = Entry(
        spring = spring,
        fall = fall,
        sourceUrl = "https://www.uec.ac.jp/campus/academic/calendar/pdf/$year/gakuiki.pdf"
    )

    private val table: Map<Int, Entry> = mapOf(
        // 令和2年度（コロナ対応で前学期の開始が繰り下がった年度）
        2020 to entry(
            SemesterDates("2020-05-07", "2020-09-01"),
            SemesterDates("2020-10-01", "2021-02-06"),
            2020
        ),
        2021 to entry(
            SemesterDates("2021-04-08", "2021-08-16"),
            SemesterDates("2021-10-01", "2022-02-02"),
            2021
        ),
        2022 to entry(
            SemesterDates("2022-04-08", "2022-07-29"),
            SemesterDates("2022-10-03", "2023-02-03"),
            2022
        ),
        2023 to entry(
            SemesterDates("2023-04-10", "2023-07-29"),
            SemesterDates("2023-10-02", "2024-02-02"),
            2023
        ),
        2024 to entry(
            SemesterDates("2024-04-06", "2024-07-29"),
            SemesterDates("2024-10-01", "2025-02-07"),
            2024
        ),
        2025 to entry(
            SemesterDates("2025-04-08", "2025-07-29"),
            SemesterDates("2025-10-01", "2026-02-06"),
            2025
        ),
        2026 to entry(
            SemesterDates("2026-04-08", "2026-07-29"),
            SemesterDates("2026-10-01", "2027-02-05"),
            2026
        )
    )

    /**
     * 表に無い年度は暫定の日程を返す（2027〜2050）。
     * 大学の PDF が公開されたら [table] に足して上書きする。
     * 暫定: 前学期 4/8〜7/29、後学期 10/1〜翌 2/5。
     */
    private const val PROVISIONAL_FROM = 2027
    private const val PROVISIONAL_TO = 2050

    fun isVerified(year: Int): Boolean = table.containsKey(year)

    fun dates(year: Int, season: Semester): SemesterDates? {
        table[year]?.let { entry ->
            return when (season) {
                Semester.SPRING -> entry.spring
                Semester.FALL -> entry.fall
            }
        }
        if (year !in PROVISIONAL_FROM..PROVISIONAL_TO) return null
        return when (season) {
            Semester.SPRING -> SemesterDates("$year-04-08", "$year-07-29")
            Semester.FALL -> SemesterDates("$year-10-01", "${year + 1}-02-05")
        }
    }

    fun term(year: Int, season: Semester): Term? =
        dates(year, season)?.let { Term.semester(year, season, it.start, it.end) }

    /** 4 月始まりの学年。1〜3 月は前年度に属する。 */
    fun academicYearOf(today: String): Int {
        val year = today.substring(0, 4).toIntOrNull() ?: return 2000
        val month = today.substring(5, 7).toIntOrNull() ?: 4
        return if (month >= 4) year else year - 1
    }
}
