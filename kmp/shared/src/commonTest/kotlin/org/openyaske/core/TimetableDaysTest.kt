package org.openyaske.core

import kotlin.test.Test
import kotlin.test.assertEquals
import org.openyaske.core.logic.TimetableDays
import org.openyaske.core.model.Weekday

class TimetableDaysTest {
    @Test
    fun hidesDaysRemovedInSettings() {
        val used = listOf(Weekday.MON, Weekday.TUE, Weekday.SAT)
        val visible = listOf(Weekday.MON, Weekday.TUE, Weekday.WED, Weekday.THU, Weekday.FRI)
        // 授業が無い曜日も列として残す（1 日だけに潰さない）。
        assertEquals(
            listOf(Weekday.MON, Weekday.TUE, Weekday.WED, Weekday.THU, Weekday.FRI),
            TimetableDays.resolve(used, visible)
        )
    }

    @Test
    fun fallsBackToSettingsWhenNoClassMatches() {
        val used = listOf(Weekday.SAT)
        val visible = listOf(Weekday.MON, Weekday.TUE)
        assertEquals(listOf(Weekday.MON, Weekday.TUE), TimetableDays.resolve(used, visible))
    }

    @Test
    fun keepsOrderAndDropsDuplicates() {
        val used = listOf(Weekday.FRI, Weekday.MON, Weekday.FRI)
        val visible = Weekday.entries
        assertEquals(Weekday.entries, TimetableDays.resolve(used, visible))
    }
}
