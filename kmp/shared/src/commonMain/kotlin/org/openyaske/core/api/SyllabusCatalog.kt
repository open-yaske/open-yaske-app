package org.openyaske.core.api

import org.openyaske.core.model.SyllabusEntry
import org.openyaske.core.model.Weekday

data class SyllabusSearchHit(
    val entry: SyllabusEntry,
    val score: Int
)

/**
 * 同梱シラバスの検索。
 *
 * 科目コードの完全一致・前方一致を最優先し、次に科目名と教員名を見る。
 * 日本語は空白・句読点を落として文字 bigram の重なりも加点する。
 * 索引はメモリ上に置く。数年分（1 万件前後）なら十分に速い。
 */
class SyllabusCatalog(entries: List<SyllabusEntry> = emptyList()) {
    private var indexed: List<IndexedEntry> = entries.map(::index)

    /** bigram から候補の位置へ引く転置索引。全件を採点しないためのもの。 */
    private var bigramIndex: Map<String, IntArray> = buildBigramIndex(indexed)

    val size: Int get() = indexed.size

    fun replace(entries: List<SyllabusEntry>) {
        indexed = entries.map(::index)
        bigramIndex = buildBigramIndex(indexed)
    }

    fun search(
        query: String,
        year: Int? = null,
        weekday: Weekday? = null,
        period: Int? = null,
        programType: String? = null,
        limit: Int = 30
    ): List<SyllabusSearchHit> {
        val normalizedQuery = normalize(query)
        if (normalizedQuery.isEmpty()) return emptyList()
        val queryBigrams = bigrams(normalizedQuery)

        // 2 文字以上は bigram の転置索引で候補を絞ってから採点する。
        val candidateIndexes: Iterable<Int> = if (normalizedQuery.length >= 2 && queryBigrams.isNotEmpty()) {
            val positions = LinkedHashSet<Int>()
            for (bigram in queryBigrams) {
                bigramIndex[bigram]?.forEach { positions.add(it) }
            }
            positions
        } else {
            indexed.indices
        }

        val hits = ArrayList<SyllabusSearchHit>()
        for (position in candidateIndexes) {
            if (position !in indexed.indices) continue
            val item = indexed[position]
            val entry = item.entry
            if (year != null && entry.year != year) continue
            if (weekday != null && entry.weekday != weekday) continue
            if (period != null && entry.period != period) continue
            if (programType != null && entry.programType != programType) continue
            val score = score(normalizedQuery, queryBigrams, item)
            if (score > 0) hits += SyllabusSearchHit(entry, score)
        }

        return hits.sortedWith(
            compareByDescending<SyllabusSearchHit> { it.score }
                .thenByDescending { it.entry.year }
                .thenBy { it.entry.title }
        ).take(limit.coerceIn(1, 200))
    }

    /**
     * キーワード無しで、年度・曜日・時限・課程から一覧する。
     * コマから科目を追加するときに、そのコマの候補をそのまま出すために使う。
     */
    fun list(
        year: Int? = null,
        weekday: Weekday? = null,
        period: Int? = null,
        programType: String? = null,
        limit: Int = 40
    ): List<SyllabusEntry> = indexed
        .asSequence()
        .map { it.entry }
        .filter { year == null || it.year == year }
        .filter { weekday == null || it.weekday == weekday }
        .filter { period == null || it.period == period }
        .filter { programType == null || it.programType == programType }
        .sortedWith(compareByDescending<SyllabusEntry> { it.year }.thenBy { it.title })
        .take(limit.coerceIn(1, 200))
        .toList()

    private fun score(query: String, queryBigrams: Set<String>, item: IndexedEntry): Int {
        val code = item.normalizedCode
        val title = item.normalizedTitle
        val instructor = item.normalizedInstructor

        if (code == query) return 1000
        if (code.startsWith(query)) return 900
        // 1 文字は誤爆が多いので、科目コードの一致だけを見る。
        if (query.length < 2) return 0
        if (title == query) return 800
        if (title.startsWith(query)) return 700
        if (title.contains(query)) return 600
        if (instructor.contains(query)) return 500

        if (queryBigrams.isEmpty()) return 0
        val overlap = queryBigrams.count { it in item.titleBigrams || it in item.instructorBigrams }
        if (overlap == 0) return 0
        return 100 + overlap * 10
    }

    private fun index(entry: SyllabusEntry): IndexedEntry = IndexedEntry(
        entry = entry,
        normalizedCode = normalize(entry.code),
        normalizedTitle = normalize(entry.title),
        normalizedInstructor = normalize(entry.instructor),
        titleBigrams = bigrams(normalize(entry.title)),
        instructorBigrams = bigrams(normalize(entry.instructor)),
        codeBigrams = bigrams(normalize(entry.code))
    )

    private fun buildBigramIndex(entries: List<IndexedEntry>): Map<String, IntArray> {
        val map = HashMap<String, MutableList<Int>>()
        entries.forEachIndexed { position, item ->
            val bigrams = item.titleBigrams + item.instructorBigrams + item.codeBigrams
            for (bigram in bigrams) {
                map.getOrPut(bigram) { ArrayList() }.add(position)
            }
        }
        return map.mapValues { (_, positions) -> positions.toIntArray() }
    }

    private data class IndexedEntry(
        val entry: SyllabusEntry,
        val normalizedCode: String,
        val normalizedTitle: String,
        val normalizedInstructor: String,
        val titleBigrams: Set<String>,
        val instructorBigrams: Set<String>,
        val codeBigrams: Set<String>
    )

    private companion object {
        fun normalize(input: String): String {
            val lower = input.lowercase()
            val builder = StringBuilder(lower.length)
            for (char in lower) {
                val code = char.code
                val mapped = when {
                    code in 0xFF01..0xFF5E -> (code - 0xFEE0).toChar()
                    code in 0x30A1..0x30F6 -> (code - 0x60).toChar()
                    else -> char
                }
                if (!mapped.isWhitespace() && mapped !in "、。・,.:：()（）[]【】") {
                    builder.append(mapped)
                }
            }
            return builder.toString()
        }

        fun bigrams(value: String): Set<String> {
            if (value.length < 2) return setOf(value)
            return (0..value.length - 2).mapTo(LinkedHashSet()) { value.substring(it, it + 2) }
        }
    }
}
