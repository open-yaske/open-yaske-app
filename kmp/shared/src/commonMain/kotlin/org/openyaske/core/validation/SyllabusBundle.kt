@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package org.openyaske.core.validation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.cbor.Cbor
import org.openyaske.core.model.SyllabusEntry
import org.openyaske.core.model.Weekday

/**
 * 同梱シラバスの配布形式。
 *
 * TSV をそのまま読むと毎回パースと索引構築が走るため、ビルド時に CBOR へ変換する。
 * 科目名・教員名・教室・科目コードは辞書にし、レコードからは文字列と ID を落とす。
 * ID は年度・コード・曜日・時限・教員から復元する。
 */
@Serializable
data class SyllabusBundle(
    @SerialName("v") val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    @SerialName("t") val generatedAt: String = "",
    @SerialName("y") val years: List<Int> = emptyList(),
    @SerialName("c") val codes: List<String> = emptyList(),
    @SerialName("ti") val titles: List<String> = emptyList(),
    @SerialName("in") val instructors: List<String> = emptyList(),
    @SerialName("ro") val rooms: List<String> = emptyList(),
    @SerialName("se") val semesters: List<String> = emptyList(),
    @SerialName("e") val records: List<SyllabusRecord> = emptyList()
) {
    val entryCount: Int get() = records.size

    fun toEntries(): List<SyllabusEntry> = records.map { record ->
        val code = codes.getOrElse(record.code) { "" }
        val instructor = instructors.getOrElse(record.instructor) { "" }
        val weekday = record.weekday.takeIf { it in 1..7 }?.let(Weekday::fromIsoNumber)
        val period = record.period.takeIf { it in 1..7 }
        val baseId = buildString {
            append(record.year).append(':').append(code)
            append(':').append(weekday?.name ?: "none")
            append(':').append(period ?: 0)
            append(':').append(instructor)
        }
        SyllabusEntry(
            id = if (record.suffix > 0) "$baseId#${record.suffix + 1}" else baseId,
            year = record.year,
            code = code,
            title = titles.getOrElse(record.title) { "" },
            instructor = instructor,
            room = rooms.getOrNull(record.room),
            weekday = weekday,
            period = period,
            semester = semesters.getOrElse(record.semester) { "" },
            semester1 = semesters.getOrNull(record.semester1),
            semester2 = semesters.getOrNull(record.semester2),
            programType = PROGRAM_TYPES.getOrNull(record.programType)
        )
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        private val PROGRAM_TYPES = listOf<String?>(null, "昼間", "夜間", "修士", "博士")
    }
}

@Serializable
data class SyllabusRecord(
    @SerialName("y") val year: Int,
    @SerialName("c") val code: Int = 0,
    @SerialName("t") val title: Int = 0,
    @SerialName("n") val instructor: Int = 0,
    @SerialName("r") val room: Int = -1,
    @SerialName("w") val weekday: Int = 0,
    @SerialName("p") val period: Int = 0,
    @SerialName("s") val semester: Int = 0,
    @SerialName("s1") val semester1: Int = -1,
    @SerialName("s2") val semester2: Int = -1,
    @SerialName("g") val programType: Int = 0,
    /** 同じ年度・コード・時限・教員の重複を区別する。 */
    @SerialName("x") val suffix: Int = 0
)

object SyllabusBundleCodec {
    private val cbor = Cbor {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    fun encode(entries: List<SyllabusEntry>, generatedAt: String = ""): ByteArray {
        val codes = LinkedHashMap<String, Int>()
        val titles = LinkedHashMap<String, Int>()
        val instructors = LinkedHashMap<String, Int>()
        val rooms = LinkedHashMap<String, Int>()
        val semesters = LinkedHashMap<String, Int>()
        val years = mutableSetOf<Int>()
        val records = ArrayList<SyllabusRecord>(entries.size)
        val usedIds = HashMap<String, Int>()

        fun index(map: LinkedHashMap<String, Int>, value: String): Int =
            map.getOrPut(value) { map.size }

        for (entry in entries.sortedWith(compareBy({ it.year }, { it.code }, { it.title }))) {
            years += entry.year
            val baseId = buildString {
                append(entry.year).append(':').append(entry.code)
                append(':').append(entry.weekday?.name ?: "none")
                append(':').append(entry.period ?: 0)
                append(':').append(entry.instructor)
            }
            val suffix = usedIds[baseId] ?: 0
            usedIds[baseId] = suffix + 1

            records += SyllabusRecord(
                year = entry.year,
                code = index(codes, entry.code),
                title = index(titles, entry.title),
                instructor = index(instructors, entry.instructor),
                room = entry.room?.let { index(rooms, it) } ?: -1,
                weekday = entry.weekday?.isoNumber ?: 0,
                period = entry.period ?: 0,
                semester = index(semesters, entry.semester),
                semester1 = entry.semester1?.let { index(semesters, it) } ?: -1,
                semester2 = entry.semester2?.let { index(semesters, it) } ?: -1,
                programType = PROGRAM_TYPES.indexOf(entry.programType).takeIf { it >= 0 } ?: 0,
                suffix = suffix
            )
        }

        val bundle = SyllabusBundle(
            generatedAt = generatedAt,
            years = years.sorted(),
            codes = codes.keys.toList(),
            titles = titles.keys.toList(),
            instructors = instructors.keys.toList(),
            rooms = rooms.keys.toList(),
            semesters = semesters.keys.toList(),
            records = records
        )
        return cbor.encodeToByteArray(SyllabusBundle.serializer(), bundle)
    }

    fun decode(bytes: ByteArray): SyllabusBundle =
        cbor.decodeFromByteArray(SyllabusBundle.serializer(), bytes)

    private val PROGRAM_TYPES = listOf<String?>(null, "昼間", "夜間", "修士", "博士")
}

/** ビルド時に書き出す検証用のマニフェスト。 */
@Serializable
data class SyllabusManifest(
    @SerialName("schemaVersion") val schemaVersion: Int,
    @SerialName("generatedAt") val generatedAt: String,
    @SerialName("entryCount") val entryCount: Int,
    @SerialName("bundleBytes") val bundleBytes: Long,
    @SerialName("bundleSha256") val bundleSha256: String,
    @SerialName("years") val years: List<Int>
)
