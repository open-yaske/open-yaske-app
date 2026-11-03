package org.openyaske.core.validation

import org.openyaske.core.model.SyllabusEntry
import org.openyaske.core.model.Weekday

/**
 * UEC シラバス TSV のパーサ。
 *
 * 形式は 1 行 8 列（タブ区切り）。
 * `//` で始まる行はコメント、空行は無視する。
 *
 * 列:
 * 0 連番 / 1 学期 / 2 学期 / 3 曜日時限 / 4 科目コード / 5 科目名 / 6 担当 / 7 教室
 *
 * 曜日時限は「月1, 月2」「月1・月2」のような複数コマ表記を許す。
 * 1 行から見つかったコマ数だけエントリを作る。
 */
object SyllabusTsvParser {
    private val SLOT = Regex("""([月火水木金土日])\s*([1-7])""")

    fun parse(text: String, year: Int): List<SyllabusEntry> {
        val entries = mutableListOf<SyllabusEntry>()
        val usedIds = mutableSetOf<String>()

        for (line in text.lineSequence()) {
            val trimmed = line.trimEnd('\r', '\n')
            if (trimmed.isBlank() || trimmed.startsWith("//")) continue

            val columns = trimmed.split('\t')
            if (columns.size < 7) continue

            val semester1 = columns.getOrNull(1)?.trim()?.ifBlank { null }
            val semester2 = columns.getOrNull(2)?.trim()?.ifBlank { null }
            val slotSpec = columns.getOrNull(3)?.trim().orEmpty()
            val code = columns.getOrNull(4)?.trim().orEmpty()
            val title = columns.getOrNull(5)?.trim().orEmpty()
            val instructor = columns.getOrNull(6)?.trim().orEmpty()
            val room = columns.getOrNull(7)?.trim()?.ifBlank { null }

            if (code.isEmpty() && title.isEmpty()) continue

            val semester = normalizeSemester(semester1 ?: semester2 ?: "")
            val slots = SLOT.findAll(slotSpec).map { match ->
                val weekday = weekdayOf(match.groupValues[1])
                val period = match.groupValues[2].toInt()
                weekday to period
            }.toList()

            // 曜日時限を持たない行（集中講義など）も 1 件として残す
            val targets: List<Pair<Weekday?, Int?>> =
                if (slots.isEmpty()) listOf(null to null) else slots

            for ((weekday, period) in targets) {
                val baseId = buildString {
                    append(year).append(':').append(code)
                    append(':').append(weekday?.name ?: "none")
                    append(':').append(period ?: 0)
                    append(':').append(instructor)
                }
                var id = baseId
                var suffix = 2
                while (!usedIds.add(id)) {
                    id = "$baseId#$suffix"
                    suffix++
                }

                entries += SyllabusEntry(
                    id = id,
                    year = year,
                    code = code,
                    title = title,
                    instructor = instructor,
                    room = room,
                    weekday = weekday,
                    period = period,
                    semester = semester,
                    semester1 = semester1,
                    semester2 = semester2,
                    programType = programTypeOf(code)
                )
            }
        }

        return entries
    }

    /** 前期・後期の表記ゆれを 2 学期に寄せる。 */
    fun normalizeSemester(raw: String): String {
        val value = raw
            .replace(" ", "")
            .replace("\u3000", "")
            .replace("ﾀｰﾑ", "ターム")
        if (value.isEmpty()) return ""
        if (value.contains("通年")) return "通年"
        if (listOf("前期", "前学期", "春ターム", "夏ターム", "春季", "夏季").any { value.contains(it) }) {
            return "前学期"
        }
        if (listOf("後期", "後学期", "秋ターム", "冬ターム", "秋季", "冬季").any { value.contains(it) }) {
            return "後学期"
        }
        return value
    }

    /** 科目コード先頭 2 桁から昼間・夜間・大学院を判定する。 */
    fun programTypeOf(code: String): String? = when (code.take(2)) {
        "21" -> "昼間"
        "22" -> "夜間"
        "25" -> "修士"
        "26" -> "博士"
        else -> null
    }

    private fun weekdayOf(char: String): Weekday = when (char) {
        "月" -> Weekday.MON
        "火" -> Weekday.TUE
        "水" -> Weekday.WED
        "木" -> Weekday.THU
        "金" -> Weekday.FRI
        "土" -> Weekday.SAT
        else -> Weekday.SUN
    }
}
