package org.openyaske.core

import org.openyaske.core.model.Weekday
import org.openyaske.core.validation.SyllabusTsvParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SyllabusTsvParserTest {
    @Test
    fun commentLineIsIgnored() {
        val text = """
            //昼間
            1	前学期	前学期	月1	21016136	情報数学	山田	筑5-109
        """.trimIndent()

        val entries = SyllabusTsvParser.parse(text, 2026)

        assertEquals(1, entries.size)
        assertEquals("情報数学", entries.single().title)
    }

    @Test
    fun multiSlotRowProducesOneEntryPerPeriod() {
        val text = """
            7	前学期	前学期	月1, 月2	21322109	基盤鋼構造物のよこ振動	貝野 明人	筑5-109
        """.trimIndent()

        val entries = SyllabusTsvParser.parse(text, 2026)

        assertEquals(2, entries.size)
        assertEquals(listOf(Weekday.MON to 1, Weekday.MON to 2), entries.map { it.weekday to it.period })
        assertEquals(2, entries.map { it.id }.toSet().size)
        assertTrue(entries.all { it.code == "21322109" })
    }

    @Test
    fun unscheduledRowIsKeptWithNullSlot() {
        val text = """
            531	前学期	前学期	他	21016115	Preparation for Overseas Study	未定	
        """.trimIndent()

        val entry = SyllabusTsvParser.parse(text, 2026).single()

        assertNull(entry.weekday)
        assertNull(entry.period)
        assertEquals("昼間", entry.programType)
        assertEquals("前学期", entry.semester)
    }

    @Test
    fun malformedRowWithTooFewColumnsIsDropped() {
        val text = """
            999	前学期	前学期	月3	21000000	壊れた行
        """.trimIndent()

        assertTrue(SyllabusTsvParser.parse(text, 2026).isEmpty())
    }

    @Test
    fun sameCourseOnDifferentSlotsGetsDistinctIds() {
        val text = """
            1	前学期	前学期	月1・水2	21016136	情報数学	山田	筑5-109
            2	前学期	前学期	月1	21016136	情報数学	山田	筑5-109
        """.trimIndent()

        val entries = SyllabusTsvParser.parse(text, 2026)

        assertEquals(3, entries.size)
        assertEquals(3, entries.map { it.id }.toSet().size)
        assertEquals(2, entries.count { it.weekday == Weekday.MON })
    }

    @Test
    fun semesterVariantsAreNormalized() {
        assertEquals("前学期", SyllabusTsvParser.normalizeSemester("春ターム"))
        assertEquals("後学期", SyllabusTsvParser.normalizeSemester("秋ターム"))
        assertEquals("通年", SyllabusTsvParser.normalizeSemester("通年"))
        assertEquals("前学期", SyllabusTsvParser.normalizeSemester("春ﾀｰﾑ"))
        assertEquals("後学期", SyllabusTsvParser.normalizeSemester("秋ﾀｰﾑ"))
        assertEquals("前学期", SyllabusTsvParser.normalizeSemester("前学期集中"))
        assertEquals("後学期", SyllabusTsvParser.normalizeSemester("後学期集中"))
    }

    @Test
    fun programTypeComesFromCodePrefix() {
        assertEquals("昼間", SyllabusTsvParser.programTypeOf("21016136"))
        assertEquals("夜間", SyllabusTsvParser.programTypeOf("22000000"))
        assertEquals("修士", SyllabusTsvParser.programTypeOf("25000000"))
        assertEquals("博士", SyllabusTsvParser.programTypeOf("26000000"))
        assertNull(SyllabusTsvParser.programTypeOf("99999999"))
    }
}
