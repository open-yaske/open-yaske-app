package org.openyaske.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.openyaske.core.logic.GradeCatalog
import org.openyaske.core.logic.GradeScheme
import org.openyaske.core.model.GpaGrade

class GradeCatalogTest {
    @Test
    fun labelsFollowTheChosenScheme() {
        assertEquals("S", GradeCatalog.label(GpaGrade.S, GradeScheme.SABCD))
        assertEquals("D", GradeCatalog.label(GpaGrade.F, GradeScheme.SABCD))
        assertEquals("秀", GradeCatalog.label(GpaGrade.S, GradeScheme.JAPANESE))
        assertEquals("不可", GradeCatalog.label(GpaGrade.F, GradeScheme.JAPANESE))
        assertEquals("4.0", GradeCatalog.label(GpaGrade.S, GradeScheme.NUMERIC))
        assertEquals("0.0", GradeCatalog.label(GpaGrade.F, GradeScheme.NUMERIC))
    }

    @Test
    fun everySchemeHasFiveSteps() {
        GradeScheme.entries.forEach { scheme ->
            val options = GradeCatalog.options(scheme)
            assertEquals(5, options.size)
            assertEquals(listOf(4.0, 3.0, 2.0, 1.0, 0.0), options.map { it.point })
        }
    }

    @Test
    fun parseAcceptsAnyNotation() {
        assertEquals(GpaGrade.S, GradeCatalog.parse("秀"))
        assertEquals(GpaGrade.S, GradeCatalog.parse("s"))
        assertEquals(GpaGrade.A, GradeCatalog.parse("優"))
        assertEquals(GpaGrade.A, GradeCatalog.parse("3.0"))
        assertEquals(GpaGrade.B, GradeCatalog.parse("良"))
        assertEquals(GpaGrade.C, GradeCatalog.parse("可"))
        assertEquals(GpaGrade.F, GradeCatalog.parse("不可"))
        assertEquals(GpaGrade.F, GradeCatalog.parse("D"))
        assertNull(GradeCatalog.parse("優良"))
    }
}
