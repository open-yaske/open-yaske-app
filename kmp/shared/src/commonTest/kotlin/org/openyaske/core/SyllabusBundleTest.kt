package org.openyaske.core

import org.openyaske.core.model.SyllabusEntry
import org.openyaske.core.model.Weekday
import org.openyaske.core.validation.SyllabusBundleCodec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SyllabusBundleTest {
    private val entries = listOf(
        SyllabusEntry(
            id = "old-1",
            year = 2026,
            code = "21016136",
            title = "情報数学",
            instructor = "山田",
            room = "筑5-109",
            weekday = Weekday.MON,
            period = 1,
            semester = "前学期",
            semester1 = "前学期",
            semester2 = "前学期",
            programType = "昼間"
        ),
        SyllabusEntry(
            id = "old-2",
            year = 2026,
            code = "21016136",
            title = "情報数学",
            instructor = "山田",
            room = "筑5-109",
            weekday = Weekday.MON,
            period = 1,
            semester = "前学期",
            programType = "昼間"
        ),
        SyllabusEntry(
            id = "old-3",
            year = 2026,
            code = "21322109",
            title = "基盤鋼構造物のよこ振動",
            instructor = "貝野",
            weekday = Weekday.WED,
            period = 2,
            semester = "後学期",
            programType = "昼間"
        )
    )

    @Test
    fun compactBundleRoundTripsWithoutStoringIdsOrRepeatedNames() {
        val bytes = SyllabusBundleCodec.encode(entries, "2026-11-03T00:00:00Z")
        val decoded = SyllabusBundleCodec.decode(bytes).toEntries()

        assertEquals(3, decoded.size)
        assertEquals(setOf("情報数学", "基盤鋼構造物のよこ振動"), decoded.map { it.title }.toSet())
        assertEquals(Weekday.MON, decoded.first().weekday)
        assertEquals(1, decoded.first().period)
        assertEquals("昼間", decoded.first().programType)
        assertEquals(3, decoded.map { it.id }.toSet().size)
        assertTrue(bytes.size < 700)
    }

    @Test
    fun yearsAreSorted() {
        val bundle = SyllabusBundleCodec.decode(SyllabusBundleCodec.encode(entries))
        assertEquals(listOf(2026), bundle.years)
    }
}
